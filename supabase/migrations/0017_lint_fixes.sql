-- 0017_lint_fixes.sql
-- Clears the warnings from `supabase db lint`.

-- fn_raise only raises an error, it writes nothing. Declaring it STABLE (instead of the
-- default VOLATILE) lets read-only rpc functions that call it be STABLE without a warning.
alter function public.fn_raise(text, text) stable;

-- explicit enum casts (no behaviour change)
create or replace function public.rpc_update_app(p_app_id uuid, p_fields jsonb)
returns public.apps
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := (select auth.uid());
  v_app public.apps;
  v_taken int;
  v_needed int;
  v_status public.app_status;
  v_constraint text;
  v_key text;
begin
  if v_uid is null then perform public.fn_raise('not_authenticated'); end if;
  if p_fields is null or jsonb_typeof(p_fields) <> 'object' then
    perform public.fn_raise('invalid_input', 'fields');
  end if;

  for v_key in select jsonb_object_keys(p_fields) loop
    if v_key not in ('name', 'category', 'description', 'icon_url', 'opt_in_url',
                     'group_url', 'testers_needed', 'status') then
      perform public.fn_raise('invalid_input', v_key);
    end if;
  end loop;

  select * into v_app from public.apps where id = p_app_id for update;
  if not found then perform public.fn_raise('not_found'); end if;
  if v_app.owner_id <> v_uid then perform public.fn_raise('forbidden'); end if;
  if v_app.status in ('draft', 'completed') then perform public.fn_raise('app_not_open'); end if;

  select count(*) into v_taken from public.app_testers e
  where e.app_id = p_app_id and e.status in ('joined', 'completed');

  v_needed := case when p_fields ? 'testers_needed' then (p_fields ->> 'testers_needed')::int
                   else v_app.testers_needed end;
  if v_needed < v_taken then perform public.fn_raise('invalid_input', 'testers_needed_below_joined'); end if;

  v_status := v_app.status;
  if p_fields ? 'status' then
    case p_fields ->> 'status'
      when 'paused' then v_status := 'paused'::public.app_status;
      when 'active' then
        if v_app.status <> 'paused' then perform public.fn_raise('invalid_input', 'status'); end if;
        v_status := 'recruiting'::public.app_status;
      else perform public.fn_raise('invalid_input', 'status');
    end case;
  end if;
  -- an open app is "testing" once every seat is taken, "recruiting" otherwise
  if v_status in ('recruiting', 'testing') then
    v_status := case when v_taken >= v_needed then 'testing'::public.app_status else 'recruiting'::public.app_status end;
  end if;

  begin
    update public.apps a set
      name           = case when p_fields ? 'name'        then btrim(p_fields ->> 'name') else a.name end,
      category       = case when p_fields ? 'category'    then (p_fields ->> 'category')::public.app_category else a.category end,
      description    = case when p_fields ? 'description' then nullif(btrim(p_fields ->> 'description'), '') else a.description end,
      icon_url       = case when p_fields ? 'icon_url'    then nullif(btrim(p_fields ->> 'icon_url'), '') else a.icon_url end,
      opt_in_url     = case when p_fields ? 'opt_in_url'  then btrim(p_fields ->> 'opt_in_url') else a.opt_in_url end,
      group_url      = case when p_fields ? 'group_url'   then btrim(p_fields ->> 'group_url') else a.group_url end,
      testers_needed = v_needed,
      status         = v_status
    where a.id = p_app_id
    returning * into v_app;
  exception
    when check_violation or unique_violation or not_null_violation then
      get stacked diagnostics v_constraint = constraint_name;
      perform public.fn_raise('invalid_input', v_constraint);
    when invalid_text_representation then
      perform public.fn_raise('invalid_input', 'bad_value');
  end;

  return v_app;
end;
$$;
