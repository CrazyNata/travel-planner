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
  if (!Number.isFinite(parsed)) return 12;
  return Math.min(Math.max(Math.trunc(parsed), 1), 20);
}

function textValue(value: unknown): string {
  if (typeof value === "string") return value.trim();
  if (value && typeof value === "object" && "text" in value) {
    return String((value as { text?: unknown }).text ?? "").trim();
  }
  return "";
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

async function resolvePhoto(
  photo: Record<string, unknown> | undefined,
  apiKey: string,
): Promise<{ photoUrl: string | null; photoAttribution: string | null }> {
  const photoName = typeof photo?.name === "string" ? photo.name : "";
  const attribution = Array.isArray(photo?.authorAttributions)
    ? photo.authorAttributions[0]
    : null;
  const photoAttribution = attribution && typeof attribution === "object"
    ? textValue((attribution as Record<string, unknown>).displayName)
    : null;
  if (!photoName) return { photoUrl: null, photoAttribution };

  // Photo resource names are intentionally resolved on demand and never stored.
  const mediaUrl = `https://places.googleapis.com/v1/${photoName}/media?maxWidthPx=640&maxHeightPx=480&skipHttpRedirect=true&key=${encodeURIComponent(apiKey)}`;
  const mediaResponse = await fetch(mediaUrl);
  if (!mediaResponse.ok) return { photoUrl: null, photoAttribution };
  const media = await mediaResponse.json().catch(() => null) as { photoUri?: unknown } | null;
  return {
    photoUrl: typeof media?.photoUri === "string" ? media.photoUri : null,
    photoAttribution,
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
  const city = String(body?.city ?? "").trim().slice(0, 100);
  const query = String(body?.query ?? "").trim().slice(0, 80);
  const languageCode = safeLanguageCode(body?.languageCode);
  const limit = clampLimit(body?.limit);
  if (!city) return jsonResponse({ error: "city is required" }, 400);

  const textQuery = query
    ? `${query} restaurant in ${city}`
    : `restaurants in ${city}`;
  const placesResponse = await fetch("https://places.googleapis.com/v1/places:searchText", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Goog-Api-Key": apiKey,
      "X-Goog-FieldMask": [
        "places.id",
        "places.displayName",
        "places.formattedAddress",
        "places.location",
        "places.rating",
        "places.userRatingCount",
        "places.priceLevel",
        "places.googleMapsUri",
        "places.photos",
        "places.types",
      ].join(","),
    },
    body: JSON.stringify({
      textQuery,
      languageCode,
      pageSize: limit,
    }),
  });

  if (!placesResponse.ok) {
    const details = await placesResponse.text().catch(() => "");
    console.error("Google Places request failed", placesResponse.status, details);
    return jsonResponse({ error: "Google Places request failed" }, 502);
  }

  const data = await placesResponse.json().catch(() => null) as { places?: unknown } | null;
  const places = Array.isArray(data?.places) ? data.places : [];
  const restaurants = await Promise.all(places.slice(0, limit).map(async (rawPlace, index) => {
    const place = rawPlace as Record<string, unknown>;
    const displayName = textValue(place.displayName);
    const location = place.location && typeof place.location === "object"
      ? place.location as Record<string, unknown>
      : {};
    const types = Array.isArray(place.types)
      ? place.types.filter((type): type is string => typeof type === "string")
          .filter((type) => !["establishment", "point_of_interest", "food"].includes(type))
          .slice(0, 2)
          .map((type) => type.replaceAll("_", " "))
          .join(" · ")
      : "";
    const firstPhoto = Array.isArray(place.photos) && place.photos[0] && typeof place.photos[0] === "object"
      ? place.photos[0] as Record<string, unknown>
      : undefined;
    const photo = index < 8
      ? await resolvePhoto(firstPhoto, apiKey)
      : { photoUrl: null, photoAttribution: null };
    return {
      place_id: textValue(place.id),
      name: displayName,
      address: textValue(place.formattedAddress),
      cuisine: types,
      rating: numberValue(place.rating),
      rating_count: typeof place.userRatingCount === "number" ? Math.trunc(place.userRatingCount) : null,
      price_level: priceLevelValue(place.priceLevel),
      photo_url: photo.photoUrl,
      photo_attribution: photo.photoAttribution,
      google_maps_url: textValue(place.googleMapsUri),
      latitude: numberValue(location.latitude),
      longitude: numberValue(location.longitude),
    };
  }));

  return jsonResponse({ restaurants });
});
