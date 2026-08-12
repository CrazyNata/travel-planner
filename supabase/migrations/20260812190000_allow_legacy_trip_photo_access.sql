-- Keep legacy trip photos private while allowing their authorized trip members
-- to create signed URLs. Older web releases stored photos as
-- <owner-id>/accommodations/<file> and <owner-id>/sight-days/<file>, while the
-- current Android release stores them below <owner-id>/<trip-id>/.
drop policy if exists "users can read accessible trip photos" on storage.objects;

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
                      )
                  )
            )
        )
    )
);

