-- Keep the Wikimedia thumbnail URL free of tracking parameters for Coil/Android compatibility.

update public.sight_catalog
set photo_url = 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Dresden_-_Semperoper_-_2013.jpg/960px-Dresden_-_Semperoper_-_2013.jpg'
where id = 'semperoper-dresden';
