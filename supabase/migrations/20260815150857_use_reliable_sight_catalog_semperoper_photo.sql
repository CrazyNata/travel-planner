-- Use the Openverse-indexed Flickr image that loads reliably in the Android client.

update public.sight_catalog
set photo_url = 'https://live.staticflickr.com/4611/25797774248_67bb5c3f7e_b.jpg'
where id = 'semperoper-dresden';
