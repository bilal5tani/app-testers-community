-- 0015_fix_foundation.sql
-- Fixes found while testing 0001-0014 against a real database. Safe to run whether or not
-- earlier files were edited: every statement is idempotent.

-- 1. username must be plain lowercase text. citext's comparison functions are revoked
--    from signed-in users by 0009, so any query on a citext column failed with
--    "permission denied for function citext_eq".
alter table public.profiles
  alter column username type text using lower(username::text);

-- the check constraint was built with citext's regex operator: rebuild it on plain text
alter table public.profiles drop constraint if exists profiles_username_format;
alter table public.profiles
  add constraint profiles_username_format check (username is null or username ~ '^[a-z0-9._]{3,20}$');

do $$
begin
  drop extension if exists citext;
exception when others then
  null; -- still referenced somewhere: harmless, leave it
end $$;

-- 2. app_config is locked by RLS, so these two helpers must run as their owner.
create or replace function public.fn_config_int(p_key text)
returns int
language sql
stable
security definer
set search_path = ''
as $$
  select c.value::int from public.app_config c where c.key = p_key;
$$;

create or replace function public.fn_today()
returns date
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(
    (select nullif(c.value, '')::date from public.app_config c where c.key = 'today_override'),
    (now() at time zone (select c.value from public.app_config c where c.key = 'timezone'))::date
  );
$$;

-- 3. Postgres gives PUBLIC execute on every new function, and 0009's schema-level revoke
--    does not remove that built-in default. Close it for future functions, and re-lock
--    everything that exists now (authenticated keeps its explicit grants).
alter default privileges for role postgres revoke execute on functions from public;
revoke execute on all functions in schema public from public, anon;

grant execute on function public.fn_today()          to authenticated;
grant execute on function public.fn_config_int(text) to authenticated;
