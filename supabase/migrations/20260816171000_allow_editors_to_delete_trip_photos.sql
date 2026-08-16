-- Editors may remove photos uploaded by the owner as well as their own
-- uploads. The old policy restricted editors to paths beginning with their
-- own user id, which made replacing/removing shared photos fail.
drop policy if exists "users can delete own trip photos" on storage.objects;
drop policy if exists "users can delete accessible trip photos" on storage.objects;

create policy "users can delete accessible trip photos"
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
                  or exists (
                      select 1
                      from public.trip_collaborators collaborator
                      where collaborator.trip_id = trip.id
                        and collaborator.user_id = (select auth.uid())
                        and collaborator.role = 'Редактор'
                  )
              )
        )
        or (
            (storage.foldername(name))[2] in ('accommodations', 'sight-days')
            and exists (
                select 1
                from public.trips trip
                where trip.owner_id::text = (storage.foldername(name))[1]
                  and position(name in trip.payload::text) > 0
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
);
