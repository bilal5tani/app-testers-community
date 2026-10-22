-- 0014_moderation_views.sql (plan Phase L task L3)
-- View for daily manual review of open reports.

create view public.v_open_reports
with (security_invoker = true)
as
select
  r.id,
  r.reporter_id,
  p.display_name as reporter_name,
  r.target_type,
  r.target_id,
  r.reason,
  r.status,
  r.created_at
from public.reports r
left join public.profiles p on p.id = r.reporter_id
where r.status = 'open'
order by r.created_at desc;

grant select on public.v_open_reports to authenticated;
