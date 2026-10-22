-- 0007_views.sql  (plan task B7)
-- security_invoker = true so RLS of the querying user applies (added in Phase C).

-- Per enrolled tester: days checked, current streak, checked today.
create view public.v_enrollment_progress
with (security_invoker = true) as
with d as (
  select c.enrollment_id, c.day,
         c.day - (row_number() over (partition by c.enrollment_id order by c.day))::int as grp
  from public.check_ins c
),
islands as (
  select enrollment_id, count(*) as len, max(day) as last_day
  from d
  group by enrollment_id, grp
),
streaks as (
  select enrollment_id,
         coalesce(max(len) filter (where last_day >= public.fn_today() - 1), 0)::int as current_streak
  from islands
  group by enrollment_id
)
select e.id            as enrollment_id,
       e.app_id,
       e.tester_id,
       e.status,
       e.missed_streak,
       count(c.id)::int                                   as days_checked,
       max(c.day)                                         as last_check_in,
       coalesce(bool_or(c.day = public.fn_today()), false) as checked_today,
       coalesce(s.current_streak, 0)                      as current_streak
from public.app_testers e
left join public.check_ins c on c.enrollment_id = e.id
left join streaks s on s.enrollment_id = e.id
group by e.id, s.current_streak;

-- Per app: opted-in count, checked in today, spots left, qualified days.
create view public.v_app_progress
with (security_invoker = true) as
select a.id as app_id,
       a.owner_id,
       a.status,
       a.testers_needed,
       a.qualified_days,
       (select count(*) from public.app_testers e
         where e.app_id = a.id and e.status in ('joined', 'completed'))::int as opted_in,
       (select count(*) from public.check_ins c
         where c.app_id = a.id and c.day = public.fn_today())::int           as checked_in_today,
       greatest(a.testers_needed - (select count(*) from public.app_testers e
         where e.app_id = a.id and e.status in ('joined', 'completed')), 0)::int as spots_left
from public.apps a;

-- Per user: Profile screen stats.
create view public.v_my_stats
with (security_invoker = true) as
select p.id as user_id,
       (select count(*) from public.app_testers e
         where e.tester_id = p.id and e.status = 'completed')::int as apps_tested,
       coalesce((select max(v.current_streak) from public.v_enrollment_progress v
         where v.tester_id = p.id), 0)::int                          as best_streak,
       (select count(*) from public.app_testers e
         join public.apps a on a.id = e.app_id
         where a.owner_id = p.id and e.status in ('joined', 'completed'))::int as testers_received
from public.profiles p;
