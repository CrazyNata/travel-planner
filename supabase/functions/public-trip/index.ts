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

  // Keep the same trip payload that the authenticated trip screen receives.
  // The public route is read-only in the client, so the endpoint only removes
  // storage/auth internals and normalizes legacy nested payloads below.
  const publicTrip: JsonObject = { ...payload };
  delete publicTrip.data;
  delete publicTrip.owner_id;
  delete publicTrip.ownerId;
  delete publicTrip.userId;
  delete publicTrip.isOwner;

  const copyArrayFallback = (key: string, ...values: unknown[]) => {
    if (Array.isArray(publicTrip[key])) return;
    const fallback = values.find((value) => Array.isArray(value));
    if (Array.isArray(fallback)) publicTrip[key] = fallback;
  };
  const copyValueFallback = (key: string, ...values: unknown[]) => {
    if (publicTrip[key] !== undefined && publicTrip[key] !== null) return;
    const fallback = values.find((value) => value !== undefined && value !== null);
    if (fallback !== undefined && fallback !== null) publicTrip[key] = fallback;
  };

  copyArrayFallback("days", nestedData.days, publicDays(payload, nestedData));
  copyArrayFallback("sights", nestedData.sights);
  copyArrayFallback("sightDays", nestedTrip.sightDays, nestedData.sightDays);
  copyArrayFallback("restaurants", nestedData.restaurants);
  copyArrayFallback(
    "accommodations",
    nestedTrip.accommodations,
    nestedData.accommodations,
    publicAccommodations(payload, nestedData, nestedTrip),
  );
  copyArrayFallback("budgetExpenses", nestedData.budgetExpenses);
  copyArrayFallback("petPlaces", nestedTrip.petPlaces, nestedData.petPlaces);
  copyArrayFallback("members", nestedTrip.members, nestedData.members);
  copyArrayFallback("overviewMapPoints", nestedTrip.overviewMapPoints, nestedData.overviewMapPoints);
  copyArrayFallback("coverPhotos", nestedTrip.coverPhotos, nestedTrip.photos, nestedData.coverPhotos);
  copyArrayFallback("photos", nestedTrip.photos, nestedTrip.coverPhotos, nestedData.photos);

  copyValueFallback("dates", nestedTrip.dates);
  copyValueFallback("startDate", nestedTrip.startDate, nestedTrip.start);
  copyValueFallback("endDate", nestedTrip.endDate, nestedTrip.end);
  copyValueFallback("cities", nestedTrip.cities);
  copyValueFallback("status", nestedTrip.status);
  copyValueFallback("coverImage", nestedTrip.coverImage);
  copyValueFallback("coverTextColor", nestedTrip.coverTextColor);
  copyValueFallback("sightDaysVersion", nestedTrip.sightDaysVersion, nestedData.sightDaysVersion);
  copyValueFallback("sightNotes", nestedTrip.sightNotes, nestedData.sightNotes);
  copyValueFallback("budgetSplit", nestedTrip.budgetSplit, nestedData.budgetSplit);
  copyValueFallback("budgetCurrency", nestedTrip.budgetCurrency, nestedData.budgetCurrency);
  copyValueFallback("budgetManualRates", nestedTrip.budgetManualRates, nestedData.budgetManualRates);

  publicTrip.id = id;
  publicTrip.title = title;
  publicTrip.dates = firstString(publicTrip.dates, nestedTrip.dates);
  publicTrip.startDate = firstString(publicTrip.startDate, nestedTrip.startDate, nestedTrip.start);
  publicTrip.endDate = firstString(publicTrip.endDate, nestedTrip.endDate, nestedTrip.end);

  const days = Array.isArray(publicTrip.days) ? publicTrip.days : [];
  const fallbackCities = days
    .filter(isJsonObject)
    .map((day) => {
      const roadLeg = isJsonObject(day.roadLeg) ? day.roadLeg : {};
      return firstString(day.city, roadLeg.to, roadLeg.from);
    })
    .filter(Boolean);
  publicTrip.cities = firstString(publicTrip.cities, nestedTrip.cities) ||
    [...new Set(fallbackCities)].join(" · ");

  const coverPhotos = Array.isArray(publicTrip.coverPhotos)
    ? publicTrip.coverPhotos
    : publicCoverPhotos(firstArray(payload.coverPhotos, payload.photos, nestedTrip.coverPhotos, nestedTrip.photos));
  if (!Array.isArray(publicTrip.coverPhotos)) publicTrip.coverPhotos = coverPhotos;
  if (!Array.isArray(publicTrip.photos)) publicTrip.photos = coverPhotos;
  publicTrip.coverImage = firstString(
    publicTrip.coverImage,
    nestedTrip.coverImage,
    coverPhotos.find(isJsonObject)?.image,
  );

  // Some early payloads stored places at the top level instead of in days.
  if (!days.length) {
    const topLevelPlaces = firstArray(publicTrip.places)
      .map(stringValue)
      .filter(Boolean);
    if (topLevelPlaces.length) {
      publicTrip.days = [{
        id: "public-day-1",
        city: publicTrip.cities || "Маршрут",
        date: "",
        places: topLevelPlaces,
      }];
    }
  }

  return publicTrip;
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
