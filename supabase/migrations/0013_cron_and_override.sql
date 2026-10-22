-- 0013_cron_and_override.sql (plan Phase E tasks E2 and E5)
-- Test hook for fn_today() and pg_cron schedule for daily rollover.

-- E5: Support today_override in app_config
create or replace function public.fn_today()
returns date
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(
    (select value::date from public.app_config where key = 'today_override' and value <> ''),
    (now() at time zone (select c.value from public.app_config c where c.key = 'timezone'))::date
  );
$$;

-- Grant execute to authenticated
grant execute on function public.fn_today() to authenticated;


-- E2: pg_cron setup for daily rollover at 00:05 Asia/Karachi (19:05 UTC)
create extension if not exists pg_cron with schema extensions;

do $$
begin
  if exists (select 1 from cron.job where jobname = 'daily-rollover-job') then
    perform cron.unschedule('daily-rollover-job');
  end if;
end;
$$;

select cron.schedule(
  'daily-rollover-job',
  '5 19 * * *',
  $$select public.fn_daily_rollover();$$
);
