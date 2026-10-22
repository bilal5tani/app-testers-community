-- 0011_functions.sql (plan Phase D tasks D1 to D12, L4)
-- Helper functions and RPC functions for all screens.

-- ---------------------------------------------------------------- D1: Helpers
create function public.fn_raise(p_code text)
returns void
language plpgsql
set search_path = ''
as $$
begin
  raise exception '%', p_code using errcode = 'P0001';
end;
$$;

create function public.fn_spots_left(p_app_id uuid)
returns int
language sql
stable
security definer
set search_path = ''
as $$
  select greatest(0, a.testers_needed - count(e.id)::int)
  from public.apps a
  left join public.app_testers e on e.app_id = a.id and e.status = 'joined'
  where a.id = p_app_id
  group by a.id, a.testers_needed;
$$;

create function public.fn_current_streak(p_app_id uuid, p_tester_id uuid)
returns int
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_today date := public.fn_today();
  v_curr date;
  v_streak int := 0;
begin
  select max(day) into v_curr
  from public.check_ins
  where app_id = p_app_id and tester_id = p_tester_id;

  if v_curr is null or v_curr < (v_today - interval '1 day')::date then
    return 0;
  end if;

  v_curr := v_today;
  if not exists (select 1 from public.check_ins where app_id = p_app_id and tester_id = p_tester_id and day = v_curr) then
    v_curr := (v_today - interval '1 day')::date;
  end if;

  while exists (select 1 from public.check_ins where app_id = p_app_id and tester_id = p_tester_id and day = v_curr) loop
    v_streak := v_streak + 1;
    v_curr := (v_curr - interval '1 day')::date;
  end loop;

  return v_streak;
end;
$$;

-- ---------------------------------------------------------------- D2: Create & Update App
create function public.rpc_create_app(
  p_title text,
  p_package_name text,
  p_play_store_url text,
  p_google_group_url text,
  p_icon_url text default null,
  p_category text default 'other',
  p_description text default null,
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
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  insert into public.apps (
    owner_id, title, package_name, play_store_url, google_group_url,
    icon_url, category, description, testers_needed, status
  ) values (
    v_uid, p_title, p_package_name, p_play_store_url, p_google_group_url,
    p_icon_url, p_category::public.app_category, p_description, coalesce(p_testers_needed, 12), 'recruiting'
  )
  returning * into v_app;

  return v_app;
end;
$$;

create function public.rpc_update_app(
  p_app_id uuid,
  p_title text default null,
  p_play_store_url text default null,
  p_google_group_url text default null,
  p_icon_url text default null,
  p_category text default null,
  p_description text default null
)
returns public.apps
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_app from public.apps where id = p_app_id;
  if v_app.id is null then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;

  update public.apps set
    title = coalesce(p_title, title),
    play_store_url = coalesce(p_play_store_url, play_store_url),
    google_group_url = coalesce(p_google_group_url, google_group_url),
    icon_url = coalesce(p_icon_url, icon_url),
    category = coalesce(p_category::public.app_category, category),
    description = coalesce(p_description, description)
  where id = p_app_id
  returning * into v_app;

  return v_app;
end;
$$;

-- ---------------------------------------------------------------- D3 & D4: Join & Leave App
create function public.rpc_join_app(p_app_id uuid)
returns public.app_testers
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_active_count int;
  v_joined_count int;
  v_enrollment public.app_testers;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_app from public.apps where id = p_app_id for update;
  if v_app.id is null then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id = v_uid then perform public.fn_raise('own_app'); end if;
  if v_app.status <> 'recruiting' and v_app.status <> 'testing' then perform public.fn_raise('app_not_open'); end if;
  if public.fn_is_blocked(v_uid, v_app.owner_id) then perform public.fn_raise('blocked'); end if;

  select count(*) into v_active_count
  from public.app_testers
  where tester_id = v_uid and status = 'joined';

  if v_active_count >= public.fn_config_int('max_active_tests') then
    perform public.fn_raise('too_many_active_tests');
  end if;

  select count(*) into v_joined_count
  from public.app_testers
  where app_id = p_app_id and status = 'joined';

  if v_joined_count >= v_app.testers_needed then
    perform public.fn_raise('app_full');
  end if;

  insert into public.app_testers (app_id, tester_id, status, joined_at, missed_streak)
  values (p_app_id, v_uid, 'joined', now(), 0)
  on conflict (app_id, tester_id) do update set
    status = 'joined',
    joined_at = now(),
    dropped_at = null,
    missed_streak = 0
  returning * into v_enrollment;

  insert into public.notifications (user_id, event, data)
  values (
    v_app.owner_id,
    'tester_joined',
    jsonb_build_object('app_id', p_app_id, 'tester_id', v_uid, 'route', 'app/' || p_app_id || '/progress')
  );

  return v_enrollment;
end;
$$;

create function public.rpc_leave_app(p_app_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_app from public.apps where id = p_app_id;
  if v_app.id is null then perform public.fn_raise('not_found'); end if;

  update public.app_testers
  set status = 'dropped', dropped_at = now()
  where app_id = p_app_id and tester_id = v_uid and status = 'joined';

  if not found then perform public.fn_raise('not_enrolled'); end if;
end;
$$;

-- ---------------------------------------------------------------- D5: Check In
create function public.rpc_check_in(p_app_id uuid, p_note text default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_today date := public.fn_today();
  v_streak int;
  v_days_done int;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  if not exists (
    select 1 from public.app_testers
    where app_id = p_app_id and tester_id = v_uid and status = 'joined'
  ) then
    perform public.fn_raise('not_enrolled');
  end if;

  insert into public.check_ins (app_id, tester_id, day, note)
  values (p_app_id, v_uid, v_today, p_note)
  on conflict (app_id, tester_id, day) do nothing;

  update public.app_testers
  set missed_streak = 0
  where app_id = p_app_id and tester_id = v_uid;

  v_streak := public.fn_current_streak(p_app_id, v_uid);
  select count(*) into v_days_done from public.check_ins where app_id = p_app_id and tester_id = v_uid;

  return jsonb_build_object(
    'day', v_today,
    'streak', v_streak,
    'days_done', v_days_done
  );
end;
$$;

-- ---------------------------------------------------------------- D6: Home
create function public.rpc_get_home()
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_my_app jsonb;
  v_today_checkins jsonb;
  v_needs_testers jsonb;
  v_unread_count int;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select jsonb_build_object(
    'id', a.id,
    'title', a.title,
    'icon_url', a.icon_url,
    'status', a.status,
    'qualified_days', a.qualified_days,
    'testers_needed', a.testers_needed,
    'joined_count', (select count(*) from public.app_testers e where e.app_id = a.id and e.status = 'joined')
  ) into v_my_app
  from public.apps a
  where a.owner_id = v_uid and a.status in ('recruiting', 'testing')
  order by a.created_at desc
  limit 1;

  select coalesce(jsonb_agg(jsonb_build_object(
    'app_id', a.id,
    'title', a.title,
    'icon_url', a.icon_url,
    'package_name', a.package_name,
    'play_store_url', a.play_store_url,
    'checked_in_today', exists (select 1 from public.check_ins c where c.app_id = a.id and c.tester_id = v_uid and c.day = public.fn_today()),
    'streak', public.fn_current_streak(a.id, v_uid)
  )), '[]'::jsonb) into v_today_checkins
  from public.app_testers e
  join public.apps a on a.id = e.app_id
  where e.tester_id = v_uid and e.status = 'joined';

  select coalesce(jsonb_agg(jsonb_build_object(
    'id', a.id,
    'title', a.title,
    'icon_url', a.icon_url,
    'category', a.category,
    'spots_left', public.fn_spots_left(a.id),
    'owner_name', p.display_name
  )), '[]'::jsonb) into v_needs_testers
  from (
    select a.*, public.fn_spots_left(a.id) as spots
    from public.apps a
    where a.owner_id <> v_uid
      and a.status = 'recruiting'
      and not public.fn_is_blocked(v_uid, a.owner_id)
      and not exists (select 1 from public.app_testers e where e.app_id = a.id and e.tester_id = v_uid and e.status in ('joined', 'completed'))
    order by spots desc, a.created_at desc
    limit 5
  ) a
  join public.profiles p on p.id = a.owner_id;

  select count(*) into v_unread_count
  from public.notifications
  where user_id = v_uid and read_at is null;

  return jsonb_build_object(
    'my_app', v_my_app,
    'today_checkins', v_today_checkins,
    'needs_testers', v_needs_testers,
    'unread_notifications', v_unread_count
  );
end;
$$;

-- ---------------------------------------------------------------- D7: Discover
create function public.rpc_discover(
  p_q text default null,
  p_category text default null,
  p_limit int default 20,
  p_offset int default 0
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_result jsonb;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select coalesce(jsonb_agg(t), '[]'::jsonb) into v_result
  from (
    select
      a.id,
      a.title,
      a.icon_url,
      a.category,
      a.description,
      a.testers_needed,
      public.fn_spots_left(a.id) as spots_left,
      p.display_name as owner_name,
      p.avatar_url as owner_avatar,
      p.karma as owner_karma,
      a.created_at
    from public.apps a
    join public.profiles p on p.id = a.owner_id
    where a.status in ('recruiting', 'testing')
      and not public.fn_is_blocked(v_uid, a.owner_id)
      and (p_category is null or p_category = '' or a.category::text = p_category)
      and (p_q is null or p_q = '' or a.title ilike '%' || p_q || '%' or a.description ilike '%' || p_q || '%')
    order by (public.fn_spots_left(a.id) > 0) desc, p.karma desc, a.created_at desc
    limit coalesce(p_limit, 20)
    offset coalesce(p_offset, 0)
  ) t;

  return v_result;
end;
$$;

-- ---------------------------------------------------------------- D8: App Detail
create function public.rpc_get_app_detail(p_app_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_owner public.profiles;
  v_enrollment public.app_testers;
  v_faces jsonb;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_app from public.apps where id = p_app_id;
  if v_app.id is null then perform public.fn_raise('not_found'); end if;
  if public.fn_is_blocked(v_uid, v_app.owner_id) then perform public.fn_raise('blocked'); end if;

  select * into v_owner from public.profiles where id = v_app.owner_id;
  select * into v_enrollment from public.app_testers where app_id = p_app_id and tester_id = v_uid;

  select coalesce(jsonb_agg(jsonb_build_object(
    'id', p.id,
    'display_name', p.display_name,
    'avatar_url', p.avatar_url
  )), '[]'::jsonb) into v_faces
  from public.app_testers e
  join public.profiles p on p.id = e.tester_id
  where e.app_id = p_app_id and e.status = 'joined' and p.is_public = true
  limit 10;

  return jsonb_build_object(
    'app', jsonb_build_object(
      'id', v_app.id,
      'owner_id', v_app.owner_id,
      'title', v_app.title,
      'package_name', v_app.package_name,
      'play_store_url', v_app.play_store_url,
      'google_group_url', v_app.google_group_url,
      'icon_url', v_app.icon_url,
      'category', v_app.category,
      'description', v_app.description,
      'status', v_app.status,
      'testers_needed', v_app.testers_needed,
      'spots_left', public.fn_spots_left(v_app.id),
      'qualified_days', v_app.qualified_days
    ),
    'owner', jsonb_build_object(
      'id', v_owner.id,
      'display_name', v_owner.display_name,
      'avatar_url', v_owner.avatar_url,
      'karma', v_owner.karma
    ),
    'my_enrollment', case when v_enrollment.id is null then null else jsonb_build_object(
      'status', v_enrollment.status,
      'joined_at', v_enrollment.joined_at,
      'streak', public.fn_current_streak(p_app_id, v_uid)
    ) end,
    'tester_faces', v_faces
  );
end;
$$;

-- ---------------------------------------------------------------- D9: Progress & Tester Emails
create function public.rpc_get_app_progress(p_app_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_testers jsonb;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_app from public.apps where id = p_app_id;
  if v_app.id is null then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;

  select coalesce(jsonb_agg(jsonb_build_object(
    'tester_id', e.tester_id,
    'display_name', p.display_name,
    'avatar_url', p.avatar_url,
    'status', e.status,
    'joined_at', e.joined_at,
    'streak', public.fn_current_streak(p_app_id, e.tester_id),
    'checked_in_today', exists (select 1 from public.check_ins c where c.app_id = p_app_id and c.tester_id = e.tester_id and c.day = public.fn_today()),
    'missed_streak', e.missed_streak
  )), '[]'::jsonb) into v_testers
  from public.app_testers e
  join public.profiles p on p.id = e.tester_id
  where e.app_id = p_app_id and e.status in ('joined', 'completed');

  return jsonb_build_object(
    'app_id', v_app.id,
    'qualified_days', v_app.qualified_days,
    'testers_needed', v_app.testers_needed,
    'joined_count', (select count(*) from public.app_testers where app_id = p_app_id and status = 'joined'),
    'testers', v_testers
  );
end;
$$;

create function public.rpc_get_tester_emails(p_app_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_emails jsonb;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_app from public.apps where id = p_app_id;
  if v_app.id is null then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;

  select coalesce(jsonb_agg(jsonb_build_object(
    'tester_id', e.tester_id,
    'display_name', p.display_name,
    'play_email', pp.play_email
  )), '[]'::jsonb) into v_emails
  from public.app_testers e
  join public.profiles p on p.id = e.tester_id
  left join public.profile_private pp on pp.user_id = e.tester_id
  where e.app_id = p_app_id and e.status in ('joined', 'completed');

  return v_emails;
end;
$$;

-- ---------------------------------------------------------------- D10: Nudge Tester
create function public.rpc_nudge_tester(p_app_id uuid, p_tester_id uuid)
returns void
language plpgsql
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
  if v_app.id is null then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;

  if exists (select 1 from public.nudges where app_id = p_app_id and tester_id = p_tester_id and day = v_today) then
    perform public.fn_raise('already_nudged');
  end if;

  insert into public.nudges (app_id, tester_id, day) values (p_app_id, p_tester_id, v_today);

  insert into public.notifications (user_id, event, data)
  values (
    p_tester_id,
    'remind',
    jsonb_build_object('app_id', p_app_id, 'app_title', v_app.title, 'route', 'app/' || p_app_id)
  );
end;
$$;

-- ---------------------------------------------------------------- D11: Profile & Preferences
create function public.rpc_get_profile(p_user_id uuid default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_target_id uuid := coalesce(p_user_id, v_uid);
  v_profile public.profiles;
  v_private public.profile_private;
  v_my_apps jsonb;
  v_stats jsonb;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_profile from public.profiles where id = v_target_id;
  if v_profile.id is null then perform public.fn_raise('not_found'); end if;

  if v_target_id <> v_uid and (not v_profile.is_public or public.fn_is_blocked(v_uid, v_target_id)) then
    perform public.fn_raise('forbidden');
  end if;

  if v_target_id = v_uid then
    select * into v_private from public.profile_private where user_id = v_uid;
  end if;

  select coalesce(jsonb_agg(jsonb_build_object(
    'id', a.id,
    'title', a.title,
    'icon_url', a.icon_url,
    'status', a.status,
    'qualified_days', a.qualified_days
  )), '[]'::jsonb) into v_my_apps
  from public.apps a
  where a.owner_id = v_target_id;

  select jsonb_build_object(
    'apps_tested', (select count(*) from public.app_testers where tester_id = v_target_id and status in ('joined', 'completed')),
    'karma', v_profile.karma,
    'testers_received', (select count(*) from public.app_testers e join public.apps a on a.id = e.app_id where a.owner_id = v_target_id and e.status in ('joined', 'completed'))
  ) into v_stats;

  return jsonb_build_object(
    'profile', jsonb_build_object(
      'id', v_profile.id,
      'username', v_profile.username,
      'display_name', v_profile.display_name,
      'avatar_url', v_profile.avatar_url,
      'bio', v_profile.bio,
      'is_public', v_profile.is_public,
      'karma', v_profile.karma,
      'theme', v_profile.theme,
      'language', v_profile.language
    ),
    'private', case when v_target_id = v_uid then jsonb_build_object(
      'play_email', v_private.play_email,
      'device_model', v_private.device_model,
      'android_version', v_private.android_version
    ) else null end,
    'my_apps', v_my_apps,
    'stats', v_stats
  );
end;
$$;

create function public.rpc_update_profile(
  p_username text default null,
  p_display_name text default null,
  p_bio text default null,
  p_avatar_url text default null,
  p_is_public boolean default null,
  p_play_email text default null,
  p_device_model text default null,
  p_android_version text default null,
  p_theme text default null,
  p_language text default null
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_clean_username text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  if p_username is not null then
    v_clean_username := lower(trim(p_username));
    if exists (select 1 from public.profiles where username = v_clean_username and id <> v_uid) then
      perform public.fn_raise('invalid_input');
    end if;
  end if;

  update public.profiles set
    username = coalesce(v_clean_username, username),
    display_name = coalesce(p_display_name, display_name),
    bio = coalesce(p_bio, bio),
    avatar_url = coalesce(p_avatar_url, avatar_url),
    is_public = coalesce(p_is_public, is_public),
    theme = coalesce(p_theme, theme),
    language = coalesce(p_language, language)
  where id = v_uid;

  if p_play_email is not null or p_device_model is not null or p_android_version is not null then
    update public.profile_private set
      play_email = coalesce(p_play_email, play_email),
      device_model = coalesce(p_device_model, device_model),
      android_version = coalesce(p_android_version, android_version)
    where user_id = v_uid;
  end if;
end;
$$;

-- ---------------------------------------------------------------- D12: Misc RPCs
create function public.rpc_list_notifications(p_limit int default 20, p_before timestamptz default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_result jsonb;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select coalesce(jsonb_agg(t), '[]'::jsonb) into v_result
  from (
    select id, event, data, read_at, created_at
    from public.notifications
    where user_id = v_uid
      and (p_before is null or created_at < p_before)
    order by created_at desc
    limit coalesce(p_limit, 20)
  ) t;

  return v_result;
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

  if p_ids is null or array_length(p_ids, 1) is null then
    update public.notifications set read_at = now() where user_id = v_uid and read_at is null;
  else
    update public.notifications set read_at = now() where user_id = v_uid and id = any(p_ids) and read_at is null;
  end if;

  get diagnostics v_count = row_count;
  return v_count;
end;
$$;

create function public.rpc_save_prefs(p_prefs jsonb)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  update public.notification_prefs set
    remind = coalesce((p_prefs->>'remind')::boolean, remind),
    streak_risk = coalesce((p_prefs->>'streak_risk')::boolean, streak_risk),
    new_apps = coalesce((p_prefs->>'new_apps')::boolean, new_apps),
    tester_joined = coalesce((p_prefs->>'tester_joined')::boolean, tester_joined),
    tester_missed = coalesce((p_prefs->>'tester_missed')::boolean, tester_missed),
    reached_target = coalesce((p_prefs->>'reached_target')::boolean, reached_target),
    period_complete = coalesce((p_prefs->>'period_complete')::boolean, period_complete),
    weekly = coalesce((p_prefs->>'weekly')::boolean, weekly),
    remind_at = coalesce((p_prefs->>'remind_at')::time, remind_at)
  where user_id = v_uid;
end;
$$;

create function public.rpc_register_device(p_fcm_token text, p_platform text default 'android')
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  insert into public.devices (user_id, fcm_token, platform, last_seen_at)
  values (v_uid, p_fcm_token, coalesce(p_platform, 'android'), now())
  on conflict (user_id, fcm_token) do update set last_seen_at = now();
end;
$$;

create function public.rpc_block_user(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if p_user_id = v_uid then perform public.fn_raise('invalid_input'); end if;

  insert into public.blocks (blocker_id, blocked_id)
  values (v_uid, p_user_id)
  on conflict do nothing;
end;
$$;

create function public.rpc_report(p_target_type text, p_target_id uuid, p_reason text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  insert into public.reports (reporter_id, target_type, target_id, reason)
  values (v_uid, p_target_type, p_target_id, p_reason);
end;
$$;

create function public.rpc_send_feedback(
  p_app_id uuid,
  p_kind text,
  p_body text,
  p_screenshot_path text default null
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  if not public.fn_is_enrolled(p_app_id) then
    perform public.fn_raise('not_enrolled');
  end if;

  insert into public.feedback (app_id, tester_id, kind, body, screenshot_path)
  values (p_app_id, v_uid, p_kind, p_body, p_screenshot_path);
end;
$$;

-- ---------------------------------------------------------------- L4: Remove Tester
create function public.rpc_remove_tester(p_app_id uuid, p_tester_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;

  select * into v_app from public.apps where id = p_app_id;
  if v_app.id is null then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;

  update public.app_testers
  set status = 'removed', dropped_at = now()
  where app_id = p_app_id and tester_id = p_tester_id and status = 'joined';

  if not found then perform public.fn_raise('not_enrolled'); end if;
end;
$$;

-- Grant EXECUTE to authenticated role for all RPC functions
grant execute on function public.fn_raise(text) to authenticated;
grant execute on function public.fn_spots_left(uuid) to authenticated;
grant execute on function public.fn_current_streak(uuid, uuid) to authenticated;
grant execute on function public.rpc_create_app(text, text, text, text, text, text, text, int) to authenticated;
grant execute on function public.rpc_update_app(uuid, text, text, text, text, text, text) to authenticated;
grant execute on function public.rpc_join_app(uuid) to authenticated;
grant execute on function public.rpc_leave_app(uuid) to authenticated;
grant execute on function public.rpc_check_in(uuid, text) to authenticated;
grant execute on function public.rpc_get_home() to authenticated;
grant execute on function public.rpc_discover(text, text, int, int) to authenticated;
grant execute on function public.rpc_get_app_detail(uuid) to authenticated;
grant execute on function public.rpc_get_app_progress(uuid) to authenticated;
grant execute on function public.rpc_get_tester_emails(uuid) to authenticated;
grant execute on function public.rpc_nudge_tester(uuid, uuid) to authenticated;
grant execute on function public.rpc_get_profile(uuid) to authenticated;
grant execute on function public.rpc_update_profile(text, text, text, text, boolean, text, text, text, text, text) to authenticated;
grant execute on function public.rpc_list_notifications(int, timestamptz) to authenticated;
grant execute on function public.rpc_mark_notifications_read(uuid[]) to authenticated;
grant execute on function public.rpc_save_prefs(jsonb) to authenticated;
grant execute on function public.rpc_register_device(text, text) to authenticated;
grant execute on function public.rpc_block_user(uuid) to authenticated;
grant execute on function public.rpc_report(text, uuid, text) to authenticated;
grant execute on function public.rpc_send_feedback(uuid, text, text, text) to authenticated;
grant execute on function public.rpc_remove_tester(uuid, uuid) to authenticated;
