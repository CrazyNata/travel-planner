-- Keep the Semperoper card image small enough for fast mobile loading.

update public.sight_catalog
set photo_url = 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/bf/Dresden_-_Semperoper_-_2013.jpg/960px-Dresden_-_Semperoper_-_2013.jpg?utm_source=commons.wikimedia.org&utm_campaign=imageinfo&utm_content=thumbnail'
where id = 'semperoper-dresden';
