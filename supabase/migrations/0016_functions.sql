-- 0016_functions.sql  (plan tasks D1 to D13, E1, E3, L4)
-- Replaces the functions and triggers of 0011/0012, which used column names that do not
-- exist in the schema (title, play_store_url, dropped_at, notifications.event ...).
-- Drops every rpc_/helper/trigger function first so it works whatever state the database is in.

do $$
declare r record;
begin
  for r in
    select p.oid::regprocedure as sig
    from pg_proc p join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = 'public'
      and (p.proname like 'rpc\_%' or p.proname like 'trg\_fn\_%'
           or p.proname in ('fn_raise', 'fn_spots_left', 'fn_current_streak', 'fn_daily_rollover',
                            'fn_notify', 'fn_feedback_object_visible', 'fn_create_reminders'))
  loop
    execute 'drop function if exists ' || r.sig || ' cascade';
  end loop;
end $$;

-- ---------------------------------------------------------------- D1: helpers
create function public.fn_raise(p_code text, p_detail text default null)
returns void
language plpgsql
set search_path = ''
as $$
begin
  raise exception using errcode = 'P0001', message = p_code, detail = coalesce(p_detail, '');
end;
$$;

-- Consecutive days with a check-in, ending today or yesterday (else 0).
create function public.fn_current_streak(p_enrollment_id uuid)
returns int
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(max(i.len) filter (where i.last_day >= public.fn_today() - 1), 0)::int
  from (
    select count(*) as len, max(d.day) as last_day
    from (
      select c.day, c.day - (row_number() over (order by c.day))::int as grp
      from public.check_ins c
      where c.enrollment_id = p_enrollment_id
    ) d
    group by d.grp
  ) i;
$$;

-- Seats still open (joined + completed testers occupy a seat).
create function public.fn_spots_left(p_app_id uuid)
returns int
language sql
stable
security definer
set search_path = ''
as $$
  select greatest(a.testers_needed - (
           select count(*) from public.app_testers e
           where e.app_id = a.id and e.status in ('joined', 'completed')), 0)::int
  from public.apps a
  where a.id = p_app_id;
$$;

-- ---------------------------------------------------------------- D2: create / update app
create function public.rpc_create_app(
  p_name           text,
  p_package_name   text,
  p_category       public.app_category,
  p_description    text,
  p_icon_url       text,
  p_opt_in_url     text,
  p_group_url      text,
  p_testers_needed int default 12
)
returns public.apps
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_constraint text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  begin
    insert into public.apps (owner_id, name, package_name, category, description, icon_url,
                             opt_in_url, group_url, testers_needed, status, listed_at)
    values (v_uid, btrim(p_name), btrim(p_package_name), coalesce(p_category, 'tools'),
            nullif(btrim(p_description), ''), nullif(btrim(p_icon_url), ''),
            btrim(p_opt_in_url), btrim(p_group_url), coalesce(p_testers_needed, 12),
            'recruiting', now())
    returning * into v_app;
  exception
    when check_violation or unique_violation or not_null_violation then
      get stacked diagnostics v_constraint = constraint_name;
      perform public.fn_raise('invalid_input', v_constraint);
  end;

  return v_app;
end;
$$;

-- p_fields keys (all optional): name, category, description, icon_url, opt_in_url,
-- group_url, testers_needed, status ('paused' | 'active'). package_name never changes.
create function public.rpc_update_app(p_app_id uuid, p_fields jsonb)
returns public.apps
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_taken int;
  v_needed int;
  v_status public.app_status;
  v_constraint text;
  v_key text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if p_fields is null or jsonb_typeof(p_fields) <> 'object' then
    perform public.fn_raise('invalid_input', 'fields');
  end if;

  for v_key in select jsonb_object_keys(p_fields) loop
    if v_key not in ('name', 'category', 'description', 'icon_url', 'opt_in_url',
                     'group_url', 'testers_needed', 'status') then
      perform public.fn_raise('invalid_input', v_key);
    end if;
  end loop;

  select * into v_app from public.apps where id = p_app_id for update;
  if not found then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;
  if v_app.status in ('draft', 'completed') then perform public.fn_raise('app_not_open'); end if;

  select count(*) into v_taken from public.app_testers e
  where e.app_id = p_app_id and e.status in ('joined', 'completed');

  v_needed := case when p_fields ? 'testers_needed' then (p_fields ->> 'testers_needed')::int
                   else v_app.testers_needed end;
  if v_needed < v_taken then perform public.fn_raise('invalid_input', 'testers_needed_below_joined'); end if;

  v_status := v_app.status;
  if p_fields ? 'status' then
    case p_fields ->> 'status'
      when 'paused' then v_status := 'paused';
      when 'active' then
        if v_app.status <> 'paused' then perform public.fn_raise('invalid_input', 'status'); end if;
        v_status := 'recruiting';
      else perform public.fn_raise('invalid_input', 'status');
    end case;
  end if;
  -- an open app is "testing" once every seat is taken, "recruiting" otherwise
  if v_status in ('recruiting', 'testing') then
    v_status := case when v_taken >= v_needed then 'testing' else 'recruiting' end;
  end if;

  begin
    update public.apps a set
      name           = case when p_fields ? 'name'        then btrim(p_fields ->> 'name') else a.name end,
      category       = case when p_fields ? 'category'    then (p_fields ->> 'category')::public.app_category else a.category end,
      description    = case when p_fields ? 'description' then nullif(btrim(p_fields ->> 'description'), '') else a.description end,
      icon_url       = case when p_fields ? 'icon_url'    then nullif(btrim(p_fields ->> 'icon_url'), '') else a.icon_url end,
      opt_in_url     = case when p_fields ? 'opt_in_url'  then btrim(p_fields ->> 'opt_in_url') else a.opt_in_url end,
      group_url      = case when p_fields ? 'group_url'   then btrim(p_fields ->> 'group_url') else a.group_url end,
      testers_needed = v_needed,
      status         = v_status
    where a.id = p_app_id
    returning * into v_app;
  exception
    when check_violation or unique_violation or not_null_violation then
      get stacked diagnostics v_constraint = constraint_name;
      perform public.fn_raise('invalid_input', v_constraint);
    when invalid_text_representation then
      perform public.fn_raise('invalid_input', 'bad_value');
  end;

  return v_app;
end;
$$;

-- ---------------------------------------------------------------- D3: join
create function public.rpc_join_app(p_app_id uuid)
returns public.app_testers
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_enr public.app_testers;
  v_taken int;
  v_active int;
  v_name text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  -- Row lock: parallel joins for the same app run one after another.
  select * into v_app from public.apps where id = p_app_id for update;
  if not found or v_app.status = 'draft' then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id = v_uid then perform public.fn_raise('own_app'); end if;
  if public.fn_is_blocked(v_uid, v_app.owner_id) then perform public.fn_raise('blocked'); end if;
  if v_app.status not in ('recruiting', 'testing') then perform public.fn_raise('app_not_open'); end if;

  select * into v_enr from public.app_testers e where e.app_id = p_app_id and e.tester_id = v_uid;
  if found then
    if v_enr.status in ('joined', 'completed') then perform public.fn_raise('already_joined'); end if;
    if v_enr.status = 'removed' then perform public.fn_raise('forbidden'); end if;
  end if;

  select count(*) into v_taken from public.app_testers e
  where e.app_id = p_app_id and e.status in ('joined', 'completed');
  if v_taken >= v_app.testers_needed then perform public.fn_raise('app_full'); end if;

  select count(*) into v_active from public.app_testers e
  where e.tester_id = v_uid and e.status = 'joined';
  if v_active >= public.fn_config_int('max_active_tests') then perform public.fn_raise('too_many_active_tests'); end if;

  if v_enr.id is null then
    insert into public.app_testers (app_id, tester_id) values (p_app_id, v_uid) returning * into v_enr;
  else  -- coming back after leaving or being auto-dropped
    update public.app_testers e
       set status = 'joined', joined_at = now(), left_at = null, missed_streak = 0
     where e.id = v_enr.id
    returning * into v_enr;
  end if;

  if v_taken + 1 >= v_app.testers_needed and v_app.status = 'recruiting' then
    update public.apps set status = 'testing' where id = p_app_id;
  end if;

  select p.display_name into v_name from public.profiles p where p.id = v_uid;
  insert into public.notifications (user_id, type, title, body, data)
  values (v_app.owner_id, 'tester_joined', 'New tester joined',
          coalesce(v_name, 'Someone') || ' joined ' || v_app.name,
          jsonb_build_object('route', 'app/' || p_app_id || '/progress', 'app_id', p_app_id, 'tester_id', v_uid));

  return v_enr;
end;
$$;

-- ---------------------------------------------------------------- D4: leave
create function public.rpc_leave_app(p_app_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_enr public.app_testers;
  v_name text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_app from public.apps where id = p_app_id for update;
  if not found then perform public.fn_raise('not_found'); end if;

  select * into v_enr from public.app_testers e
  where e.app_id = p_app_id and e.tester_id = v_uid and e.status = 'joined';
  if not found then perform public.fn_raise('not_enrolled'); end if;

  update public.app_testers set status = 'dropped', left_at = now() where id = v_enr.id;

  if v_app.status = 'testing' then
    update public.apps set status = 'recruiting' where id = p_app_id;
  end if;

  select p.display_name into v_name from public.profiles p where p.id = v_uid;
  insert into public.notifications (user_id, type, title, body, data)
  values (v_app.owner_id, 'tester_left', 'A tester left',
          coalesce(v_name, 'Someone') || ' left ' || v_app.name || '. One spot is open again.',
          jsonb_build_object('route', 'app/' || p_app_id || '/progress', 'app_id', p_app_id, 'tester_id', v_uid));

  return jsonb_build_object('ok', true);
end;
$$;

-- ---------------------------------------------------------------- D5: daily check-in
create function public.rpc_check_in(p_app_id uuid, p_note text default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_enr public.app_testers;
  v_app_status public.app_status;
  v_today date := public.fn_today();
  v_inserted int;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_enr from public.app_testers e
  where e.app_id = p_app_id and e.tester_id = v_uid and e.status = 'joined';
  if not found then perform public.fn_raise('not_enrolled'); end if;

  select a.status into v_app_status from public.apps a where a.id = p_app_id;
  if v_app_status not in ('recruiting', 'testing') then perform public.fn_raise('app_not_open'); end if;

  begin
    insert into public.check_ins (enrollment_id, day, note)
    values (v_enr.id, v_today, nullif(btrim(p_note), ''))
    on conflict (enrollment_id, day) do nothing;
    get diagnostics v_inserted = row_count;
  exception
    when check_violation then perform public.fn_raise('invalid_input', 'note');
  end;

  update public.app_testers set missed_streak = 0 where id = v_enr.id and missed_streak <> 0;

  return jsonb_build_object(
    'day',        v_today,
    'already',    v_inserted = 0,
    'streak',     public.fn_current_streak(v_enr.id),
    'days_done',  (select count(*) from public.check_ins c where c.enrollment_id = v_enr.id)
  );
end;
$$;


-- ---------------------------------------------------------------- inbox helper (internal)
-- Inserts the inbox row. The push is sent by the send-push Edge Function (database webhook),
-- which checks the user's notification preference for the type.
create function public.fn_notify(p_user uuid, p_type text, p_title text, p_body text, p_data jsonb default '{}'::jsonb)
returns void
language sql
security definer
set search_path = ''
as $$
  insert into public.notifications (user_id, type, title, body, data)
  values (p_user, p_type, p_title, p_body, coalesce(p_data, '{}'::jsonb));
$$;

-- ---------------------------------------------------------------- D6: home
create function public.rpc_get_home()
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_today date := public.fn_today();
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  return jsonb_build_object(
    'my_apps', (
      select coalesce(jsonb_agg(jsonb_build_object(
        'id', a.id, 'name', a.name, 'icon_url', a.icon_url, 'status', a.status,
        'testers_needed', a.testers_needed, 'qualified_days', a.qualified_days,
        'opted_in', o.opted_in,
        'checked_in_today', (select count(*) from public.check_ins c where c.app_id = a.id and c.day = v_today)
      ) order by a.created_at desc), '[]'::jsonb)
      from public.apps a
      cross join lateral (select count(*)::int as opted_in from public.app_testers e
                          where e.app_id = a.id and e.status in ('joined', 'completed')) o
      where a.owner_id = v_uid and a.status <> 'draft'),
    'today', (
      select coalesce(jsonb_agg(jsonb_build_object(
        'app_id', a.id, 'name', a.name, 'icon_url', a.icon_url, 'package_name', a.package_name,
        'opt_in_url', a.opt_in_url,
        'checked_today', exists (select 1 from public.check_ins c where c.enrollment_id = e.id and c.day = v_today),
        'streak', public.fn_current_streak(e.id),
        'days_done', (select count(*) from public.check_ins c where c.enrollment_id = e.id)
      ) order by a.name), '[]'::jsonb)
      from public.app_testers e join public.apps a on a.id = e.app_id
      where e.tester_id = v_uid and e.status = 'joined'),
    'needs_testers', (
      select coalesce(jsonb_agg(t), '[]'::jsonb) from (
        select a.id, a.name, a.icon_url, a.category, public.fn_spots_left(a.id) as spots_left,
               p.display_name as owner_name
        from public.apps a join public.profiles p on p.id = a.owner_id
        where a.status = 'recruiting' and a.owner_id <> v_uid
          and not public.fn_is_blocked(v_uid, a.owner_id)
          and not exists (select 1 from public.app_testers e
                          where e.app_id = a.id and e.tester_id = v_uid and e.status in ('joined', 'completed'))
        order by p.karma desc, a.listed_at desc nulls last
        limit 5) t),
    'unread_count', (select count(*) from public.notifications n where n.user_id = v_uid and n.read_at is null)
  );
end;
$$;

-- ---------------------------------------------------------------- D7: discover
create function public.rpc_discover(
  p_q text default null, p_category text default null, p_limit int default 20, p_offset int default 0)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_cat public.app_category;
  v_q text := nullif(btrim(p_q), '');
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  begin
    v_cat := nullif(btrim(p_category), '')::public.app_category;
  exception when invalid_text_representation then
    perform public.fn_raise('invalid_input', 'category');
  end;

  -- spots open first, then owners with more karma (people who test others), then newest
  return (
    select coalesce(jsonb_agg(t order by t.has_spots desc, t.owner_karma desc, t.listed_at desc), '[]'::jsonb)
    from (
      select a.id, a.name, a.icon_url, a.category, a.description, a.testers_needed, a.status, a.listed_at,
             public.fn_spots_left(a.id) as spots_left,
             (public.fn_spots_left(a.id) > 0) as has_spots,
             (select count(*) from public.app_testers e
               where e.app_id = a.id and e.status in ('joined', 'completed'))::int as opted_in,
             p.display_name as owner_name, p.avatar_url as owner_avatar, p.karma as owner_karma,
             (select e.status from public.app_testers e
               where e.app_id = a.id and e.tester_id = v_uid) as my_status
      from public.apps a join public.profiles p on p.id = a.owner_id
      where a.status in ('recruiting', 'testing') and a.owner_id <> v_uid
        and not public.fn_is_blocked(v_uid, a.owner_id)
        and (v_cat is null or a.category = v_cat)
        and (v_q is null or a.name ilike '%' || v_q || '%' or a.description ilike '%' || v_q || '%')
      order by (public.fn_spots_left(a.id) > 0) desc, p.karma desc, a.listed_at desc nulls last
      limit least(greatest(coalesce(p_limit, 20), 1), 50) offset greatest(coalesce(p_offset, 0), 0)
    ) t);
end;
$$;

-- ---------------------------------------------------------------- D8: app detail
create function public.rpc_get_app_detail(p_app_id uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_enr public.app_testers;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  select * into v_app from public.apps where id = p_app_id;
  if not found or (v_app.status = 'draft' and v_app.owner_id <> v_uid) then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid and public.fn_is_blocked(v_uid, v_app.owner_id) then perform public.fn_raise('blocked'); end if;

  select * into v_enr from public.app_testers e where e.app_id = p_app_id and e.tester_id = v_uid;

  return jsonb_build_object(
    'app', to_jsonb(v_app) || jsonb_build_object(
      'spots_left', public.fn_spots_left(p_app_id),
      'opted_in', (select count(*) from public.app_testers e where e.app_id = p_app_id and e.status in ('joined', 'completed'))),
    'is_owner', v_app.owner_id = v_uid,
    'owner', (select jsonb_build_object('id', p.id, 'display_name', p.display_name, 'username', p.username,
                                        'avatar_url', p.avatar_url, 'karma', p.karma)
              from public.profiles p where p.id = v_app.owner_id),
    'my_enrollment', case when v_enr.id is null then null else jsonb_build_object(
        'status', v_enr.status, 'joined_at', v_enr.joined_at,
        'streak', public.fn_current_streak(v_enr.id),
        'days_done', (select count(*) from public.check_ins c where c.enrollment_id = v_enr.id),
        'checked_today', exists (select 1 from public.check_ins c where c.enrollment_id = v_enr.id and c.day = public.fn_today())
      ) end,
    -- only public profiles that are not blocked either way
    'tester_faces', (
      select coalesce(jsonb_agg(jsonb_build_object('id', p.id, 'display_name', p.display_name, 'avatar_url', p.avatar_url)), '[]'::jsonb)
      from (select e.tester_id from public.app_testers e
            where e.app_id = p_app_id and e.status in ('joined', 'completed') order by e.joined_at limit 12) f
      join public.profiles p on p.id = f.tester_id
      where p.is_public and not public.fn_is_blocked(v_uid, p.id))
  );
end;
$$;

-- ---------------------------------------------------------------- D9: owner progress + emails
create function public.rpc_get_app_progress(p_app_id uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_today date := public.fn_today();
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  select * into v_app from public.apps where id = p_app_id;
  if not found then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;

  return jsonb_build_object(
    'app_id', v_app.id, 'name', v_app.name, 'status', v_app.status,
    'testers_needed', v_app.testers_needed,
    'required_days', public.fn_config_int('required_days'),
    'qualified_days', v_app.qualified_days,
    'opted_in', (select count(*) from public.app_testers e where e.app_id = p_app_id and e.status in ('joined', 'completed')),
    'checked_in_today', (select count(*) from public.check_ins c where c.app_id = p_app_id and c.day = v_today),
    'testers', (
      select coalesce(jsonb_agg(jsonb_build_object(
        'tester_id', e.tester_id, 'display_name', p.display_name, 'avatar_url', p.avatar_url,
        'status', e.status, 'joined_at', e.joined_at, 'missed_streak', e.missed_streak,
        'streak', public.fn_current_streak(e.id),
        'days_done', (select count(*) from public.check_ins c where c.enrollment_id = e.id),
        'checked_today', exists (select 1 from public.check_ins c where c.enrollment_id = e.id and c.day = v_today),
        -- last 14 days, oldest first: true = checked in
        'pattern', (select jsonb_agg(exists (select 1 from public.check_ins c where c.enrollment_id = e.id and c.day = v_today - g) order by g desc)
                    from generate_series(0, 13) g)
      ) order by e.joined_at), '[]'::jsonb)
      from public.app_testers e join public.profiles p on p.id = e.tester_id
      where e.app_id = p_app_id and e.status in ('joined', 'completed'))
  );
end;
$$;

create function public.rpc_get_tester_emails(p_app_id uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if not exists (select 1 from public.apps a where a.id = p_app_id) then perform public.fn_raise('not_found'); end if;
  if not public.fn_owns_app(p_app_id) then perform public.fn_raise('forbidden'); end if;

  return (select coalesce(jsonb_agg(jsonb_build_object('tester_id', e.tester_id, 'display_name', p.display_name,
                                                       'play_email', pp.play_email) order by e.joined_at), '[]'::jsonb)
          from public.app_testers e
          join public.profiles p on p.id = e.tester_id
          left join public.profile_private pp on pp.user_id = e.tester_id
          where e.app_id = p_app_id and e.status in ('joined', 'completed') and pp.play_email is not null);
end;
$$;

-- ---------------------------------------------------------------- D10: nudge
create function public.rpc_nudge_tester(p_app_id uuid, p_tester_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_enr public.app_testers;
  v_rows int;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  select * into v_app from public.apps where id = p_app_id;
  if not found then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;

  select * into v_enr from public.app_testers e where e.app_id = p_app_id and e.tester_id = p_tester_id and e.status = 'joined';
  if not found then perform public.fn_raise('not_enrolled'); end if;
  if exists (select 1 from public.check_ins c where c.enrollment_id = v_enr.id and c.day = public.fn_today()) then
    perform public.fn_raise('invalid_input', 'already_checked_in');
  end if;

  insert into public.nudges (app_id, tester_id, sender_id) values (p_app_id, p_tester_id, v_uid)
  on conflict (app_id, tester_id, day) do nothing;
  get diagnostics v_rows = row_count;
  if v_rows = 0 then perform public.fn_raise('already_nudged'); end if;

  perform public.fn_notify(p_tester_id, 'nudge', 'A nudge from the developer',
    'The developer of ' || v_app.name || ' is waiting for your check-in today.',
    jsonb_build_object('route', 'app/' || p_app_id, 'app_id', p_app_id));
  return jsonb_build_object('ok', true);
end;
$$;

-- ---------------------------------------------------------------- D11: profile
create function public.rpc_get_profile(p_user_id uuid default null)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_id uuid := coalesce(p_user_id, (select auth.uid()));
  v_self boolean;
  v_p public.profiles;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  v_self := v_id = v_uid;
  select * into v_p from public.profiles where id = v_id;
  if not found then perform public.fn_raise('not_found'); end if;
  if not v_self and (not v_p.is_public or public.fn_is_blocked(v_uid, v_id)) then perform public.fn_raise('forbidden'); end if;

  return jsonb_build_object(
    'profile', jsonb_build_object('id', v_p.id, 'username', v_p.username, 'display_name', v_p.display_name,
        'bio', v_p.bio, 'avatar_url', v_p.avatar_url, 'karma', v_p.karma, 'created_at', v_p.created_at)
      || case when v_self then jsonb_build_object('is_public', v_p.is_public, 'show_history', v_p.show_history,
                'find_by_email', v_p.find_by_email, 'theme', v_p.theme, 'language', v_p.language) else '{}'::jsonb end,
    'private', case when v_self then
        (select jsonb_build_object('play_email', pp.play_email, 'device_model', pp.device_model,
                                   'android_version', pp.android_version)
         from public.profile_private pp where pp.user_id = v_id) end,
    'stats', jsonb_build_object(
        'apps_tested', (select count(*) from public.app_testers e where e.tester_id = v_id and e.status = 'completed'),
        'best_streak', (select coalesce(max(public.fn_current_streak(e.id)), 0) from public.app_testers e
                        where e.tester_id = v_id and e.status = 'joined'),
        'testers_received', (select count(*) from public.app_testers e join public.apps a on a.id = e.app_id
                             where a.owner_id = v_id and e.status in ('joined', 'completed')),
        'karma', v_p.karma),
    'my_apps', (select coalesce(jsonb_agg(jsonb_build_object('id', a.id, 'name', a.name, 'icon_url', a.icon_url,
                    'status', a.status, 'qualified_days', a.qualified_days, 'testers_needed', a.testers_needed)
                    order by a.created_at desc), '[]'::jsonb)
                from public.apps a where a.owner_id = v_id and (v_self or a.status <> 'draft')),
    'history', case when v_self or v_p.show_history then
        (select coalesce(jsonb_agg(jsonb_build_object('app_id', a.id, 'name', a.name, 'icon_url', a.icon_url,
                    'status', e.status, 'joined_at', e.joined_at,
                    'days_done', (select count(*) from public.check_ins c where c.enrollment_id = e.id))
                    order by e.joined_at desc), '[]'::jsonb)
         from public.app_testers e join public.apps a on a.id = e.app_id where e.tester_id = v_id) else '[]'::jsonb end
  );
end;
$$;

-- p_fields keys: username, display_name, bio, avatar_url, is_public, show_history, find_by_email,
-- theme, language, play_email, device_model, android_version
create function public.rpc_update_profile(p_fields jsonb)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_key text;
  v_constraint text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if p_fields is null or jsonb_typeof(p_fields) <> 'object' then perform public.fn_raise('invalid_input', 'fields'); end if;
  for v_key in select jsonb_object_keys(p_fields) loop
    if v_key not in ('username', 'display_name', 'bio', 'avatar_url', 'is_public', 'show_history', 'find_by_email',
                     'theme', 'language', 'play_email', 'device_model', 'android_version') then
      perform public.fn_raise('invalid_input', v_key);
    end if;
  end loop;

  begin
    update public.profiles p set
      username      = case when p_fields ? 'username'      then nullif(lower(btrim(p_fields ->> 'username')), '') else p.username end,
      display_name  = case when p_fields ? 'display_name'  then btrim(p_fields ->> 'display_name') else p.display_name end,
      bio           = case when p_fields ? 'bio'           then nullif(btrim(p_fields ->> 'bio'), '') else p.bio end,
      avatar_url    = case when p_fields ? 'avatar_url'    then nullif(btrim(p_fields ->> 'avatar_url'), '') else p.avatar_url end,
      is_public     = case when p_fields ? 'is_public'     then (p_fields ->> 'is_public')::boolean else p.is_public end,
      show_history  = case when p_fields ? 'show_history'  then (p_fields ->> 'show_history')::boolean else p.show_history end,
      find_by_email = case when p_fields ? 'find_by_email' then (p_fields ->> 'find_by_email')::boolean else p.find_by_email end,
      theme         = case when p_fields ? 'theme'         then p_fields ->> 'theme' else p.theme end,
      language      = case when p_fields ? 'language'      then p_fields ->> 'language' else p.language end
    where p.id = v_uid;

    update public.profile_private pp set
      play_email      = case when p_fields ? 'play_email'      then nullif(btrim(p_fields ->> 'play_email'), '') else pp.play_email end,
      device_model    = case when p_fields ? 'device_model'    then nullif(btrim(p_fields ->> 'device_model'), '') else pp.device_model end,
      android_version = case when p_fields ? 'android_version' then nullif(btrim(p_fields ->> 'android_version'), '') else pp.android_version end
    where pp.user_id = v_uid;
  exception
    when unique_violation then
      perform public.fn_raise('invalid_input', 'username_taken');
    when check_violation or not_null_violation then
      get stacked diagnostics v_constraint = constraint_name;
      perform public.fn_raise('invalid_input', v_constraint);
    when invalid_text_representation then
      perform public.fn_raise('invalid_input', 'bad_value');
  end;

  return public.rpc_get_profile(v_uid);
end;
$$;

-- ---------------------------------------------------------------- D12: inbox, prefs, devices, safety, feedback
create function public.rpc_list_notifications(p_limit int default 30, p_before timestamptz default null)
returns jsonb
language plpgsql
stable
security definer
set search_path = ''
as $$
declare v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  return (select coalesce(jsonb_agg(to_jsonb(t) order by t.created_at desc), '[]'::jsonb) from (
    select n.id, n.type, n.title, n.body, n.data, n.read_at, n.created_at
    from public.notifications n
    where n.user_id = v_uid and (p_before is null or n.created_at < p_before)
    order by n.created_at desc
    limit least(greatest(coalesce(p_limit, 30), 1), 100)) t);
end;
$$;

create function public.rpc_mark_notifications_read(p_ids uuid[] default null)
returns int
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_count int;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  update public.notifications n set read_at = now()
   where n.user_id = v_uid and n.read_at is null
     and (p_ids is null or cardinality(p_ids) = 0 or n.id = any (p_ids));
  get diagnostics v_count = row_count;
  return v_count;
end;
$$;

create function public.rpc_save_prefs(p_prefs jsonb)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_key text;
  v_row public.notification_prefs;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if p_prefs is null or jsonb_typeof(p_prefs) <> 'object' then perform public.fn_raise('invalid_input', 'prefs'); end if;
  for v_key in select jsonb_object_keys(p_prefs) loop
    if v_key not in ('remind', 'streak_risk', 'new_apps', 'tester_joined', 'tester_missed',
                     'reached_target', 'period_complete', 'weekly', 'remind_at') then
      perform public.fn_raise('invalid_input', v_key);
    end if;
  end loop;
  begin
    update public.notification_prefs p set
      remind          = case when p_prefs ? 'remind'          then (p_prefs ->> 'remind')::boolean else p.remind end,
      streak_risk     = case when p_prefs ? 'streak_risk'     then (p_prefs ->> 'streak_risk')::boolean else p.streak_risk end,
      new_apps        = case when p_prefs ? 'new_apps'        then (p_prefs ->> 'new_apps')::boolean else p.new_apps end,
      tester_joined   = case when p_prefs ? 'tester_joined'   then (p_prefs ->> 'tester_joined')::boolean else p.tester_joined end,
      tester_missed   = case when p_prefs ? 'tester_missed'   then (p_prefs ->> 'tester_missed')::boolean else p.tester_missed end,
      reached_target  = case when p_prefs ? 'reached_target'  then (p_prefs ->> 'reached_target')::boolean else p.reached_target end,
      period_complete = case when p_prefs ? 'period_complete' then (p_prefs ->> 'period_complete')::boolean else p.period_complete end,
      weekly          = case when p_prefs ? 'weekly'          then (p_prefs ->> 'weekly')::boolean else p.weekly end,
      remind_at       = case when p_prefs ? 'remind_at'       then (p_prefs ->> 'remind_at')::time else p.remind_at end
    where p.user_id = v_uid
    returning * into v_row;
  exception when invalid_text_representation or datetime_field_overflow or invalid_datetime_format then
    perform public.fn_raise('invalid_input', 'bad_value');
  end;
  return to_jsonb(v_row) - 'created_at' - 'updated_at';
end;
$$;

create function public.rpc_register_device(p_fcm_token text, p_platform text default 'android')
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if p_fcm_token is null or char_length(btrim(p_fcm_token)) < 20 then perform public.fn_raise('invalid_input', 'fcm_token'); end if;
  -- a token belongs to one phone: if another account signed in on it before, it moves to this user
  insert into public.devices (user_id, fcm_token, platform)
  values (v_uid, btrim(p_fcm_token), coalesce(nullif(btrim(p_platform), ''), 'android'))
  on conflict (fcm_token) do update set user_id = excluded.user_id, last_seen_at = now();
  return jsonb_build_object('ok', true);
end;
$$;

create function public.rpc_block_user(p_user_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if p_user_id is null or p_user_id = v_uid then perform public.fn_raise('invalid_input', 'user'); end if;
  if not exists (select 1 from public.profiles p where p.id = p_user_id) then perform public.fn_raise('not_found'); end if;
  insert into public.blocks (blocker_id, blocked_id) values (v_uid, p_user_id) on conflict do nothing;
  return jsonb_build_object('ok', true);
end;
$$;

create function public.rpc_report(p_target_type text, p_target_id uuid, p_reason text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_constraint text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  begin
    insert into public.reports (reporter_id, target_type, target_id, reason)
    values (v_uid, p_target_type, p_target_id, btrim(p_reason));
  exception when check_violation or not_null_violation then
    get stacked diagnostics v_constraint = constraint_name;
    perform public.fn_raise('invalid_input', v_constraint);
  end;
  return jsonb_build_object('ok', true);
end;
$$;

create function public.rpc_send_feedback(p_app_id uuid, p_kind text, p_body text, p_screenshot_path text default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_constraint text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if not exists (select 1 from public.apps a where a.id = p_app_id) then perform public.fn_raise('not_found'); end if;
  if not exists (select 1 from public.app_testers e where e.app_id = p_app_id and e.tester_id = v_uid
                 and e.status in ('joined', 'completed')) then
    perform public.fn_raise('not_enrolled');
  end if;
  -- screenshots must sit in the sender's own folder: feedback/<uid>/<app_id>/<file>
  if p_screenshot_path is not null and p_screenshot_path not like v_uid::text || '/' || p_app_id::text || '/%' then
    perform public.fn_raise('invalid_input', 'screenshot_path');
  end if;
  begin
    insert into public.feedback (app_id, tester_id, kind, body, screenshot_path)
    values (p_app_id, v_uid, p_kind, btrim(p_body), nullif(btrim(p_screenshot_path), ''));
  exception when check_violation or not_null_violation then
    get stacked diagnostics v_constraint = constraint_name;
    perform public.fn_raise('invalid_input', v_constraint);
  end;
  return jsonb_build_object('ok', true);
end;
$$;

-- ---------------------------------------------------------------- L4: developer removes a tester
create function public.rpc_remove_tester(p_app_id uuid, p_tester_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_rows int;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  select * into v_app from public.apps where id = p_app_id for update;
  if not found then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;

  update public.app_testers set status = 'removed', left_at = now()
   where app_id = p_app_id and tester_id = p_tester_id and status = 'joined';
  get diagnostics v_rows = row_count;
  if v_rows = 0 then perform public.fn_raise('not_enrolled'); end if;

  if v_app.status = 'testing' then update public.apps set status = 'recruiting' where id = p_app_id; end if;
  perform public.fn_notify(p_tester_id, 'removed', 'Removed from a test',
    'The developer removed you from ' || v_app.name || '.', jsonb_build_object('route', 'app/' || p_app_id));
  return jsonb_build_object('ok', true);
end;
$$;

-- ---------------------------------------------------------------- D13: notification triggers
-- Every seat taken: app goes recruiting -> testing, tell the developer.
create function public.trg_fn_apps_reached_target()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform public.fn_notify(new.owner_id, 'reached_target', 'All testers found',
    new.name || ' has all ' || new.testers_needed || ' testers. The 14-day count starts now.',
    jsonb_build_object('route', 'app/' || new.id || '/progress', 'app_id', new.id));
  return new;
end;
$$;
create trigger trg_apps_reached_target
  after update of status on public.apps
  for each row
  when (old.status = 'recruiting' and new.status = 'testing')
  execute function public.trg_fn_apps_reached_target();

-- New app listed: tell users who turned on "new apps" (opt-in, so no inbox spam for everyone else).
create function public.trg_fn_apps_listed()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.notifications (user_id, type, title, body, data)
  select np.user_id, 'new_apps', 'New app needs testers', new.name || ' is looking for testers.',
         jsonb_build_object('route', 'app/' || new.id, 'app_id', new.id)
  from public.notification_prefs np
  where np.new_apps and np.user_id <> new.owner_id
    and not public.fn_is_blocked(np.user_id, new.owner_id);
  return new;
end;
$$;
create trigger trg_apps_listed
  after insert on public.apps
  for each row
  when (new.status = 'recruiting')
  execute function public.trg_fn_apps_listed();

-- ---------------------------------------------------------------- E1: nightly rollover
-- Runs just after midnight and closes the day that just ended (fn_today() - 1).
-- Safe to run twice: app_config.last_rollover_day remembers the last closed day.
create function public.fn_daily_rollover(p_day date default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_day date := coalesce(p_day, public.fn_today() - 1);
  v_tz text := (select c.value from public.app_config c where c.key = 'timezone');
  v_required int := public.fn_config_int('required_days');
  v_max_missed int := public.fn_config_int('auto_drop_after_missed');
  v_last date;
  r record;
  v_opted int;
  v_checked int;
  v_ok boolean;
  v_completed int := 0;
  v_dropped int := 0;
begin
  select nullif(c.value, '')::date into v_last from public.app_config c where c.key = 'last_rollover_day';
  if v_last is not null and v_last >= v_day then
    return jsonb_build_object('skipped', true, 'last_rollover_day', v_last);
  end if;

  -- 1. snapshot, qualified days, completion
  for r in select a.id, a.name, a.owner_id, a.testers_needed, a.qualified_days
           from public.apps a where a.status in ('recruiting', 'testing') for update loop
    select count(*) into v_opted from public.app_testers e
     where e.app_id = r.id and e.status in ('joined', 'completed');
    select count(*) into v_checked from public.check_ins c where c.app_id = r.id and c.day = v_day;
    v_ok := v_opted >= r.testers_needed;

    insert into public.app_daily_snapshots (app_id, day, opted_in, checked_in, qualified)
    values (r.id, v_day, v_opted, v_checked, v_ok)
    on conflict (app_id, day) do update
      set opted_in = excluded.opted_in, checked_in = excluded.checked_in, qualified = excluded.qualified;

    if v_ok then
      update public.apps set qualified_days = least(r.qualified_days + 1, v_required) where id = r.id;
      if r.qualified_days + 1 >= v_required then
        update public.apps set status = 'completed', completed_at = now(), qualified_days = v_required where id = r.id;
        update public.app_testers set status = 'completed' where app_id = r.id and status = 'joined';
        update public.profiles p set karma = p.karma + 1
         where p.id in (select e.tester_id from public.app_testers e where e.app_id = r.id and e.status = 'completed');
        perform public.fn_notify(r.owner_id, 'period_complete', 'Test period complete',
          r.name || ' finished the ' || v_required || '-day closed test. You can apply for production.',
          jsonb_build_object('route', 'app/' || r.id || '/progress', 'app_id', r.id));
        insert into public.notifications (user_id, type, title, body, data)
        select e.tester_id, 'period_complete', 'You finished a test',
               'Thanks for testing ' || r.name || '. +1 karma.',
               jsonb_build_object('route', 'app/' || r.id, 'app_id', r.id)
        from public.app_testers e where e.app_id = r.id and e.status = 'completed';
        v_completed := v_completed + 1;
      end if;
    else
      update public.apps set qualified_days = 0 where id = r.id and qualified_days <> 0;
    end if;
  end loop;

  -- 2. misses and auto-drop (only testers who joined before that day, only apps still open)
  for r in select e.id as enr_id, e.app_id, e.tester_id, e.missed_streak, a.owner_id, a.name
           from public.app_testers e join public.apps a on a.id = e.app_id
           where e.status = 'joined' and a.status in ('recruiting', 'testing')
             and (e.joined_at at time zone v_tz)::date < v_day
             and not exists (select 1 from public.check_ins c where c.enrollment_id = e.id and c.day = v_day) loop
    if r.missed_streak + 1 >= v_max_missed then
      update public.app_testers set status = 'dropped', left_at = now(), missed_streak = r.missed_streak + 1 where id = r.enr_id;
      update public.profiles set karma = greatest(karma - 1, 0) where id = r.tester_id;
      perform public.fn_notify(r.owner_id, 'tester_missed', 'A tester was dropped',
        'A tester missed ' || v_max_missed || ' days in a row on ' || r.name || ' and was dropped. One spot is open.',
        jsonb_build_object('route', 'app/' || r.app_id || '/progress', 'app_id', r.app_id, 'tester_id', r.tester_id));
      perform public.fn_notify(r.tester_id, 'dropped', 'You were dropped from a test',
        'You missed ' || v_max_missed || ' check-ins in a row on ' || r.name || '.',
        jsonb_build_object('route', 'app/' || r.app_id, 'app_id', r.app_id));
      v_dropped := v_dropped + 1;
    else
      update public.app_testers set missed_streak = r.missed_streak + 1 where id = r.enr_id;
    end if;
  end loop;

  -- 3. apps that lost testers reopen for recruiting
  update public.apps a set status = 'recruiting'
   where a.status = 'testing'
     and (select count(*) from public.app_testers e where e.app_id = a.id and e.status in ('joined', 'completed')) < a.testers_needed;

  insert into public.app_config (key, value) values ('last_rollover_day', v_day::text)
  on conflict (key) do update set value = excluded.value;

  return jsonb_build_object('day', v_day, 'completed', v_completed, 'dropped', v_dropped);
end;
$$;

-- ---------------------------------------------------------------- E3: reminders (every 15 minutes)
-- Creates inbox rows for users whose reminder time falls in this 15-minute window, at most
-- once per user per day and type. The send-push function turns each row into a push.
create function public.fn_create_reminders(p_now timestamptz default now())
returns int
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_tz text := (select c.value from public.app_config c where c.key = 'timezone');
  v_today date := public.fn_today();
  v_local time := (p_now at time zone v_tz)::time;
  v_count int := 0;
  v_n int;
begin
  -- testers who have not checked in today
  insert into public.notifications (user_id, type, title, body, data)
  select np.user_id, 'remind', 'Time to check in',
         'You have ' || x.pending || ' test' || case when x.pending > 1 then 's' else '' end || ' to check in today.',
         jsonb_build_object('route', 'home')
  from public.notification_prefs np
  join lateral (
    select count(*) as pending from public.app_testers e
    where e.tester_id = np.user_id and e.status = 'joined'
      and (e.joined_at at time zone v_tz)::date <= v_today
      and not exists (select 1 from public.check_ins c where c.enrollment_id = e.id and c.day = v_today)) x on x.pending > 0
  where np.remind
    and ((extract(epoch from (v_local - np.remind_at)) / 60)::int + 1440) % 1440 < 15
    and not exists (select 1 from public.notifications n where n.user_id = np.user_id and n.type = 'remind'
                    and (n.created_at at time zone v_tz)::date = v_today);
  get diagnostics v_n = row_count; v_count := v_count + v_n;

  -- streak at risk: checked in yesterday, not yet today
  insert into public.notifications (user_id, type, title, body, data)
  select np.user_id, 'streak_risk', 'Your streak is at risk',
         'Check in today to keep your streak on ' || x.name || '.',
         jsonb_build_object('route', 'app/' || x.app_id, 'app_id', x.app_id)
  from public.notification_prefs np
  join lateral (
    select a.id as app_id, a.name from public.app_testers e join public.apps a on a.id = e.app_id
    where e.tester_id = np.user_id and e.status = 'joined'
      and exists (select 1 from public.check_ins c where c.enrollment_id = e.id and c.day = v_today - 1)
      and not exists (select 1 from public.check_ins c where c.enrollment_id = e.id and c.day = v_today)
    order by public.fn_current_streak(e.id) desc limit 1) x on true
  where np.streak_risk
    and ((extract(epoch from (v_local - np.remind_at)) / 60)::int + 1440) % 1440 < 15
    and not exists (select 1 from public.notifications n where n.user_id = np.user_id and n.type = 'streak_risk'
                    and (n.created_at at time zone v_tz)::date = v_today);
  get diagnostics v_n = row_count; v_count := v_count + v_n;

  -- developers: testers still missing today
  insert into public.notifications (user_id, type, title, body, data)
  select np.user_id, 'tester_missed', 'Testers still to check in',
         x.missing || ' of your testers have not checked in today.',
         jsonb_build_object('route', 'progress')
  from public.notification_prefs np
  join lateral (
    select count(*) as missing from public.app_testers e join public.apps a on a.id = e.app_id
    where a.owner_id = np.user_id and e.status = 'joined' and a.status in ('recruiting', 'testing')
      and (e.joined_at at time zone v_tz)::date < v_today
      and not exists (select 1 from public.check_ins c where c.enrollment_id = e.id and c.day = v_today)) x on x.missing > 0
  where np.tester_missed
    and ((extract(epoch from (v_local - np.remind_at)) / 60)::int + 1440) % 1440 < 15
    and not exists (select 1 from public.notifications n where n.user_id = np.user_id and n.type = 'tester_missed'
                    and n.data ->> 'route' = 'progress' and (n.created_at at time zone v_tz)::date = v_today);
  get diagnostics v_n = row_count; v_count := v_count + v_n;

  return v_count;
end;
$$;

-- ---------------------------------------------------------------- schedules (pg_cron)
create extension if not exists pg_cron with schema extensions;

do $$
begin
  perform cron.unschedule(jobname) from cron.job where jobname in ('daily-rollover-job', 'reminders-job');
end $$;
select cron.schedule('daily-rollover-job', '5 19 * * *',    $$select public.fn_daily_rollover();$$);   -- 00:05 Karachi
select cron.schedule('reminders-job',      '*/15 * * * *', $$select public.fn_create_reminders();$$);

-- ---------------------------------------------------------------- grants
-- Nothing is callable by anon or PUBLIC. Only the app-facing rpc_* go to signed-in users;
-- fn_* helpers, trigger functions and cron jobs run as the owner.
revoke execute on all functions in schema public from public, anon;

grant execute on function public.rpc_create_app(text, text, public.app_category, text, text, text, text, int) to authenticated;
grant execute on function public.rpc_update_app(uuid, jsonb)                to authenticated;
grant execute on function public.rpc_join_app(uuid)                         to authenticated;
grant execute on function public.rpc_leave_app(uuid)                        to authenticated;
grant execute on function public.rpc_check_in(uuid, text)                   to authenticated;
grant execute on function public.rpc_get_home()                             to authenticated;
grant execute on function public.rpc_discover(text, text, int, int)         to authenticated;
grant execute on function public.rpc_get_app_detail(uuid)                   to authenticated;
grant execute on function public.rpc_get_app_progress(uuid)                 to authenticated;
grant execute on function public.rpc_get_tester_emails(uuid)                to authenticated;
grant execute on function public.rpc_nudge_tester(uuid, uuid)               to authenticated;
grant execute on function public.rpc_get_profile(uuid)                      to authenticated;
grant execute on function public.rpc_update_profile(jsonb)                  to authenticated;
grant execute on function public.rpc_list_notifications(int, timestamptz)   to authenticated;
grant execute on function public.rpc_mark_notifications_read(uuid[])        to authenticated;
grant execute on function public.rpc_save_prefs(jsonb)                      to authenticated;
grant execute on function public.rpc_register_device(text, text)            to authenticated;
grant execute on function public.rpc_block_user(uuid)                       to authenticated;
grant execute on function public.rpc_report(text, uuid, text)               to authenticated;
grant execute on function public.rpc_send_feedback(uuid, text, text, text)  to authenticated;
grant execute on function public.rpc_remove_tester(uuid, uuid)              to authenticated;
