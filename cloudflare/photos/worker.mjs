const origins = new Set(['https://ramingo.online', 'https://travelplanner.muntim.ru', 'http://localhost:5173', 'http://127.0.0.1:5173']);
const encoder = new TextEncoder();
const keyPattern = /^([a-f0-9-]{36})\/([a-f0-9-]{36})\/album\/([a-z0-9_][a-z0-9_.-]{0,180})\.(jpg|jpeg|png|webp)$/i;
const maxBytes = 10 * 1024 * 1024;

async function signature(env, key, expires) {
  const secret = await crypto.subtle.importKey('raw', encoder.encode(env.SIGNING_SECRET), {name:'HMAC', hash:'SHA-256'}, false, ['sign']);
  const bytes = await crypto.subtle.sign('HMAC', secret, encoder.encode(`${key}\n${expires}`));
  return Array.from(new Uint8Array(bytes), b => b.toString(16).padStart(2,'0')).join('');
}
function equal(a, b) {
  if (a.length !== b.length) return false;
  let difference = 0;
  for (let i=0; i<a.length; i++) difference |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return difference === 0;
}
async function userContext(request, env) {
  const authorization = request.headers.get('authorization');
  if (!authorization?.startsWith('Bearer ')) throw new Response('Unauthorized', {status:401});
  const headers = {authorization, apikey:env.SUPABASE_KEY};
  const response = await fetch(`${env.SUPABASE_URL}/auth/v1/user`, {headers});
  if (!response.ok) throw new Response('Unauthorized', {status:401});
  return {user:await response.json(), headers};
}
async function access(env, context, tripId, write) {
  const response = await fetch(`${env.SUPABASE_URL}/rest/v1/trips?id=eq.${tripId}&select=id,owner_id`, {headers:context.headers});
  if (!response.ok) throw new Response('Forbidden', {status:403});
  const trip = (await response.json())[0];
  if (!trip) throw new Response('Forbidden', {status:403});
  if (!write || trip.owner_id === context.user.id) return trip;
  const membership = await fetch(`${env.SUPABASE_URL}/rest/v1/trip_collaborators?trip_id=eq.${tripId}&user_id=eq.${context.user.id}&select=role`, {headers:context.headers});
  if (!membership.ok || !(await membership.json()).some(row=>row.role === 'Редактор')) throw new Response('Forbidden', {status:403});
  return trip;
}
export default {
  async fetch(request, env) {
    const origin = request.headers.get('origin');
    const cors = {'Access-Control-Allow-Origin': origins.has(origin) ? origin : 'https://ramingo.online', 'Access-Control-Allow-Headers':'authorization,content-type', 'Access-Control-Allow-Methods':'POST,PUT,GET,HEAD,OPTIONS', 'Vary':'Origin', 'Cache-Control':'no-store'};
    const respond = (body, status=200, extra={}) => new Response(body, {status, headers:{...cors,...extra}});
    try {
      if (origin && !origins.has(origin)) return respond('Forbidden',403);
      if (request.method === 'OPTIONS') return respond(null,204);
      const url = new URL(request.url);
      if (url.pathname === '/health') return respond(JSON.stringify({service:'ramingo-photos',ready:Boolean(env.PHOTOS && env.SIGNING_SECRET && env.SUPABASE_KEY)}),200,{'Content-Type':'application/json'});
      if (url.pathname === '/resolve' && request.method === 'POST') {
        const context = await userContext(request,env);
        const {keys} = await request.json();
        if (!Array.isArray(keys) || keys.length > 12 || keys.some(key=>typeof key !== 'string' || !keyPattern.test(key))) return respond('Invalid keys',400);
        for (const tripId of new Set(keys.map(key=>keyPattern.exec(key)[2]))) await access(env,context,tripId,false);
        const expires = Math.floor(Date.now()/1000)+3600;
        const result = {};
        for (const key of new Set(keys)) {
          const entry = {};
          for (const variant of ['original','thumbnail']) {
            const objectKey = `${variant}/${key}`;
            if (await env.PHOTOS.head(objectKey)) entry[variant] = `${url.origin}/object/${objectKey}?expires=${expires}&signature=${await signature(env,objectKey,expires)}`;
          }
          result[key] = entry;
        }
        return respond(JSON.stringify(result),200,{'Content-Type':'application/json'});
      }
      const match = url.pathname.match(/^\/object\/(original|thumbnail)\/(.+)$/);
      if (!match || !keyPattern.test(match[2])) return respond('Not found',404);
      const [,variant,key] = match;
      const objectKey = `${variant}/${key}`;
      if (request.method === 'PUT') {
        const context = await userContext(request,env);
        const parts = keyPattern.exec(key);
        const trip = await access(env,context,parts[2],true);
        if (parts[1] !== context.user.id && trip.owner_id !== context.user.id) return respond('Forbidden',403);
        const type = request.headers.get('content-type')?.split(';')[0];
        if (!['image/jpeg','image/png','image/webp'].includes(type)) return respond('Invalid image type',415);
        const limit = variant === 'thumbnail' ? 512*1024 : maxBytes;
        const length = Number(request.headers.get('content-length'));
        if (!length || length > limit) return respond('Invalid image size',413);
        const body = await request.arrayBuffer();
        if (!body.byteLength || body.byteLength > limit) return respond('Image too large',413);
        // Copies are immutable; neither an original nor a generated preview is replaced.
        if (!(await env.PHOTOS.head(objectKey))) await env.PHOTOS.put(objectKey,body,{httpMetadata:{contentType:type}});
        return respond(null,204);
      }
      if (request.method !== 'GET' && request.method !== 'HEAD') return respond('Method not allowed',405);
      const expires = Number(url.searchParams.get('expires'));
      const now = Math.floor(Date.now()/1000);
      if (!Number.isInteger(expires) || expires < now || expires > now+3600 || !equal(await signature(env,objectKey,expires),url.searchParams.get('signature')||'')) return respond('Expired or invalid link',403);
      const object = request.method === 'HEAD' ? await env.PHOTOS.head(objectKey) : await env.PHOTOS.get(objectKey);
      if (!object) return respond('Not found',404);
      return respond(request.method === 'HEAD' ? null : object.body,200,{'Content-Type':object.httpMetadata?.contentType||'application/octet-stream','Content-Length':String(object.size),'X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'});
    } catch (error) {
      if (error instanceof Response) return respond(await error.text(),error.status);
      console.error('Photo request failed', error?.message);
      return respond('Photo service unavailable',503);
    }
  }
};
