-- 0008_indexes.sql  (plan task B8)

create index apps_discover_idx       on public.apps (status, category, listed_at desc);
create index apps_owner_idx          on public.apps (owner_id);
create index app_testers_tester_idx  on public.app_testers (tester_id, status);
create index app_testers_app_idx     on public.app_testers (app_id, status);
create index check_ins_app_day_idx   on public.check_ins (app_id, day);
create index check_ins_tester_day_idx on public.check_ins (tester_id, day desc);
create index notifications_inbox_idx on public.notifications (user_id, read_at, created_at desc);
create index devices_user_idx        on public.devices (user_id);
create index feedback_app_idx        on public.feedback (app_id, created_at desc);
create index reports_open_idx        on public.reports (status, created_at desc);
