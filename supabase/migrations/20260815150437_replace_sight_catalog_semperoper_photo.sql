-- Use a small Wikimedia Commons image that loads reliably on Android.

update public.sight_catalog
set photo_url = 'https://upload.wikimedia.org/wikipedia/commons/3/3e/Semperoper_Dresden.jpg'
where id = 'semperoper-dresden';
