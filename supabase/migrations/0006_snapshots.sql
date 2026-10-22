-- 0006_snapshots.sql  (plan task B6)
-- One row per app per day, written by the nightly rollover (E1).

create table public.app_daily_snapshots (
  app_id     uuid not null references public.apps (id) on delete cascade,
  day        date not null,
  opted_in   int  not null,
  checked_in int  not null,
  qualified  boolean not null,
  created_at timestamptz not null default now(),
  primary key (app_id, day),
  constraint snapshots_counts_nonneg check (opted_in >= 0 and checked_in >= 0)
);
