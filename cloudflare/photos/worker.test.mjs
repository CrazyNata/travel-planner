import test from 'node:test';
import assert from 'node:assert/strict';
import worker from './worker.mjs';

test('private photo access, role enforcement, signed links and size limits', async () => {
  const user='11111111-1111-1111-1111-111111111111';
  const trip='22222222-2222-2222-2222-222222222222';
  const key=`${user}/${trip}/album/${'a'.repeat(64)}.jpg`;
  const objects=new Map();
  const env={SUPABASE_URL:'https://example.supabase.co',SUPABASE_KEY:'test',SIGNING_SECRET:'test-only-signing-secret',PHOTOS:{
    head:async key=>objects.get(key)||null,
    put:async (key,body,options)=>objects.set(key,{size:body.byteLength,body,httpMetadata:options.httpMetadata}),
    get:async key=>objects.get(key)||null
  }};
  const nativeFetch=globalThis.fetch;
  let role='owner';
  globalThis.fetch=async url=>Response.json(url.endsWith('/auth/v1/user') ? {id:user} : url.includes('/trips?') ? role==='outsider' ? [] : [{id:trip,owner_id:role==='owner'?user:'other'}] : [{role:role==='editor'?'Редактор':'Читатель'}]);
  const call=(path,init={})=>worker.fetch(new Request(`https://photos.example${path}`,init),env);
  const auth={authorization:'Bearer test'};
  try {
    assert.equal((await call('/resolve',{method:'POST',body:'{}'})).status,401);
    assert.equal((await call('/health',{headers:{origin:'https://evil.example'}})).status,403);
    assert.equal((await call(`/object/original/${key}`)).status,403);
    const put=()=>call(`/object/original/${key}`,{method:'PUT',headers:{...auth,'content-type':'image/jpeg','content-length':'3'},body:new Uint8Array([1,2,3])});
    role='reader'; assert.equal((await put()).status,403); assert.equal(objects.size,0);
    role='outsider'; assert.equal((await put()).status,403);
    role='editor'; assert.equal((await put()).status,204);
    assert.equal((await call(`/object/thumbnail/${key}`,{method:'PUT',headers:{...auth,'content-type':'image/jpeg','content-length':'999999'}})).status,413);
    const resolved=await call('/resolve',{method:'POST',headers:auth,body:JSON.stringify({keys:[key]})});
    assert.equal(resolved.status,200);
    const signed=(await resolved.json())[key].original;
    const signedPath=new URL(signed).pathname+new URL(signed).search;
    assert.equal((await call(signedPath)).status,200);
    assert.equal((await call(signedPath.replace('signature=','signature=0'))).status,403);
    const expired=new URL(signed); expired.searchParams.set('expires','1');
    assert.equal((await call(expired.pathname+expired.search)).status,403);
    role='outsider'; assert.equal((await call('/resolve',{method:'POST',headers:auth,body:JSON.stringify({keys:[key]})})).status,403);
  } finally {globalThis.fetch=nativeFetch;}
});
