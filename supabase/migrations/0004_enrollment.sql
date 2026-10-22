-- 0004_enrollment.sql  (plan task B4)
-- app_testers (one row per tester per app) and check_ins.

create table public.app_testers (
  id            uuid primary key default gen_random_uuid(),
  app_id        uuid not null references public.apps (id) on delete cascade,
  tester_id     uuid not null references public.profiles (id) on delete cascade,
  status        public.enroll_status not null default 'joined',
  joined_at     timestamptz not null default now(),
  left_at       timestamptz,
  missed_streak int not null default 0,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  constraint app_testers_unique unique (app_id, tester_id),
  constraint app_testers_missed_nonneg check (missed_streak >= 0)
);

create trigger trg_app_testers_updated before update on public.app_testers
  for each row execute function public.set_updated_at();

-- A developer cannot test their own app.
create function public.fn_block_own_app_enrollment()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if exists (select 1 from public.apps a where a.id = new.app_id and a.owner_id = new.tester_id) then
    raise exception 'own_app' using errcode = 'P0001';
  end if;
  return new;
end;
$$;

create trigger trg_app_testers_not_own
  before insert on public.app_testers
  for each row execute function public.fn_block_own_app_enrollment();

create table public.check_ins (
  id            uuid primary key default gen_random_uuid(),
  enrollment_id uuid not null references public.app_testers (id) on delete cascade,
  app_id        uuid not null references public.apps (id) on delete cascade,
  tester_id     uuid not null references public.profiles (id) on delete cascade,
  day           date not null default public.fn_today(),
  note          text,
  created_at    timestamptz not null default now(),
  constraint check_ins_one_per_day unique (enrollment_id, day),
  constraint check_ins_note_len    check (note is null or char_length(note) <= 200)
);

-- app_id / tester_id are copies kept for fast filters and realtime; always derive them from the enrollment.
create function public.fn_check_in_fill()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  select e.app_id, e.tester_id into new.app_id, new.tester_id
  from public.app_testers e where e.id = new.enrollment_id;
  return new;
end;
$$;

create trigger trg_check_ins_fill
  before insert on public.check_ins
  for each row execute function public.fn_check_in_fill();
