create or replace function public.leave_trip(p_trip_id text)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_actor_id uuid := auth.uid();
    v_actor_email text;
    v_trip_owner uuid;
    v_payload jsonb;
    v_members jsonb;
begin
    if v_actor_id is null then
        raise exception 'Authentication required';
    end if;

    select u.email
    into v_actor_email
    from auth.users as u
    where u.id = v_actor_id;

    select t.owner_id, coalesce(t.payload, '{}'::jsonb)
    into v_trip_owner, v_payload
    from public.trips as t
    where t.id = p_trip_id
    for update;

    if not found then
        raise exception 'Путешествие не найдено';
    end if;

    if v_trip_owner = v_actor_id then
        raise exception 'Владелец должен удалить путешествие, а не выходить из него';
    end if;

    if not exists (
        select 1
        from public.trip_collaborators as c
        where c.trip_id = p_trip_id
          and c.user_id = v_actor_id
    ) then
        raise exception 'Вы не являетесь участником этого путешествия';
    end if;

    delete from public.trip_collaborators as c
    where c.trip_id = p_trip_id
      and c.user_id = v_actor_id;

    if jsonb_typeof(v_payload -> 'members') = 'array' then
        select coalesce(
            jsonb_agg(item.member order by item.ordinal),
            '[]'::jsonb
        )
        into v_members
        from jsonb_array_elements(v_payload -> 'members') with ordinality as item(member, ordinal)
        where coalesce(item.member ->> 'userId', '') <> v_actor_id::text
          and (
              v_actor_email is null
              or lower(trim(coalesce(item.member ->> 'email', ''))) <> lower(trim(v_actor_email))
          );

        v_payload := jsonb_set(v_payload, '{members}', v_members, true);
    end if;

    update public.trips as t
    set payload = v_payload,
        revision = coalesce(t.revision, 0) + 1,
        updated_at = now()
    where t.id = p_trip_id;

    return jsonb_build_object('ok', true, 'tripId', p_trip_id);
end;
$function$;

revoke all on function public.leave_trip(text) from public;
revoke all on function public.leave_trip(text) from anon;
grant execute on function public.leave_trip(text) to authenticated;
