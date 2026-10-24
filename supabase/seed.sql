-- seed.sql  (plan task B9) — LOCAL DEVELOPMENT ONLY, runs on `supabase db reset`.
-- Never push this to the hosted project. Fake users have no password; to sign in as one of
-- them locally use Studio, or test through the rpc functions with a JWT.
--
-- Users 1..14:  user01 .. user14 (display names Ayesha, Bilal, ...).
-- App A (owner user01): recruiting, 5 of 12 testers, a few days of check-ins.
-- App B (owner user02): testing, all 12 seats taken, 6 qualified days.
-- App C (owner user03): recruiting, nobody yet.

insert into auth.users (id, email, raw_user_meta_data)
select ('00000000-0000-0000-0000-' || lpad(g::text, 12, '0'))::uuid,
       'user' || lpad(g::text, 2, '0') || '@example.com',
       jsonb_build_object('full_name', (array['Ayesha','Bilal','Chand','Danish','Eman','Farhan','Gul','Hina',
                                              'Imran','Javeria','Kamran','Laiba','Mustafa','Nida'])[g])
from generate_series(1, 14) g;

update public.profiles p set
  username = lower(p.display_name) || '.dev',
  bio = 'Android developer, testing apps for the community.',
  karma = (random() * 8)::int;

update public.profile_private pp set
  play_email = lower(p.display_name) || '.testing@gmail.com',
  device_model = (array['Pixel 7','Galaxy A54','Redmi Note 12','Moto G84'])[1 + (random() * 3)::int],
  android_version = (array['12','13','14'])[1 + (random() * 2)::int]
from public.profiles p where p.id = pp.user_id;

insert into public.apps (id, owner_id, name, package_name, category, description, opt_in_url, group_url,
                         testers_needed, status, listed_at) values
 ('a0000000-0000-0000-0000-00000000000a', '00000000-0000-0000-0000-000000000001', 'Habit Tracker', 'com.seed.habits', 'lifestyle',
  'Simple daily habit tracking.', 'https://play.google.com/apps/testing/com.seed.habits', 'https://groups.google.com/g/seed-habits', 12, 'recruiting', now() - interval '3 days'),
 ('a0000000-0000-0000-0000-00000000000b', '00000000-0000-0000-0000-000000000002', 'Quick Notes', 'com.seed.notes', 'tools',
  'Fast offline notes.', 'https://play.google.com/apps/testing/com.seed.notes', 'https://groups.google.com/g/seed-notes', 12, 'testing', now() - interval '8 days'),
 ('a0000000-0000-0000-0000-00000000000c', '00000000-0000-0000-0000-000000000003', 'Word Puzzle', 'com.seed.puzzle', 'games',
  'Daily word puzzle.', 'https://play.google.com/apps/testing/com.seed.puzzle', 'https://groups.google.com/g/seed-puzzle', 12, 'recruiting', now() - interval '1 day');

-- App A: users 6..10 joined 3 days ago
insert into public.app_testers (app_id, tester_id, joined_at)
select 'a0000000-0000-0000-0000-00000000000a', ('00000000-0000-0000-0000-' || lpad(g::text, 12, '0'))::uuid, now() - interval '3 days'
from generate_series(6, 10) g;

-- App B: users 1, 3..13 (12 testers, owner is user02)
insert into public.app_testers (app_id, tester_id, joined_at)
select 'a0000000-0000-0000-0000-00000000000b', ('00000000-0000-0000-0000-' || lpad(g::text, 12, '0'))::uuid, now() - interval '7 days'
from unnest(array[1,3,4,5,6,7,8,9,10,11,12,13]) g;
update public.apps set qualified_days = 6 where id = 'a0000000-0000-0000-0000-00000000000b';

-- check-ins: app A, last 3 days (user10 skips yesterday); app B, last 6 days (users 12 and 13 miss some)
insert into public.check_ins (enrollment_id, day)
select e.id, public.fn_today() - d
from public.app_testers e, generate_series(0, 2) d
where e.app_id = 'a0000000-0000-0000-0000-00000000000a'
  and not (e.tester_id = '00000000-0000-0000-0000-000000000010' and d = 1);

insert into public.check_ins (enrollment_id, day)
select e.id, public.fn_today() - d
from public.app_testers e, generate_series(0, 5) d
where e.app_id = 'a0000000-0000-0000-0000-00000000000b'
  and not (e.tester_id = '00000000-0000-0000-0000-000000000012' and d in (0, 1))
  and not (e.tester_id = '00000000-0000-0000-0000-000000000013' and d in (1, 2, 3));
update public.app_testers set missed_streak = 1
 where app_id = 'a0000000-0000-0000-0000-00000000000b' and tester_id = '00000000-0000-0000-0000-000000000012';

insert into public.app_daily_snapshots (app_id, day, opted_in, checked_in, qualified)
select 'a0000000-0000-0000-0000-00000000000b', public.fn_today() - d, 12, 10 + (d % 2), true
from generate_series(1, 6) d;

insert into public.notifications (user_id, type, title, body, data) values
 ('00000000-0000-0000-0000-000000000001', 'tester_joined', 'New tester joined', 'Gul joined Habit Tracker', '{"route":"app/a0000000-0000-0000-0000-00000000000a/progress"}'),
 ('00000000-0000-0000-0000-000000000002', 'reached_target', 'All testers found', 'Quick Notes has all 12 testers.', '{"route":"app/a0000000-0000-0000-0000-00000000000b/progress"}');
