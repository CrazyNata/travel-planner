# Ramingo album media

Private Cloudflare R2 bucket `ramingo-photos` in the EU jurisdiction, served by
`https://ramingo-photos.road-guide-natasha7261.workers.dev`.

The Worker checks Supabase Auth and the caller's existing trip RLS permissions.
Reads use HMAC links valid for one hour; uploads require an owner or editor and
an object prefix belonging to the authenticated uploader. There is no public
bucket access or delete/list endpoint. Automatic R2 cleanup during account
deletion requires separate user approval; it has not been enabled. Secrets live in Wrangler secret storage:
`SUPABASE_KEY` (publishable key) and `SIGNING_SECRET` (random signing key).

The web album resolves private R2 copies in batches of 12, with up to three
batches in flight. Recent dated photos are requested first, and each completed
batch renders immediately without waiting for the rest of the album. The Worker
checks up to six R2 objects concurrently and reuses one signing key per batch.
The album displays 720px JPEG thumbnails and loads originals only in the expanded viewer. Missing copies are
prepared sequentially while the uploader has the album open. New web uploads
also prepare both R2 variants. Existing trip payloads and Supabase originals are
preserved, including EXIF-derived metadata. Supabase copies remain necessary for
installed mobile releases and public-trip links during this compatibility phase.
R2 failure falls back to the existing images; failed copies retry next visit.

Set `VITE_PHOTO_API_URL` in the web build to enable R2. Removing it restores the
previous delivery path without deleting any objects. Do not remove Supabase
originals until all clients support the new private photo service.

Deploy with `pnpm dlx wrangler@4 deploy --config cloudflare/photos/wrangler.jsonc`.
Run security checks with `node --test cloudflare/photos/worker.test.mjs`.
