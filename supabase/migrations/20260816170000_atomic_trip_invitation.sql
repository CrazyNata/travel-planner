-- Invitation retries must update both the user-facing member list and the
-- authorization table in one database transaction. The Edge Function invokes
-- this function with the service role only after it has resolved the invitee.
create or replace function public.upsert_trip_invitation(
    p_trip_id text,
    p_actor_id uuid,
    p_user_id uuid,
    p_email text,
    p_name text,
    p_role text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_payload jsonb;
    v_owner_id uuid;
    v_members jsonb;
    v_member jsonb;
    v_member_id text;
    v_existing_ordinal bigint;
    v_email text := lower(trim(coalesce(p_email, '')));
    v_name text := trim(coalesce(p_name, ''));
    v_initials text;
begin
    if p_actor_id is null or p_user_id is null then
        raise exception 'Invitation requires an actor and an invitee';
    end if;
    if v_email = '' or p_role not in ('Редактор', 'Читатель') then
        raise exception 'Invalid invitation';
    end if;

    select trip.payload, trip.owner_id
    into v_payload, v_owner_id
    from public.trips trip
    where trip.id = p_trip_id
    for update;

    if not found or v_owner_id <> p_actor_id then
        raise exception 'Trip not found or only the owner can invite members';
    end if;

    v_members := coalesce(v_payload -> 'members', '[]'::jsonb);
    if jsonb_typeof(v_members) <> 'array' then
        raise exception 'Invalid members payload';
    end if;

    select items.member, items.ordinality
    into v_member, v_existing_ordinal
    from jsonb_array_elements(v_members) with ordinality as items(member, ordinality)
    where lower(trim(coalesce(items.member ->> 'email', ''))) = v_email
       or items.member ->> 'userId' = p_user_id::text
    order by items.ordinality
    limit 1;

    v_member_id := nullif(trim(coalesce(v_member ->> 'id', '')), '');
    if v_member_id is null then
        v_member_id := md5(p_trip_id || ':' || p_user_id::text);
    end if;
    if v_name = '' then
        v_name := split_part(v_email, '@', 1);
    end if;
    v_initials := upper(left(regexp_replace(v_name, '[^[:alnum:][:alpha:]]', '', 'g'), 2));

    v_member := coalesce(v_member, '{}'::jsonb) || jsonb_build_object(
        'id', v_member_id,
        'name', v_name,
        'email', v_email,
        'role', p_role,
        'userId', p_user_id::text,
        'initials', coalesce(nullif(v_initials, ''), upper(left(v_name, 2))),
        'tone', coalesce(nullif(v_member ->> 'tone', ''), 'blue')
    );

    if v_existing_ordinal is null then
        v_members := v_members || jsonb_build_array(v_member);
    else
        select coalesce(
            jsonb_agg(
                case
                    when items.ordinality = v_existing_ordinal
                        then v_member
                    else items.member
                end
                order by items.ordinality
            ) filter (
                where items.ordinality = v_existing_ordinal
                   or not (
                       lower(trim(coalesce(items.member ->> 'email', ''))) = v_email
                       or items.member ->> 'userId' = p_user_id::text
                   )
            ),
            jsonb_build_array(v_member)
        )
        into v_members
        from jsonb_array_elements(v_members) with ordinality as items(member, ordinality);
    end if;

    if p_user_id <> v_owner_id then
        insert into public.trip_collaborators (trip_id, user_id, role)
        values (p_trip_id, p_user_id, p_role)
        on conflict (trip_id, user_id)
        do update set role = excluded.role;
    end if;

    update public.trips
    set payload = jsonb_set(coalesce(payload, '{}'::jsonb), '{members}', v_members, true),
        revision = coalesce(revision, 0) + 1,
        updated_at = now()
    where id = p_trip_id;

    return jsonb_build_object('ok', true, 'memberId', v_member_id, 'role', p_role);
end;
$$;

revoke all on function public.upsert_trip_invitation(text, uuid, uuid, text, text, text) from public;
revoke all on function public.upsert_trip_invitation(text, uuid, uuid, text, text, text) from anon;
revoke all on function public.upsert_trip_invitation(text, uuid, uuid, text, text, text) from authenticated;
grant execute on function public.upsert_trip_invitation(text, uuid, uuid, text, text, text) to service_role;
