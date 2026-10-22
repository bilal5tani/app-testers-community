-- 0003_apps.sql  (plan task B3)

create table public.apps (
  id              uuid primary key default gen_random_uuid(),
  owner_id        uuid not null references public.profiles (id) on delete cascade,
  name            text not null,
  package_name    text not null unique,
  category        public.app_category not null default 'tools',
  description     text,
  icon_url        text,
  opt_in_url      text not null,
  group_url       text not null,
  testers_needed  int  not null default 12,
  status          public.app_status not null default 'draft',
  listed_at       timestamptz,
  qualified_days  int  not null default 0,
  completed_at    timestamptz,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  constraint apps_name_len        check (char_length(name) between 2 and 40),
  constraint apps_package_format  check (package_name ~ '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$'),
  constraint apps_description_len check (description is null or char_length(description) <= 300),
  constraint apps_opt_in_prefix   check (opt_in_url like 'https://play.google.com/%'),
  constraint apps_group_prefix    check (group_url  like 'https://groups.google.com/%'),
  constraint apps_testers_needed  check (testers_needed in (12, 15, 20)),
  constraint apps_qualified_range check (qualified_days between 0 and 14)
);

create trigger trg_apps_updated before update on public.apps
  for each row execute function public.set_updated_at();
