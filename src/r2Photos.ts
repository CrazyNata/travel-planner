import { supabase } from './supabase';

const endpoint = String(import.meta.env.VITE_PHOTO_API_URL || '').replace(/\/$/, '');
export const r2PhotosEnabled = Boolean(endpoint);
export type R2Photo = { original?: string; thumbnail?: string };

async function photoRequest(url: string, options: RequestInit, signal?: AbortSignal, timeout=15000) {
  const controller=new AbortController();
  const abort=()=>controller.abort();
  signal?.addEventListener('abort',abort,{once:true});
  if (signal?.aborted) controller.abort();
  const timer=window.setTimeout(abort,timeout);
  try {return await fetch(url,{...options,signal:controller.signal});}
  finally {window.clearTimeout(timer);signal?.removeEventListener('abort',abort);}
}

export function albumStorageKey(image: string): string | null {
  try {
    const path = decodeURIComponent(new URL(image).pathname).match(/\/storage\/v1\/object\/(?:sign|public)\/trip-photos\/(.+)$/)?.[1];
    return path && /^[a-f0-9-]{36}\/[a-f0-9-]{36}\/album\/[a-z0-9_][a-z0-9_.-]{0,180}\.(jpg|jpeg|png|webp)$/i.test(path) ? path : null;
  } catch { return null; }
}
async function authorization() {
  const {data:{session}} = await supabase.auth.getSession();
  if (!session) throw new Error('No active session');
  return {authorization:`Bearer ${session.access_token}`};
}
export async function resolveR2Photos(
  keys: string[],
  signal?: AbortSignal,
  onBatch?: (photos: Record<string,R2Photo>) => void,
): Promise<Record<string,R2Photo>> {
  if (!endpoint || !keys.length) return {};
  const headers = {...await authorization(), 'content-type':'application/json'};
  const result: Record<string,R2Photo> = {};
  const uniqueKeys = [...new Set(keys)];
  let nextBatch = 0;
  const failures: unknown[] = [];
  await Promise.all(Array.from({length:Math.min(3,Math.ceil(uniqueKeys.length/12))},async () => {
    while (nextBatch < uniqueKeys.length) {
      const start = nextBatch;
      nextBatch += 12;
      const batch = uniqueKeys.slice(start,start+12);
      try {
        const response = await photoRequest(`${endpoint}/resolve`, {method:'POST',headers,body:JSON.stringify({keys:batch})},signal);
        if (!response.ok) throw new Error(`Photo lookup failed: ${response.status}`);
        const photos: Record<string,R2Photo> = await response.json();
        Object.assign(result,photos);
        onBatch?.(photos);
      } catch (error) {
        if (signal?.aborted) throw error;
        failures.push(error);
        // Let this batch use its Supabase fallback without blocking other batches.
        onBatch?.(Object.fromEntries(batch.map(key=>[key,{}])));
      }
    }
  }));
  if (failures.length) throw failures[0];
  return result;
}
async function thumbnail(blob: Blob): Promise<Blob> {
  const objectUrl=URL.createObjectURL(blob);
  try {
    const image=new Image();
    await new Promise<void>((resolve,reject)=>{image.onload=()=>resolve();image.onerror=()=>reject(new Error('Cannot decode preview'));image.src=objectUrl;});
    const scale=Math.min(1,720/Math.max(image.naturalWidth,image.naturalHeight));
    const canvas=document.createElement('canvas');
    canvas.width=Math.max(1,Math.round(image.naturalWidth*scale));
    canvas.height=Math.max(1,Math.round(image.naturalHeight*scale));
    const context=canvas.getContext('2d');
    if (!context) throw new Error('Cannot create preview');
    context.drawImage(image,0,0,canvas.width,canvas.height);
    const preview=await new Promise<Blob|null>(resolve=>canvas.toBlob(resolve,'image/jpeg',0.72));
    if (!preview || preview.size>512*1024) throw new Error('Invalid preview size');
    return preview;
  } finally {URL.revokeObjectURL(objectUrl);}
}
export async function mirrorR2Photo(key: string, original: Blob, signal?: AbortSignal): Promise<R2Photo> {
  if (!endpoint) return {};
  const preview=await thumbnail(original);
  const headers=await authorization();
  for (const [variant,blob] of [['thumbnail',preview],['original',original]] as const) {
    const response=await photoRequest(`${endpoint}/object/${variant}/${key}`,{method:'PUT',headers:{...headers,'content-type':blob.type},body:blob},signal,120000);
    if (!response.ok) throw new Error(`Photo copy failed: ${response.status}`);
  }
  return (await resolveR2Photos([key],signal))[key] || {};
}
