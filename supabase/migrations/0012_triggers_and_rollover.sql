-- 0012_triggers_and_rollover.sql (plan Phase D task D13 and Phase E task E1)
-- Notification triggers and daily rollover function for cron jobs.

-- ---------------------------------------------------------------- D13: Notification Triggers
create function public.trg_fn_app_tester_joined()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_app public.apps;
  v_joined_count int;
begin
  if new.status = 'joined' and (old is null or old.status <> 'joined') then
    select * into v_app from public.apps where id = new.app_id;

    select count(*) into v_joined_count
    from public.app_testers
    where app_id = new.app_id and status = 'joined';

    if v_joined_count >= v_app.testers_needed then
      -- Update status to testing if recruiting
      if v_app.status = 'recruiting' then
        update public.apps set status = 'testing' where id = new.app_id;
      end if;

      -- Notify owner if target reached
      insert into public.notifications (user_id, event, data)
      values (
        v_app.owner_id,
        'reached_target',
        jsonb_build_object('app_id', v_app.id, 'app_title', v_app.title, 'route', 'app/' || v_app.id || '/progress')
      );
    end if;
  end if;

  return new;
end;
$$;

create trigger trg_app_tester_joined
  after insert or update on public.app_testers
  for each row
  execute function public.trg_fn_app_tester_joined();


create function public.trg_fn_app_created()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.status = 'recruiting' and (old is null or old.status <> 'recruiting') then
    insert into public.notifications (user_id, event, data)
    select
      np.user_id,
      'new_apps',
      jsonb_build_object('app_id', new.id, 'app_title', new.title, 'category', new.category, 'route', 'app/' || new.id)
    from public.notification_prefs np
    where np.new_apps = true and np.user_id <> new.owner_id;
  end if;

  return new;
end;
$$;

create trigger trg_app_created
  after insert or update on public.apps
  for each row
  execute function public.trg_fn_app_created();


-- ---------------------------------------------------------------- E1: Daily Rollover
create function public.fn_daily_rollover()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_today date := public.fn_today();
  v_auto_drop_missed int := public.fn_config_int('auto_drop_after_missed');
  v_required_days int := public.fn_config_int('required_days');
  r_app record;
  r_tester record;
  v_joined_count int;
  v_checkin_count int;
begin
  -- 1. Snapshots
  for r_app in select id, testers_needed, status from public.apps where status in ('recruiting', 'testing') loop
    select count(*) into v_joined_count from public.app_testers where app_id = r_app.id and status = 'joined';
    select count(*) into v_checkin_count from public.check_ins where app_id = r_app.id and day = v_today;

    insert into public.app_daily_snapshots (app_id, day, joined_count, checkin_count)
    values (r_app.id, v_today, v_joined_count, v_checkin_count)
    on conflict (app_id, day) do update set
      joined_count = excluded.joined_count,
      checkin_count = excluded.checkin_count;
  end loop;

  -- 2. Process missed check-ins & auto-drop
  for r_tester in
    select e.id, e.app_id, e.tester_id, e.missed_streak, a.owner_id, a.title
    from public.app_testers e
    join public.apps a on a.id = e.app_id
    where e.status = 'joined'
      and not exists (
        select 1 from public.check_ins c
        where c.app_id = e.app_id and c.tester_id = e.tester_id and c.day = v_today
      )
  loop
    if r_tester.missed_streak + 1 >= v_auto_drop_missed then
      -- Auto-drop
      update public.app_testers
      set status = 'dropped', dropped_at = now(), missed_streak = r_tester.missed_streak + 1
      where id = r_tester.id;

      -- Deduct karma
      update public.profiles
      set karma = greatest(0, karma - 1)
      where id = r_tester.tester_id;

      -- Notify owner
      insert into public.notifications (user_id, event, data)
      values (
        r_tester.owner_id,
        'tester_missed',
        jsonb_build_object('app_id', r_tester.app_id, 'app_title', r_tester.title, 'tester_id', r_tester.tester_id, 'route', 'app/' || r_tester.app_id || '/progress')
      );
    else
      -- Increment missed streak
      update public.app_testers
      set missed_streak = missed_streak + 1
      where id = r_tester.id;
    end if;
  end loop;

  -- 3. Qualified days & completion check for testing apps
  for r_app in select id, owner_id, title, testers_needed, qualified_days from public.apps where status = 'testing' loop
    select count(*) into v_joined_count from public.app_testers where app_id = r_app.id and status = 'joined';

    if v_joined_count >= r_app.testers_needed then
      if r_app.qualified_days + 1 >= v_required_days then
        -- App completed!
        update public.apps set status = 'completed', qualified_days = v_required_days where id = r_app.id;

        -- Update enrollments to completed & award +1 karma to testers
        update public.app_testers set status = 'completed' where app_id = r_app.id and status = 'joined';

        update public.profiles p
        set karma = karma + 1
        from public.app_testers e
        where e.app_id = r_app.id and e.tester_id = p.id and e.status = 'completed';

        -- Notify developer
        insert into public.notifications (user_id, event, data)
        values (
          r_app.owner_id,
          'period_complete',
          jsonb_build_object('app_id', r_app.id, 'app_title', r_app.title, 'route', 'app/' || r_app.id || '/progress')
        );

        -- Notify testers
        insert into public.notifications (user_id, event, data)
        select
          e.tester_id,
          'period_complete',
          jsonb_build_object('app_id', r_app.id, 'app_title', r_app.title, 'route', 'app/' || r_app.id)
        from public.app_testers e
        where e.app_id = r_app.id and e.status = 'completed';
      else
        -- Increment qualified_days
        update public.apps set qualified_days = qualified_days + 1 where id = r_app.id;
      end if;
    else
      -- Reset qualified_days if dropped below required testers
      update public.apps set qualified_days = 0 where id = r_app.id;
    end if;
  end loop;
end;
$$;
