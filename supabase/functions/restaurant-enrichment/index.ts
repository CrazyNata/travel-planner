import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function jsonResponse(payload: Record<string, unknown>, status = 200) {
  return new Response(JSON.stringify(payload), { status, headers: corsHeaders });
}

function safeLanguageCode(value: unknown): string {
  const language = String(value ?? "ru").trim().toLowerCase();
  return ["ru", "en", "es", "de"].includes(language) ? language : "ru";
}

function clampLimit(value: unknown): number {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return 60;
  return Math.min(Math.max(Math.trunc(parsed), 1), 60);
}

const PlacesPageSize = 20;
// Keep the first screen backwards-compatible for older Android builds. The
// remaining photos are resolved on demand by the current client.
const InitialPhotoLimit = 8;
const PhotoConcurrency = 8;
const PhotoWidth = 480;
const PhotoHeight = 360;
const PhotoTimeoutMs = 5_000;

function textValue(value: unknown): string {
  if (typeof value === "string") return value.trim();
  if (value && typeof value === "object" && "text" in value) {
    return String((value as { text?: unknown }).text ?? "").trim();
  }
  return "";
}

function editorialSummaryValue(value: unknown): string {
  if (!value || typeof value !== "object") return "";
  return textValue((value as Record<string, unknown>).text);
}

function numberValue(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function priceLevelValue(value: unknown): number | null {
  if (typeof value === "number" && Number.isFinite(value)) return Math.trunc(value);
  switch (String(value ?? "")) {
    case "PRICE_LEVEL_INEXPENSIVE": return 1;
    case "PRICE_LEVEL_MODERATE": return 2;
    case "PRICE_LEVEL_EXPENSIVE": return 3;
    case "PRICE_LEVEL_VERY_EXPENSIVE": return 4;
    default: return null;
  }
}

function photoAttributionValue(photo: Record<string, unknown> | undefined): string | null {
  const attribution = Array.isArray(photo?.authorAttributions)
    ? photo.authorAttributions[0]
    : null;
  return attribution && typeof attribution === "object"
    ? textValue((attribution as Record<string, unknown>).displayName)
    : null;
}

async function resolvePhoto(
  photo: Record<string, unknown> | undefined,
  apiKey: string,
): Promise<{ photoUrl: string | null; photoAttribution: string | null }> {
  const photoName = typeof photo?.name === "string" ? photo.name : "";
  const photoAttribution = photoAttributionValue(photo);
  if (!photoName) return { photoUrl: null, photoAttribution };

  // Photo resource names are intentionally resolved on demand and never stored.
  // Catalog cards are small, so keep the network payload below the full-size
  // Google photo while retaining enough resolution for the preview.
  const mediaUrl = `https://places.googleapis.com/v1/${photoName}/media?maxWidthPx=${PhotoWidth}&maxHeightPx=${PhotoHeight}&skipHttpRedirect=true&key=${encodeURIComponent(apiKey)}`;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), PhotoTimeoutMs);
  try {
    const mediaResponse = await fetch(mediaUrl, { signal: controller.signal });
    if (!mediaResponse.ok) return { photoUrl: null, photoAttribution };
    const media = await mediaResponse.json().catch(() => null) as { photoUri?: unknown } | null;
    return {
      photoUrl: typeof media?.photoUri === "string" ? media.photoUri : null,
      photoAttribution,
    };
  } catch {
    // A single unavailable photo must not discard the rest of the catalog.
    return { photoUrl: null, photoAttribution };
  } finally {
    clearTimeout(timeout);
  }
}

async function mapWithConcurrency<T, R>(
  items: T[],
  concurrency: number,
  mapper: (item: T, index: number) => Promise<R>,
): Promise<R[]> {
  const results = new Array<R>(items.length);
  let nextIndex = 0;
  const workerCount = Math.min(Math.max(concurrency, 1), items.length);

  const worker = async () => {
    while (true) {
      const index = nextIndex++;
      if (index >= items.length) return;
      results[index] = await mapper(items[index], index);
    }
  };

  await Promise.all(Array.from({ length: workerCount }, () => worker()));
  return results;
}

async function fetchPlacesPage(
  apiKey: string,
  textQuery: string,
  languageCode: string,
  pageSize: number,
  includedType: string,
  pageToken?: string,
  includeEditorialSummary = true,
): Promise<{ places?: unknown; nextPageToken?: unknown }> {
  const fields = [
    "places.id",
    "places.displayName",
    "places.formattedAddress",
    "places.location",
    "places.rating",
    "places.userRatingCount",
    "places.priceLevel",
    "places.googleMapsUri",
    "places.websiteUri",
    "places.nationalPhoneNumber",
    "places.internationalPhoneNumber",
    "places.photos",
    "places.types",
    "places.primaryType",
    "places.primaryTypeDisplayName",
    ...(includeEditorialSummary ? ["places.editorialSummary"] : []),
    "nextPageToken",
  ];
  const placesResponse = await fetch("https://places.googleapis.com/v1/places:searchText", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Goog-Api-Key": apiKey,
      "X-Goog-FieldMask": fields.join(","),
    },
    body: JSON.stringify({
      textQuery,
      languageCode,
      includedType,
      pageSize,
      ...(pageToken ? { pageToken } : {}),
    }),
  });

  if (!placesResponse.ok) {
    const details = await placesResponse.text().catch(() => "");
    console.error("Google Places request failed", placesResponse.status, details);
    throw new Error("Google Places request failed");
  }

  return await placesResponse.json().catch(() => ({})) as {
    places?: unknown;
    nextPageToken?: unknown;
  };
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  const apiKey = Deno.env.get("GOOGLE_PLACES_API_KEY")?.trim();
  if (!apiKey) {
    return jsonResponse({ error: "GOOGLE_PLACES_API_KEY is not configured" }, 503);
  }

  const body = await request.json().catch(() => null) as Record<string, unknown> | null;
  const requestedCategory = String(body?.category ?? "restaurant").trim().toLowerCase();
  const category = requestedCategory === "sight" || requestedCategory === "accommodation"
    ? requestedCategory
    : "restaurant";
  const responseKey = category === "sight"
    ? "sights"
    : category === "accommodation"
      ? "accommodations"
      : "restaurants";

  const requestedPhotoNames = Array.isArray(body?.photoNames)
    ? body.photoNames
      .filter((value): value is string => typeof value === "string")
      .map((value) => value.trim().slice(0, 1000))
      .filter((value) => value.startsWith("places/") && value.includes("/photos/"))
      .filter((value, index, values) => values.indexOf(value) === index)
      .slice(0, 24)
    : [];
  if (requestedPhotoNames.length > 0) {
    const photos = await mapWithConcurrency(
      requestedPhotoNames,
      PhotoConcurrency,
      async (photoName) => {
        const photo = await resolvePhoto({ name: photoName }, apiKey);
        return {
          photo_name: photoName,
          photo_url: photo.photoUrl,
          photo_attribution: photo.photoAttribution,
        };
      },
    );
    return jsonResponse({ photos });
  }

  const requestedPhotoName = String(body?.photoName ?? "").trim().slice(0, 1000);
  if (requestedPhotoName) {
    if (!requestedPhotoName.startsWith("places/") || !requestedPhotoName.includes("/photos/")) {
      return jsonResponse({ error: "invalid photoName" }, 400);
    }
    const photo = await resolvePhoto({ name: requestedPhotoName }, apiKey);
    return jsonResponse({
      photo_url: photo.photoUrl,
      photo_attribution: photo.photoAttribution,
    });
  }

  const city = String(body?.city ?? "").trim().slice(0, 100);
  const query = String(body?.query ?? "").trim().slice(0, 80);
  const languageCode = safeLanguageCode(body?.languageCode);
  // The original Android client sent limit=20. Keep the blank city catalog
  // full-size so older clients also receive the complete city dataset.
  const limit = query ? clampLimit(body?.limit) : 60;
  if (!city) return jsonResponse({ error: "city is required" }, 400);

  const textQuery = query
    ? category === "sight"
      ? `${query} tourist attraction in ${city}`
      : category === "accommodation"
        ? `${query} hotel lodging in ${city}`
        : `${query} restaurant in ${city}`
    : category === "sight"
      ? `top tourist attractions in ${city}`
      : category === "accommodation"
        ? `hotels, apartments, hostels, resorts, bed and breakfasts, guest houses and motels in ${city}`
        : `restaurants in ${city}`;
  // Text Search accepts one included type. `lodging` is the broad Google
  // lodging type and the text query keeps hotels, apartments and hostels in
  // the result set without making a separate paid request for every subtype.
  const includedType = category === "sight"
    ? "tourist_attraction"
    : category === "accommodation"
      ? "lodging"
      : "restaurant";
  const places: unknown[] = [];
  const pageSize = Math.min(PlacesPageSize, limit);
  const seenPageTokens = new Set<string>();
  let pageToken: string | undefined;
  let includeEditorialSummary = category === "sight";
  while (places.length < limit) {
    let page: { places?: unknown; nextPageToken?: unknown };
    try {
      page = await fetchPlacesPage(
        apiKey,
        textQuery,
        languageCode,
        pageSize,
        includedType,
        pageToken,
        includeEditorialSummary,
      );
    } catch (error) {
      // Keep live ratings/photos working if a Places project does not have the
      // Atmosphere field enabled yet. Descriptions are optional enrichment.
      if (!includeEditorialSummary) throw error;
      console.warn("Google Places editorial summaries unavailable; retrying without them");
      includeEditorialSummary = false;
      page = await fetchPlacesPage(
        apiKey,
        textQuery,
        languageCode,
        pageSize,
        includedType,
        pageToken,
        false,
      );
    }
    const pagePlaces = Array.isArray(page.places) ? page.places : [];
    places.push(...pagePlaces);
    const nextPageToken = typeof page.nextPageToken === "string"
      ? page.nextPageToken.trim()
      : "";
    if (!nextPageToken || pagePlaces.length === 0 || seenPageTokens.has(nextPageToken)) break;
    seenPageTokens.add(nextPageToken);
    pageToken = nextPageToken;
  }

  // Resolve only the first visible batch here. Resolving all 60 photos before
  // returning the catalog made the first screen wait for ten photo batches.
  // Current clients resolve the rest from photoName as cards enter the list.
  const initialPhotos = await mapWithConcurrency(
    places.slice(0, Math.min(limit, InitialPhotoLimit)),
    PhotoConcurrency,
    async (rawPlace) => {
      const place = rawPlace as Record<string, unknown>;
      const firstPhoto = Array.isArray(place.photos) && place.photos[0] && typeof place.photos[0] === "object"
        ? place.photos[0] as Record<string, unknown>
        : undefined;
      return resolvePhoto(firstPhoto, apiKey);
    },
  );

  const results = places.slice(0, limit).map((rawPlace, index) => {
    const place = rawPlace as Record<string, unknown>;
    const displayName = textValue(place.displayName);
    const location = place.location && typeof place.location === "object"
      ? place.location as Record<string, unknown>
      : {};
    const types = Array.isArray(place.types)
      ? place.types.filter((type): type is string => typeof type === "string")
          .filter((type) => !["establishment", "point_of_interest", "food", "lodging"].includes(type))
          .slice(0, 2)
          .map((type) => type.replaceAll("_", " "))
          .join(" · ")
      : "";
    const primaryTypeDisplayName = textValue(place.primaryTypeDisplayName);
    const typeLabel = category === "accommodation"
      ? primaryTypeDisplayName || types || "Жильё"
      : types;
    const firstPhoto = Array.isArray(place.photos) && place.photos[0] && typeof place.photos[0] === "object"
      ? place.photos[0] as Record<string, unknown>
      : undefined;
    const photoName = typeof firstPhoto?.name === "string" ? firstPhoto.name : "";
    const photoNames = Array.isArray(place.photos)
      ? place.photos
        .filter((photo): photo is Record<string, unknown> => Boolean(photo) && typeof photo === "object")
        .map((photo) => typeof photo.name === "string" ? photo.name.trim() : "")
        .filter((name) => name.startsWith("places/") && name.includes("/photos/"))
        .filter((name, photoIndex, names) => names.indexOf(name) === photoIndex)
        .slice(0, 5)
      : [];
    const photo = initialPhotos[index];
    return {
      place_id: textValue(place.id),
      name: displayName,
      address: textValue(place.formattedAddress),
      cuisine: types,
      category: types,
      rating: numberValue(place.rating),
      rating_count: typeof place.userRatingCount === "number" ? Math.trunc(place.userRatingCount) : null,
      price_level: priceLevelValue(place.priceLevel),
      description: editorialSummaryValue(place.editorialSummary),
      photo_url: photo?.photoUrl ?? null,
      photo_name: photoName,
      photo_names: photoNames,
      photo_attribution: photo?.photoAttribution ?? photoAttributionValue(firstPhoto),
      google_maps_url: textValue(place.googleMapsUri),
      website: textValue(place.websiteUri),
      phone: textValue(place.internationalPhoneNumber) || textValue(place.nationalPhoneNumber),
      type: typeLabel,
      latitude: numberValue(location.latitude),
      longitude: numberValue(location.longitude),
    };
  });

  return jsonResponse({ [responseKey]: results });
});
