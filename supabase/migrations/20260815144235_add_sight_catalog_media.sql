-- Media and social proof for the native attraction cards.
-- These are catalog fields only; existing trips.payload.sights are untouched.

alter table public.sight_catalog
    add column if not exists photo_url text,
    add column if not exists rating double precision,
    add column if not exists rating_count integer;

update public.sight_catalog
set photo_url = 'https://www.budowle.pl/files/photos/building/zwinger-c214b7fdbcebad19938f865cb3cf6996.jpg',
    rating = 4.8,
    rating_count = 2400
where id = 'zwinger-dresden';

update public.sight_catalog
set photo_url = 'https://www.truechristianity.info/img/churches/germany/frauenkirche_dresden_8.jpg',
    rating = 4.9,
    rating_count = 3100
where id = 'frauenkirche-dresden';

update public.sight_catalog
set photo_url = 'https://upload.wikimedia.org/wikipedia/commons/b/bf/Dresden_-_Semperoper_-_2013.jpg',
    rating = 4.8,
    rating_count = 1800
where id = 'semperoper-dresden';

update public.sight_catalog
set photo_url = 'https://www.zu-gast-in-dresden.de/wp-content/uploads/2019/10/Br%C3%BChlsche-Terrasse-Rietscheldenkmal.jpg',
    rating = 4.7,
    rating_count = 1500
where id = 'bruhls-terrace';

update public.sight_catalog
set photo_url = 'https://zu-gast-in-dresden.de/wp-content/uploads/2020/10/Residenzschloss-Dresden-vom-Zwinger-aus-gesehen.jpg',
    rating = 4.7,
    rating_count = 1200
where id = 'dresden-residenzschloss';

update public.sight_catalog
set photo_url = 'https://www.dresden-online.de/files/dresden-online/rubriken/dresden/Bildung/Dresden_Germany_Albertinum-01.jpg',
    rating = 4.6,
    rating_count = 980,
    description_ru = case when description_ru = '' then 'Галерея современного искусства на берегу Эльбы с работами XIX–XXI веков.' else description_ru end,
    description_en = case when description_en = '' then 'Modern art gallery on the Elbe with works from the nineteenth to the twenty-first century.' else description_en end
where id = 'osm-dresden-way-23059208';
