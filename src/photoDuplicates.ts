export type PhotoIdentity = {
  image: string;
  contentHash?: string;
  sourceHash?: string;
};

// Older versions used random file names. Newer names already contain the SHA-256.
export function photoContentHash(photo: PhotoIdentity): string | undefined {
  if (/^[a-f0-9]{64}$/i.test(photo.contentHash || "")) return photo.contentHash!.toLowerCase();
  try {
    const name = decodeURIComponent(new URL(photo.image).pathname).split("/").pop() || "";
    return name.match(/^([a-f0-9]{64})\.[a-z0-9]+$/i)?.[1].toLowerCase();
  } catch { return undefined; }
}

export function findDuplicatePhoto(
  photos: PhotoIdentity[], sourceHash: string, preparedHash: string,
): number {
  const incoming = new Set([sourceHash, preparedHash].map(hash => hash.toLowerCase()));
  return photos.findIndex(photo => [photoContentHash(photo), photo.sourceHash?.toLowerCase()]
    .some(hash => hash !== undefined && incoming.has(hash)));
}
