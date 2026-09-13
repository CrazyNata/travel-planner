import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

type JsonRecord = Record<string, unknown>;

function jsonResponse(payload: JsonRecord, status = 200) {
  return new Response(JSON.stringify(payload), { status, headers: corsHeaders });
}

function textValue(value: unknown, maxLength: number): string {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

function numberValue(value: unknown): number | null {
  const parsed = typeof value === "number" ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function integerValue(value: unknown): number | null {
  const parsed = numberValue(value);
  return parsed == null ? null : Math.trunc(parsed);
}

function clampInteger(value: unknown, fallback: number, min: number, max: number): number {
  const parsed = integerValue(value);
  if (parsed == null) return fallback;
  return Math.min(Math.max(parsed, min), max);
}

function baseUrl(): string {
  const configured = Deno.env.get("TIQETS_BASE_URL")?.trim();
  const value = configured || "https://api.tiqets.com/v2";
  if (!/^https:\/\//i.test(value)) throw new Error("TIQETS_BASE_URL must use HTTPS");
  return value.replace(/\/+$/, "");
}

function languageCode(value: unknown): string {
  const requested = textValue(value, 12).toLowerCase().split("-")[0];
  // The Android client currently sends ru, en, es or de. Keeping a small
  // allow-list prevents an arbitrary value from producing a provider error.
  return new Set(["ru", "en", "es", "de"]).has(requested) ? requested : "en";
}

function cityName(value: string): string {
  // Preserve hyphens inside names such as Figline-Valdarno; only remove the
  // country part that is commonly appended after a comma or em dash.
  return value.split(/[,—–]/)[0].trim().slice(0, 100) || value.trim().slice(0, 100);
}

function formatPrice(value: number | null): string {
  if (value == null) return "";
  return Number.isInteger(value)
    ? String(value)
    : value.toFixed(2).replace(/0+$/, "").replace(/\.$/, "");
}

function nestedRecord(value: unknown): JsonRecord | null {
  return value && typeof value === "object" && !Array.isArray(value)
    ? value as JsonRecord
    : null;
}

function providerError(status: number): Error {
  if (status === 401 || status === 403) return new Error("Ticket provider authentication failed");
  if (status === 429) return new Error("Ticket provider rate limit reached");
  return new Error("Ticket provider request failed");
}

async function requestJson(url: string, token: string): Promise<JsonRecord> {
  const response = await fetch(url, {
    headers: {
      Accept: "application/json",
      "User-Agent": "Odyssey Travel Planner",
      Authorization: `Token ${token}`,
    },
  });
  const payload = await response.json().catch(() => ({})) as JsonRecord;
  if (!response.ok) {
    console.error("Tiqets request failed", response.status, textValue(payload.error ?? payload.message, 240));
    throw providerError(response.status);
  }
  return payload;
}

function firstImage(product: JsonRecord): string {
  const images = Array.isArray(product.images) ? product.images : [];
  for (const item of images) {
    const image = nestedRecord(item);
    if (!image) continue;
    for (const key of ["extra_large", "large", "medium", "small"]) {
      const value = textValue(image[key], 1_000);
      if (/^https?:\/\//i.test(value)) return value;
    }
  }
  return "";
}

function mapProduct(item: unknown, fallbackCity: string): JsonRecord | null {
  const product = nestedRecord(item);
  if (!product) return null;

  const title = textValue(product.title, 180);
  if (!title) return null;

  const venue = nestedRecord(product.venue);
  const geolocation = nestedRecord(product.geolocation);
  const ratings = nestedRecord(product.ratings);
  const description = textValue(product.summary, 700) || textValue(product.tagline, 700);
  const bookingUrl = textValue(product.product_checkout_url, 1_000) || textValue(product.product_url, 1_000);
  const rating = numberValue(ratings?.average ?? product.rating);
  const ratingCount = integerValue(ratings?.total ?? product.ratingCount);

  return {
    id: textValue(product.id, 120) || `${fallbackCity}:${title}`,
    title,
    city: textValue(product.city_name, 100) || fallbackCity,
    description,
    priceAmount: formatPrice(numberValue(product.price)),
    currencyCode: textValue(product.currency, 8),
    rating,
    ratingCount,
    imageUrl: firstImage(product),
    bookingUrl,
    provider: "Tiqets",
    _latitude: numberValue(geolocation?.lat),
    _longitude: numberValue(geolocation?.lng),
    _venue: textValue(venue?.name, 180),
  };
}

function removeInternalFields(item: JsonRecord): JsonRecord {
  const result = { ...item };
  delete result._latitude;
  delete result._longitude;
  delete result._venue;
  return result;
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  const token = Deno.env.get("TIQETS_API_TOKEN")?.trim();
  if (!token) return jsonResponse({ error: "Ticket search is not configured" }, 503);

  const body = await request.json().catch(() => null) as JsonRecord | null;
  const requestedCity = textValue(body?.city, 100);
  const query = textValue(body?.query, 80);
  if (!requestedCity) return jsonResponse({ error: "city is required" }, 400);

  const requestedRadius = clampInteger(body?.radiusKm, 10, 1, 100);
  const requestedLimit = clampInteger(body?.limit, 20, 1, 30);
  const language = languageCode(body?.languageCode);
  const latitude = numberValue(body?.latitude);
  const longitude = numberValue(body?.longitude);

  try {
    const url = new URL(`${baseUrl()}/products`);
    url.searchParams.set("lang", language);
    url.searchParams.set("currency", "EUR");
    url.searchParams.set("page", "1");
    url.searchParams.set("page_size", String(Math.min(requestedLimit, 100)));
    url.searchParams.set("require_venue", "true");
    url.searchParams.set("sort", query ? "score" : "popularity");
    if (query) url.searchParams.set("query", query);

    // Coordinates are preferred because the app already has localized city
    // labels and Tiqets' city_name filter is intended mainly for debugging.
    if (latitude != null && longitude != null) {
      url.searchParams.set("lat", String(latitude));
      url.searchParams.set("lng", String(longitude));
      url.searchParams.set("max_distance", String(requestedRadius));
    } else {
      url.searchParams.set("city_name", cityName(requestedCity));
    }

    const payload = await requestJson(url.toString(), token);
    const products = Array.isArray(payload.products) ? payload.products : [];
    const selected = products
      .map((item) => mapProduct(item, requestedCity))
      .filter((item): item is JsonRecord => item != null)
      .slice(0, requestedLimit)
      .map(removeInternalFields);

    return jsonResponse({ city: requestedCity, results: selected });
  } catch (error) {
    console.error("Ticket search failed", error);
    return jsonResponse({ error: error instanceof Error ? error.message : "Ticket search failed" }, 502);
  }
});
