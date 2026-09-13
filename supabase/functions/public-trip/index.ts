import { createClient } from "npm:@supabase/supabase-js@2";

type JsonObject = Record<string, unknown>;

const isJsonObject = (value: unknown): value is JsonObject =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const stringValue = (value: unknown) =>
  typeof value === "string" ? value.trim() : "";

const firstString = (...values: unknown[]) => {
  for (const value of values) {
    const text = stringValue(value);
    if (text) return text;
  }
  return "";
};

const firstArray = (...values: unknown[]) =>
  values.find((value): value is unknown[] => Array.isArray(value)) || [];

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Cache-Control": "no-store",
  "Content-Type": "application/json; charset=utf-8",
};

function jsonResponse(body: JsonObject, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function notFound() {
  return jsonResponse({ error: "Публичное путешествие не найдено." }, 404);
}

function photoPath(value: string) {
  const storageUrl = value.match(/^storage:\/\/([^/]+)\/(.+)$/);
  if (storageUrl?.[1] === "trip-photos") {
    try {
      return decodeURIComponent(storageUrl[2]);
    } catch {
      return storageUrl[2];
    }
  }
  try {
    const pathname = new URL(value).pathname;
    const match = pathname.match(
      /\/storage\/v1\/object\/(?:public|sign)\/trip-photos\/(.+)$/,
    );
    return match ? decodeURIComponent(match[1]) : null;
  } catch {
    return null;
  }
}

function mapPhotoUrls<T>(value: T, mapUrl: (url: string, path: string) => string): T {
  if (typeof value === "string") {
    const path = photoPath(value);
    return (path ? mapUrl(value, path) : value) as T;
  }
  if (Array.isArray(value)) {
    return value.map((item) => mapPhotoUrls(item, mapUrl)) as T;
  }
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, mapPhotoUrls(item, mapUrl)]),
    ) as T;
  }
  return value;
}

async function signPublicPhotos(
  admin: ReturnType<typeof createClient>,
  trip: JsonObject,
) {
  const paths = new Set<string>();
  mapPhotoUrls(trip, (_url, path) => {
    paths.add(path);
    return _url;
  });
  if (!paths.size) return trip;

  const { data, error } = await admin.storage
    .from("trip-photos")
    .createSignedUrls([...paths], 60 * 60);
  if (error) {
    console.error("public-trip: could not sign trip photos", error);
    return trip;
  }
  const signedUrls = new Map(
    (data || []).flatMap((item) =>
      item.signedUrl ? [[item.path, item.signedUrl] as const] : [],
    ),
  );
  return mapPhotoUrls(trip, (url, path) => signedUrls.get(path) || url);
}

function publicCoverPhotos(value: unknown) {
  return firstArray(value)
    .filter(isJsonObject)
    .map((photo, index) => {
      const image = stringValue(photo.image);
      if (!image) return null;
      return {
        id: firstString(photo.id) || `public-cover-${index + 1}`,
        image,
        ...(stringValue(photo.city) ? { city: stringValue(photo.city) } : {}),
        ...(stringValue(photo.date) ? { date: stringValue(photo.date) } : {}),
        ...(stringValue(photo.textColor)
          ? { textColor: stringValue(photo.textColor) }
          : {}),
      };
    })
    .filter((photo): photo is Record<string, string> => photo !== null);
}

function publicDays(payload: JsonObject, nestedData: JsonObject) {
  const rawDays = firstArray(payload.days, nestedData.days);
  return rawDays
    .filter(isJsonObject)
    .map((day, index) => {
      const roadLeg = isJsonObject(day.roadLeg) ? day.roadLeg : {};
      const rawItems = firstArray(day.places, day.items);
      const places = rawItems
        .map((item) => (isJsonObject(item) ? item.title : item))
        .map(stringValue)
        .filter(Boolean);
      return {
        id: firstString(day.id) || `public-day-${index + 1}`,
        city: firstString(day.city, roadLeg.to) || "Маршрут",
        date: firstString(day.date, roadLeg.date),
        places,
      };
    })
    .filter((day) => day.date || day.places.length || day.city !== "Маршрут");
}

function publicAccommodations(
  payload: JsonObject,
  nestedData: JsonObject,
  nestedTrip: JsonObject,
) {
  const rawAccommodations = firstArray(
    payload.accommodations,
    nestedTrip.accommodations,
    nestedData.accommodations,
  );
  return rawAccommodations
    .filter(isJsonObject)
    .map((stay, index) => ({
      id: firstString(stay.id) || `public-accommodation-${index + 1}`,
      name: firstString(stay.name) || "Жильё",
      city: firstString(stay.city),
      dates: firstString(stay.dates),
      paymentDeadline: firstString(stay.paymentDeadline),
      status: firstString(stay.status) || "бронь",
      price: firstString(stay.price),
      ...(firstString(stay.bookingUrl)
        ? { bookingUrl: firstString(stay.bookingUrl) }
        : {}),
    }));
}

function publicTripFromPayload(id: string, payload: JsonObject) {
  const nestedData = isJsonObject(payload.data) ? payload.data : {};
  const nestedTrip = isJsonObject(nestedData.trip) ? nestedData.trip : {};
  const title = firstString(payload.title, nestedTrip.title);
  if (!title) return null;

  const days = publicDays(payload, nestedData);
  const topLevelPlaces = firstArray(payload.places)
    .map(stringValue)
    .filter(Boolean);
  if (!days.length && topLevelPlaces.length) {
    days.push({
      id: "public-day-1",
      city: firstString(payload.cities, nestedTrip.cities) || "Маршрут",
      date: "",
      places: topLevelPlaces,
    });
  }

  const coverPhotos = publicCoverPhotos(
    firstArray(payload.coverPhotos, payload.photos, nestedTrip.coverPhotos, nestedTrip.photos),
  );
  const coverImage = firstString(
    payload.coverImage,
    nestedTrip.coverImage,
    coverPhotos[0]?.image,
  );
  const cities = firstString(payload.cities, nestedTrip.cities) ||
    [...new Set(days.map((day) => day.city).filter((city) => city !== "Маршрут"))].join(" · ");
  const accommodations = publicAccommodations(payload, nestedData, nestedTrip);

  return {
    id,
    title,
    dates: firstString(payload.dates, nestedTrip.dates),
    startDate: firstString(payload.startDate, nestedTrip.startDate, nestedTrip.start),
    endDate: firstString(payload.endDate, nestedTrip.endDate, nestedTrip.end),
    cities,
    status: firstString(payload.status, nestedTrip.status),
    coverImage,
    coverPhotos,
    photos: coverPhotos,
    days,
    accommodations,
  } satisfies JsonObject;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "GET") return jsonResponse({ error: "Method not allowed" }, 405);

  const slug = new URL(request.url).searchParams.get("slug")?.trim() || "";
  if (!slug || slug.length > 160 || !/^[A-Za-z0-9_-]+$/.test(slug)) return notFound();

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    console.error("public-trip: Supabase server credentials are not configured");
    return jsonResponse({ error: "Публичный просмотр временно недоступен." }, 503);
  }

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const { data: row, error } = await admin
    .from("trips")
    .select("id,payload")
    .eq("id", slug)
    .maybeSingle();
  if (error) {
    console.error("public-trip: trip lookup failed", error);
    return jsonResponse({ error: "Публичный просмотр временно недоступен." }, 500);
  }
  if (!row || !isJsonObject(row.payload)) return notFound();

  const payload = row.payload;
  const nestedData = isJsonObject(payload.data) ? payload.data : {};
  const nestedTrip = isJsonObject(nestedData.trip) ? nestedData.trip : {};
  const publicLinkEnabled = payload.publicLinkEnabled ?? nestedTrip.publicLinkEnabled;
  if (publicLinkEnabled === false || stringValue(payload.deletedAt)) return notFound();

  const publicTrip = publicTripFromPayload(slug, payload);
  if (!publicTrip) return notFound();

  return jsonResponse({ trip: await signPublicPhotos(admin, publicTrip) });
});
