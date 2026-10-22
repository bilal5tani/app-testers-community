-- 0005_social.sql  (plan task B5)
-- feedback, notifications, devices, blocks, reports, nudges.

create table public.feedback (
  id              uuid primary key default gen_random_uuid(),
  app_id          uuid not null references public.apps (id) on delete cascade,
  tester_id       uuid not null references public.profiles (id) on delete cascade,
  kind            text not null,
  body            text not null,
  screenshot_path text,
  created_at      timestamptz not null default now(),
  constraint feedback_kind_valid check (kind in ('bug', 'suggestion', 'praise')),
  constraint feedback_body_len   check (char_length(body) between 1 and 1000)
);

create table public.notifications (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references public.profiles (id) on delete cascade,
  type       text not null,
  title      text not null,
  body       text not null,
  data       jsonb not null default '{}'::jsonb,
  read_at    timestamptz,
  created_at timestamptz not null default now()
);

create table public.devices (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references public.profiles (id) on delete cascade,
  fcm_token    text not null unique,
  platform     text not null default 'android',
  last_seen_at timestamptz not null default now(),
  created_at   timestamptz not null default now()
);

create table public.blocks (
  blocker_id uuid not null references public.profiles (id) on delete cascade,
  blocked_id uuid not null references public.profiles (id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  constraint blocks_not_self check (blocker_id <> blocked_id)
);

create table public.reports (
  id          uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles (id) on delete cascade,
  target_type text not null,
  target_id   uuid not null,
  reason      text not null,
  status      text not null default 'open',
  created_at  timestamptz not null default now(),
  constraint reports_target_type_valid check (target_type in ('user', 'app', 'feedback')),
  constraint reports_status_valid      check (status in ('open', 'closed')),
  constraint reports_reason_len        check (char_length(reason) between 1 and 500)
);

create table public.nudges (
  id         uuid primary key default gen_random_uuid(),
  app_id     uuid not null references public.apps (id) on delete cascade,
  tester_id  uuid not null references public.profiles (id) on delete cascade,
  sender_id  uuid not null references public.profiles (id) on delete cascade,
  day        date not null default public.fn_today(),
  created_at timestamptz not null default now(),
  constraint nudges_once_per_day unique (app_id, tester_id, day)
);
