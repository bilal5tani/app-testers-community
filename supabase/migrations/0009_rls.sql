-- 0009_rls.sql  (plan tasks C1 to C4)
-- Default deny: RLS on every table, table grants only where the app needs them,
-- and no function is callable by the app unless it is granted explicitly.

-- ---------------------------------------------------------------- helpers
-- These run as the owner (security definer) so policies can look at other tables without recursion.
create function public.fn_owns_app(p_app_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (select 1 from public.apps a where a.id = p_app_id and a.owner_id = (select auth.uid()));
$$;

create function public.fn_is_blocked(p_a uuid, p_b uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.blocks b
    where (b.blocker_id = p_a and b.blocked_id = p_b)
       or (b.blocker_id = p_b and b.blocked_id = p_a)
  );
$$;

create function public.fn_is_enrolled(p_app_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.app_testers e
    where e.app_id = p_app_id
      and e.tester_id = (select auth.uid())
      and e.status in ('joined', 'completed')
  );
$$;

-- ---------------------------------------------------------------- C1: RLS on
alter table public.app_config          enable row level security;
alter table public.profiles            enable row level security;
alter table public.profile_private     enable row level security;
alter table public.notification_prefs  enable row level security;
alter table public.apps                enable row level security;
alter table public.app_testers         enable row level security;
alter table public.check_ins           enable row level security;
alter table public.app_daily_snapshots enable row level security;
alter table public.feedback            enable row level security;
alter table public.notifications       enable row level security;
alter table public.devices             enable row level security;
alter table public.blocks              enable row level security;
alter table public.reports             enable row level security;
alter table public.nudges              enable row level security;

-- ---------------------------------------------------------------- C4: grants (start from nothing)
revoke all on all tables    in schema public from anon, authenticated;
revoke all on all sequences in schema public from anon, authenticated;
revoke execute on all functions in schema public from public, anon, authenticated;

-- Future objects created in this schema stay locked until granted explicitly.
alter default privileges in schema public revoke all on tables    from anon, authenticated;
alter default privileges in schema public revoke all on sequences from anon, authenticated;
alter default privileges in schema public revoke execute on functions from public, anon, authenticated;

grant usage on schema public to anon, authenticated;

-- Helpers used inside policies / views must be callable by signed-in users.
grant execute on function public.fn_today()              to authenticated;
grant execute on function public.fn_config_int(text)     to authenticated;
grant execute on function public.fn_owns_app(uuid)       to authenticated;
grant execute on function public.fn_is_blocked(uuid, uuid) to authenticated;
grant execute on function public.fn_is_enrolled(uuid)    to authenticated;

-- Reads
grant select on public.profiles, public.profile_private, public.notification_prefs,
                public.apps, public.app_testers, public.check_ins, public.app_daily_snapshots,
                public.feedback, public.notifications, public.devices, public.blocks
  to authenticated;
grant select on public.v_enrollment_progress, public.v_app_progress, public.v_my_stats to authenticated;

-- Direct writes the app is allowed to make (everything else goes through rpc_* functions)
grant update (username, display_name, bio, avatar_url, is_public, show_history, find_by_email, theme, language)
  on public.profiles to authenticated;
grant update (play_email, device_model, android_version) on public.profile_private to authenticated;
grant update (remind, streak_risk, new_apps, tester_joined, tester_missed, reached_target, period_complete, weekly, remind_at)
  on public.notification_prefs to authenticated;
grant insert (user_id, fcm_token, platform), update (last_seen_at), delete on public.devices to authenticated;
grant insert (blocker_id, blocked_id), delete on public.blocks to authenticated;
grant update (read_at) on public.notifications to authenticated;
grant insert (reporter_id, target_type, target_id, reason) on public.reports to authenticated;
grant insert (app_id, tester_id, kind, body, screenshot_path) on public.feedback to authenticated;

-- ---------------------------------------------------------------- C2: identity tables
create policy profiles_select on public.profiles for select to authenticated
  using (id = (select auth.uid())
         or (is_public and not public.fn_is_blocked((select auth.uid()), id)));
create policy profiles_update on public.profiles for update to authenticated
  using (id = (select auth.uid())) with check (id = (select auth.uid()));

create policy profile_private_select on public.profile_private for select to authenticated
  using (user_id = (select auth.uid()));
create policy profile_private_update on public.profile_private for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create policy prefs_select on public.notification_prefs for select to authenticated
  using (user_id = (select auth.uid()));
create policy prefs_update on public.notification_prefs for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create policy devices_select on public.devices for select to authenticated
  using (user_id = (select auth.uid()));
create policy devices_insert on public.devices for insert to authenticated
  with check (user_id = (select auth.uid()));
create policy devices_update on public.devices for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
create policy devices_delete on public.devices for delete to authenticated
  using (user_id = (select auth.uid()));

create policy blocks_select on public.blocks for select to authenticated
  using (blocker_id = (select auth.uid()));
create policy blocks_insert on public.blocks for insert to authenticated
  with check (blocker_id = (select auth.uid()));
create policy blocks_delete on public.blocks for delete to authenticated
  using (blocker_id = (select auth.uid()));

-- ---------------------------------------------------------------- C3: app tables
create policy apps_select on public.apps for select to authenticated
  using (owner_id = (select auth.uid())
         or (status <> 'draft' and not public.fn_is_blocked((select auth.uid()), owner_id)));

create policy app_testers_select on public.app_testers for select to authenticated
  using (tester_id = (select auth.uid()) or public.fn_owns_app(app_id));

create policy check_ins_select on public.check_ins for select to authenticated
  using (tester_id = (select auth.uid()) or public.fn_owns_app(app_id));

create policy snapshots_select on public.app_daily_snapshots for select to authenticated
  using (public.fn_owns_app(app_id));

create policy feedback_select on public.feedback for select to authenticated
  using (tester_id = (select auth.uid()) or public.fn_owns_app(app_id));
create policy feedback_insert on public.feedback for insert to authenticated
  with check (tester_id = (select auth.uid()) and public.fn_is_enrolled(app_id));

create policy notifications_select on public.notifications for select to authenticated
  using (user_id = (select auth.uid()));
create policy notifications_update on public.notifications for update to authenticated
  using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create policy reports_insert on public.reports for insert to authenticated
  with check (reporter_id = (select auth.uid()));

-- app_config and nudges: RLS on, no policy, no grant => only service role / security definer functions.
