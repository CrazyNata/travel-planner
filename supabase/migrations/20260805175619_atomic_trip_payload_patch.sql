create or replace function public.patch_trip_payload(
    p_trip_id text,
    p_patch jsonb
)
returns void
language plpgsql
security invoker
set search_path = public
as $$
begin
    update public.trips
    set payload = coalesce(payload, '{}'::jsonb) || coalesce(p_patch, '{}'::jsonb)
    where id = p_trip_id;

    if not found then
        raise exception 'Trip not found or not editable';
    end if;
end;
$$;

revoke all on function public.patch_trip_payload(text, jsonb) from public;
grant execute on function public.patch_trip_payload(text, jsonb) to authenticated;
