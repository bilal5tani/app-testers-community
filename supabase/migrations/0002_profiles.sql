-- 0002_profiles.sql  (plan task B2)
-- profiles (public identity), profile_private (owner-only), notification_prefs,
-- and the trigger that creates all three when someone signs up.

create table public.profiles (
  id            uuid primary key references auth.users (id) on delete cascade,
  username      citext unique,
  display_name  text not null default 'Tester',
  bio           text,
  avatar_url    text,
  is_public     boolean not null default true,
  show_history  boolean not null default true,
  find_by_email boolean not null default false,
  theme         text not null default 'system',
  language      text not null default 'en',
  karma         int  not null default 0,
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now(),
  constraint profiles_username_format check (username is null or username ~ '^[a-z0-9._]{3,20}$'),
  constraint profiles_display_name_len check (char_length(display_name) between 1 and 40),
  constraint profiles_bio_len          check (bio is null or char_length(bio) <= 160),
  constraint profiles_theme_valid      check (theme in ('system', 'light', 'dark')),
  constraint profiles_language_valid   check (language in ('en', 'ur', 'rur'))
);

create table public.profile_private (
  user_id         uuid primary key references public.profiles (id) on delete cascade,
  play_email      text,
  device_model    text,
  android_version text,
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now(),
  constraint profile_private_email_format check (play_email is null or play_email ~* '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);

create table public.notification_prefs (
  user_id         uuid primary key references public.profiles (id) on delete cascade,
  remind          boolean not null default true,
  streak_risk     boolean not null default true,
  new_apps        boolean not null default false,
  tester_joined   boolean not null default true,
  tester_missed   boolean not null default true,
  reached_target  boolean not null default true,
  period_complete boolean not null default true,
  weekly          boolean not null default false,
  remind_at       time not null default '20:00',
  created_at      timestamptz not null default now(),
  updated_at      timestamptz not null default now()
);

create trigger trg_profiles_updated         before update on public.profiles          for each row execute function public.set_updated_at();
create trigger trg_profile_private_updated  before update on public.profile_private   for each row execute function public.set_updated_at();
create trigger trg_notification_prefs_updated before update on public.notification_prefs for each row execute function public.set_updated_at();

-- Runs when Supabase Auth creates a user (Google sign-in).
create function public.fn_handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_name text;
begin
  v_name := left(coalesce(nullif(new.raw_user_meta_data ->> 'full_name', ''),
                          nullif(new.raw_user_meta_data ->> 'name', ''),
                          'Tester'), 40);

  insert into public.profiles (id, display_name) values (new.id, v_name);
  insert into public.profile_private (user_id, play_email) values (new.id, new.email);
  insert into public.notification_prefs (user_id) values (new.id);
  return new;
end;
$$;

create trigger trg_on_auth_user_created
  after insert on auth.users
  for each row execute function public.fn_handle_new_user();
