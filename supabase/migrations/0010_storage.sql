-- 0010_storage.sql (plan task C5)
-- Storage buckets avatars, app-icons, feedback with size and type limits and path policies.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
  ('avatars',   'avatars',   true,  1048576, array['image/png', 'image/jpeg', 'image/webp']),
  ('app-icons', 'app-icons', true,  524288,  array['image/png', 'image/jpeg', 'image/webp']),
  ('feedback',  'feedback',  false, 3145728, array['image/png', 'image/jpeg', 'image/webp'])
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

-- ---------------------------------------------------------------- Avatars
create policy "Public Read Avatars"
  on storage.objects for select
  using (bucket_id = 'avatars');

create policy "User Upload Avatar"
  on storage.objects for insert
  to authenticated
  with check (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

create policy "User Update Avatar"
  on storage.objects for update
  to authenticated
  using (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

create policy "User Delete Avatar"
  on storage.objects for delete
  to authenticated
  using (
    bucket_id = 'avatars'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

-- ---------------------------------------------------------------- App Icons
create policy "Public Read App Icons"
  on storage.objects for select
  using (bucket_id = 'app-icons');

create policy "User Upload App Icon"
  on storage.objects for insert
  to authenticated
  with check (
    bucket_id = 'app-icons'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

create policy "User Update App Icon"
  on storage.objects for update
  to authenticated
  using (
    bucket_id = 'app-icons'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

create policy "User Delete App Icon"
  on storage.objects for delete
  to authenticated
  using (
    bucket_id = 'app-icons'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );

-- ---------------------------------------------------------------- Feedback
create policy "Feedback Read Access"
  on storage.objects for select
  to authenticated
  using (
    bucket_id = 'feedback'
    and (
      (storage.foldername(name))[1] = (select auth.uid())::text
      or exists (
        select 1 from public.feedback f
        join public.apps a on a.id = f.app_id
        where a.owner_id = (select auth.uid())
          and f.screenshot_path = name
      )
    )
  );

create policy "User Upload Feedback"
  on storage.objects for insert
  to authenticated
  with check (
    bucket_id = 'feedback'
    and (storage.foldername(name))[1] = (select auth.uid())::text
  );
