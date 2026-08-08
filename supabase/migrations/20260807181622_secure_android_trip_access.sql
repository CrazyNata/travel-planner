-- Keep existing object names intact while switching Android trip photos to
-- authenticated access. The Android client resolves both legacy public URLs
-- and new storage:// references to short-lived signed URLs.
alter table public.trips
add column if not exists revision bigint not null default 0;

-- Keep the existing two-argument RPC working for already-installed Android
-- versions, but make every legacy write advance the revision counter.
create or replace function public.patch_trip_payload(
    p_trip_id text,
    p_patch jsonb
)
returns void
language plpgsql
security invoker
set search_path = ''
as $$
begin
    update public.trips
    set payload = coalesce(payload, '{}'::jsonb) || coalesce(p_patch, '{}'::jsonb),
        revision = revision + 1,
        updated_at = now()
    where id = p_trip_id;

    if not found then
        raise exception 'Trip not found or not editable';
    end if;
end;
$$;

-- New Android versions use optimistic concurrency control. A stale screen is
-- rejected instead of silently replacing a newer value from another device.
create or replace function public.patch_trip_payload(
    p_trip_id text,
    p_patch jsonb,
    p_expected_revision bigint
)
returns void
language plpgsql
security invoker
set search_path = ''
as $$
begin
    update public.trips
    set payload = coalesce(payload, '{}'::jsonb) || coalesce(p_patch, '{}'::jsonb),
        revision = revision + 1,
        updated_at = now()
    where id = p_trip_id
      and revision = p_expected_revision;

    if not found then
        if exists (select 1 from public.trips where id = p_trip_id) then
            raise exception using
                errcode = '40001',
                message = 'Данные изменились на другом устройстве. Обновите экран и повторите.';
        end if;
        raise exception 'Trip not found or not editable';
    end if;
end;
$$;

revoke all on function public.patch_trip_payload(text, jsonb) from public;
revoke all on function public.patch_trip_payload(text, jsonb, bigint) from public;
grant execute on function public.patch_trip_payload(text, jsonb) to authenticated;
grant execute on function public.patch_trip_payload(text, jsonb, bigint) to authenticated;

update storage.buckets
set public = false
where id = 'trip-photos';

drop policy if exists "authenticated users can upload trip photos" on storage.objects;
drop policy if exists "users can read accessible trip photos" on storage.objects;
drop policy if exists "users can upload accessible trip photos" on storage.objects;
drop policy if exists "users can update accessible trip photos" on storage.objects;
drop policy if exists "users can delete own trip photos" on storage.objects;

create policy "users can read accessible trip photos"
on storage.objects
for select
to authenticated
using (
    bucket_id = 'trip-photos'
    and (
        (
            (storage.foldername(name))[1] = (select auth.uid())::text
            and (storage.foldername(name))[2] = 'profile'
        )
        or (
            (storage.foldername(name))[2] <> 'profile'
            and exists (
                select 1
                from public.trips trip
                where trip.id = (storage.foldername(name))[2]
                  and (
                      trip.owner_id = (select auth.uid())
                      or exists (
                          select 1
                          from public.trip_collaborators collaborator
                          where collaborator.trip_id = trip.id
                            and collaborator.user_id = (select auth.uid())
                      )
                  )
            )
        )
    )
);

create policy "users can upload accessible trip photos"
on storage.objects
for insert
to authenticated
with check (
    bucket_id = 'trip-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and (
        (storage.foldername(name))[2] = 'profile'
        or exists (
            select 1
            from public.trips trip
            where trip.id = (storage.foldername(name))[2]
              and (
                  trip.owner_id = (select auth.uid())
                  or exists (
                      select 1
                      from public.trip_collaborators collaborator
                      where collaborator.trip_id = trip.id
                        and collaborator.user_id = (select auth.uid())
                        and collaborator.role = 'Редактор'
                  )
              )
        )
    )
);

create policy "users can update accessible trip photos"
on storage.objects
for update
to authenticated
using (
    bucket_id = 'trip-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and (
        (storage.foldername(name))[2] = 'profile'
        or exists (
            select 1
            from public.trips trip
            where trip.id = (storage.foldername(name))[2]
              and (
                  trip.owner_id = (select auth.uid())
                  or exists (
                      select 1
                      from public.trip_collaborators collaborator
                      where collaborator.trip_id = trip.id
                        and collaborator.user_id = (select auth.uid())
                        and collaborator.role = 'Редактор'
                  )
              )
        )
    )
)
with check (
    bucket_id = 'trip-photos'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and (
        (storage.foldername(name))[2] = 'profile'
        or exists (
            select 1
            from public.trips trip
            where trip.id = (storage.foldername(name))[2]
              and (
                  trip.owner_id = (select auth.uid())
                  or exists (
                      select 1
                      from public.trip_collaborators collaborator
                      where collaborator.trip_id = trip.id
                        and collaborator.user_id = (select auth.uid())
                        and collaborator.role = 'Редактор'
                  )
              )
        )
    )
);

create policy "users can delete own trip photos"
on storage.objects
for delete
to authenticated
using (
    bucket_id = 'trip-photos'
    and (
        (
            (storage.foldername(name))[1] = (select auth.uid())::text
            and (storage.foldername(name))[2] = 'profile'
        )
        or exists (
            select 1
            from public.trips trip
            where trip.id = (storage.foldername(name))[2]
              and (
                  trip.owner_id = (select auth.uid())
                  or (
                      (storage.foldername(name))[1] = (select auth.uid())::text
                      and exists (
                          select 1
                          from public.trip_collaborators collaborator
                          where collaborator.trip_id = trip.id
                            and collaborator.user_id = (select auth.uid())
                            and collaborator.role = 'Редактор'
                      )
                  )
              )
        )
    )
);

create index if not exists trip_collaborators_user_id_idx
on public.trip_collaborators (user_id);

-- Role changes and removals must update the user-facing member list and the
-- authorization table in one database transaction.
create or replace function public.manage_trip_member(
    p_trip_id text,
    p_member_id text,
    p_role text default null,
    p_delete boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_actor_id uuid := auth.uid();
    v_payload jsonb;
    v_members jsonb;
    v_member jsonb;
    v_member_email text;
    v_member_user_id uuid;
begin
    if v_actor_id is null then
        raise exception 'Authentication required';
    end if;

    select trip.payload
    into v_payload
    from public.trips trip
    where trip.id = p_trip_id
      and trip.owner_id = v_actor_id
    for update;

    if not found then
        raise exception 'Trip not found or only the owner can manage members';
    end if;

    v_members := coalesce(v_payload -> 'members', '[]'::jsonb);
    if jsonb_typeof(v_members) <> 'array' then
        raise exception 'Invalid members payload';
    end if;

    select member
    into v_member
    from jsonb_array_elements(v_members) member
    where member ->> 'id' = p_member_id
    limit 1;

    if v_member is null then
        raise exception 'Member not found';
    end if;

    v_member_email := lower(trim(v_member ->> 'email'));
    select user_row.id
    into v_member_user_id
    from auth.users user_row
    where lower(user_row.email) = v_member_email
    limit 1;

    if p_delete then
        if v_member_user_id is not null then
            delete from public.trip_collaborators
            where trip_id = p_trip_id
              and user_id = v_member_user_id;
        end if;

        select coalesce(jsonb_agg(member order by ordinal), '[]'::jsonb)
        into v_members
        from jsonb_array_elements(v_members) with ordinality as items(member, ordinal)
        where member ->> 'id' <> p_member_id;
    else
        if p_role not in ('Редактор', 'Читатель') then
            raise exception 'Invalid member role';
        end if;
        if v_member_user_id is null then
            raise exception 'Invited user was not found';
        end if;

        insert into public.trip_collaborators (trip_id, user_id, role)
        values (p_trip_id, v_member_user_id, p_role)
        on conflict (trip_id, user_id)
        do update set role = excluded.role;

        select coalesce(
            jsonb_agg(
                case
                    when member ->> 'id' = p_member_id
                        then member || jsonb_build_object('role', p_role, 'userId', v_member_user_id::text)
                    else member
                end
                order by ordinal
            ),
            '[]'::jsonb
        )
        into v_members
        from jsonb_array_elements(v_members) with ordinality as items(member, ordinal);
    end if;

    update public.trips
    set payload = jsonb_set(coalesce(payload, '{}'::jsonb), '{members}', v_members, true),
        revision = revision + 1,
        updated_at = now()
    where id = p_trip_id;

    return jsonb_build_object(
        'ok', true,
        'memberId', p_member_id,
        'deleted', p_delete,
        'role', p_role
    );
end;
$$;

revoke all on function public.manage_trip_member(text, text, text, boolean) from public;
revoke all on function public.manage_trip_member(text, text, text, boolean) from anon;
grant execute on function public.manage_trip_member(text, text, text, boolean) to authenticated;

-- Called only by the delete-account Edge Function with its service-role
-- client. All relational cleanup runs in one transaction and is safe to retry.
create or replace function public.delete_account_relational_data(p_user_id uuid)
returns void
language plpgsql
security invoker
set search_path = ''
as $$
declare
    v_owned_trip_ids text[];
begin
    select coalesce(array_agg(trip.id order by trip.id), array[]::text[])
    into v_owned_trip_ids
    from public.trips trip
    where trip.owner_id = p_user_id;

    delete from public.trip_collaborators collaborator
    where collaborator.user_id = p_user_id
       or collaborator.trip_id = any(v_owned_trip_ids);

    delete from public.trips trip
    where trip.owner_id = p_user_id;

    delete from public.user_data data
    where data.user_id = p_user_id;
end;
$$;

revoke all on function public.delete_account_relational_data(uuid) from public;
revoke all on function public.delete_account_relational_data(uuid) from anon;
revoke all on function public.delete_account_relational_data(uuid) from authenticated;
grant execute on function public.delete_account_relational_data(uuid) to service_role;
