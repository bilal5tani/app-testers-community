-- 0018_realtime.sql  (plan task G1)
-- Live updates for the app: owner's Live progress (check_ins, app_testers) and the inbox (notifications).
-- Realtime applies the same select policies as normal queries, so a user only receives rows
-- they could read: an owner gets the check-ins of their own apps, a tester only their own rows.
-- Safe to run twice.

do $$
declare
  t text;
begin
  if not exists (select 1 from pg_publication where pubname = 'supabase_realtime') then
    create publication supabase_realtime;
  end if;

  foreach t in array array['check_ins', 'app_testers', 'notifications'] loop
    if not exists (select 1 from pg_publication_tables
                   where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = t) then
      execute format('alter publication supabase_realtime add table public.%I', t);
    end if;
  end loop;
end $$;
