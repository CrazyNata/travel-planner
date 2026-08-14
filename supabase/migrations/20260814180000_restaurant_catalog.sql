-- Read-only restaurant catalog used by the native Android app.
-- User-selected restaurants continue to be stored in trips.payload.restaurants.

create table if not exists public.restaurant_catalog (
    id text primary key,
    city_key text not null,
    city_name_ru text not null,
    city_name_en text not null,
    city_name_es text not null default '',
    city_name_de text not null default '',
    name_ru text not null,
    name_en text not null,
    name_es text not null default '',
    name_de text not null default '',
    cuisine text not null default '',
    address text not null default '',
    website text not null default '',
    phone text not null default '',
    latitude double precision,
    longitude double precision,
    map_url text not null default '',
    search_text text not null default '',
    sort_order integer not null default 0,
    created_at timestamptz not null default now()
);

alter table public.restaurant_catalog enable row level security;

revoke all on table public.restaurant_catalog from public, anon, authenticated;
grant select on table public.restaurant_catalog to authenticated;

drop policy if exists restaurant_catalog_read_authenticated on public.restaurant_catalog;
create policy restaurant_catalog_read_authenticated
    on public.restaurant_catalog
    for select
    to authenticated
    using (true);

create index if not exists restaurant_catalog_city_key_idx on public.restaurant_catalog (city_key);
create index if not exists restaurant_catalog_city_ru_idx on public.restaurant_catalog (city_name_ru);
create index if not exists restaurant_catalog_city_en_idx on public.restaurant_catalog (city_name_en);
create index if not exists restaurant_catalog_city_es_idx on public.restaurant_catalog (city_name_es);
create index if not exists restaurant_catalog_city_de_idx on public.restaurant_catalog (city_name_de);
create index if not exists restaurant_catalog_search_text_idx on public.restaurant_catalog (search_text);
