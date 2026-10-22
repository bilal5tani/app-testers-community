-- 0001_core.sql  (plan task B1)
-- Extensions, enums, app_config, fn_today(), updated_at trigger function.

create extension if not exists citext;

create type public.app_category  as enum ('tools', 'games', 'education', 'lifestyle', 'other');
create type public.app_status    as enum ('draft', 'recruiting', 'testing', 'completed', 'paused');
create type public.enroll_status as enum ('joined', 'dropped', 'completed', 'removed');

-- Server-side settings. Only functions / service role read this (RLS added in C1).
create table public.app_config (
  key   text primary key,
  value text not null
);

insert into public.app_config (key, value) values
  ('timezone',               'Asia/Karachi'),
  ('required_days',          '14'),
  ('max_active_tests',       '5'),
  ('auto_drop_after_missed', '3');

-- Single source of "today" for all day logic (plan convention).
create function public.fn_today()
returns date
language sql
stable
set search_path = ''
as $$
  select (now() at time zone (select c.value from public.app_config c where c.key = 'timezone'))::date;
$$;

create function public.fn_config_int(p_key text)
returns int
language sql
stable
set search_path = ''
as $$
  select c.value::int from public.app_config c where c.key = p_key;
$$;

create function public.set_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;
