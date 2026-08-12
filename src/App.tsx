import {
  useEffect,
  useEffectEvent,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import type { Map } from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import { matchPath, useLocation, useNavigate } from "react-router-dom";
import { setAuthSessionPersistence, supabase } from "./supabase";

type View =
  | "auth"
  | "trips"
  | "create"
  | "trip"
  | "catalog"
  | "public"
  | "delete-account"
  | "privacy"
  | "terms";
type Tab =
  | "overview"
  | "route"
  | "sights"
  | "restaurants"
  | "accommodation"
  | "bookings"
  | "budget"
  | "photos"
  | "members";
type RoadLeg = {
  from: string;
  to: string;
  checkInFrom: string;
  checkInTo: string;
  checkOutFrom: string;
  checkOutTo: string;
  notes: string;
  mapsUrl?: string;
  completed?: string[];
};
type DraftDay = { id: string; places: string[]; roadLeg?: RoadLeg };
type CoverPhoto = {
  id: string;
  image: string;
  city?: string;
  description?: string;
  textColor?: string;
};
let weatherCoverPhotos: CoverPhoto[] = [];
type TripMember = {
  id: string;
  initials: string;
  name: string;
  email: string;
  role: "Владелец" | "Редактор" | "Читатель";
  tone: "sand" | "green" | "blue";
};
type TripSummary = {
  id: string;
  title: string;
  dates: string;
  startDate?: string;
  endDate?: string;
  cities: string;
  status: string;
  progress: number;
  tone: string;
  isDraft?: boolean;
  coverImage?: string;
  coverPhotos?: CoverPhoto[];
  coverCity?: string;
  coverDescription?: string;
  coverTextColor?: string;
  overviewMapPoints?: string[];
  places?: string[];
  days?: DraftDay[];
  sights?: StoredSight[];
  sightDays?: { id: string; title: string; photo?: string; photoPosition?: number }[];
  sightDaysVersion?: number;
  sightNotes?: Record<string, string>;
  restaurants?: ImportedRestaurant[];
  accommodations?: SavedAccommodation[];
  budgetExpenses?: BudgetExpense[];
  budgetSplit?: BudgetSplit;
  budgetCurrency?: BudgetCurrency;
  members?: TripMember[];
  publicLinkEnabled?: boolean;
  published?: boolean;
};
type TripRow = {
  id: string;
  payload: TripSummary;
};
type SavedAccommodation = {
  id: string;
  name: string;
  city: string;
  dates: string;
  days: number;
  deadline: string;
  progress: number;
  status: string;
  price: string;
  bookingUrl: string;
  details: string;
  photos: string[];
  photoTransforms?: {
    offsetX?: number;
    offsetY?: number;
    scaleX?: number;
    scaleY?: number;
  }[];
};
type ImportedRestaurant = {
  id: string;
  name: string;
  city: string;
  status: string;
  note?: string;
  link?: string;
  price?: string;
  googleRating?: number;
  googleReviews?: number;
  photos?: string[];
  placeType?: string;
  categories?: string[];
  priority?: boolean;
  dogFriendly?: boolean;
};
type BudgetScope = "общий" | "семья" | "личный";
type BudgetCurrency = "EUR" | "RUB" | "CZK";
type BudgetExpense = {
  id: string;
  name: string;
  amount: number;
  category: string;
  scope: BudgetScope;
  paidBy: string;
  date?: string;
};
type BudgetSplit = {
  groups: { id: string; name: string; people: number }[];
};
const budgetCurrencies: Record<
  BudgetCurrency,
  { label: string; rate: number }
> = {
  EUR: { label: "€", rate: 1 },
  RUB: { label: "₽", rate: 100 },
  CZK: { label: "Kč", rate: 25 },
};

function formatBudgetAmount(amount: number, currency: BudgetCurrency) {
  const { label, rate } = budgetCurrencies[currency];
  return `${(amount * rate).toLocaleString("ru-RU", {
    maximumFractionDigits: 2,
  })} ${label}`;
}
type StoredDay = {
  id?: string;
  city?: string;
  dayMapUrl?: string;
  checkInFrom?: string;
  checkInTo?: string;
  checkOutFrom?: string;
  checkOutTo?: string;
  completed?: string[];
  items?: { id?: string; title?: string; done?: boolean }[];
};
type StoredSight = {
  id: string;
  name: string;
  city: string;
  time?: string;
  done?: boolean;
  group?: string;
  photo?: string;
  photoPosition?: number;
  lnglat?: [number, number];
  walkDay?: number;
  walkOrder?: number;
  subcategory?: string;
  description?: string;
  duration?: string;
};
type DayPlaceDraft = Pick<
  StoredSight,
  "name" | "subcategory" | "description" | "photo" | "photoPosition"
>;
type StoredTripPayload = {
  data?: {
    days?: StoredDay[];
    sights?: StoredSight[];
    trip?: {
      title?: string;
      start?: string;
      end?: string;
      isDraft?: boolean;
      status?: string;
      coverImage?: string;
      coverPhotos?: CoverPhoto[];
      coverTextColor?: string;
      overviewMapPoints?: string[];
      sightDays?: { id: string; title: string; photo?: string; photoPosition?: number }[];
      sightDaysVersion?: number;
      sightNotes?: Record<string, string>;
      members?: TripMember[];
      publicLinkEnabled?: boolean;
      published?: boolean;
    };
    [key: string]: unknown;
  };
  [key: string]: unknown;
};
const emptyPlaces: string[] = [];
const emptyRouteDays: DraftDay[] = [];

function mapsUrl(from: string, to: string) {
  return `https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(from)}&destination=${encodeURIComponent(to)}&travelmode=driving`;
}

function formatTripDates(start?: string, end?: string) {
  if (!start || !end) return "Даты путешествия";
  const startDate = new Date(`${start}T00:00:00Z`);
  const endDate = new Date(`${end}T00:00:00Z`);
  if (Number.isNaN(startDate.getTime()) || Number.isNaN(endDate.getTime()))
    return `${start} – ${end}`;
  const days =
    Math.round((endDate.getTime() - startDate.getTime()) / 86_400_000) + 1;
  const month = new Intl.DateTimeFormat("ru-RU", {
    month: "long",
    timeZone: "UTC",
  });
  const startMonth = month.format(startDate);
  const endMonth = month.format(endDate);
  const startYear = startDate.getUTCFullYear();
  const endYear = endDate.getUTCFullYear();
  const range =
    startMonth === endMonth && startYear === endYear
      ? `${startDate.getUTCDate()}–${endDate.getUTCDate()} ${startMonth} ${startYear}`
      : `${startDate.getUTCDate()} ${startMonth} ${startYear} – ${endDate.getUTCDate()} ${endMonth} ${endYear}`;
  return `${range} · ${days} ${days === 1 ? "день" : days < 5 ? "дня" : "дней"}`;
}

function normalizeTripDates(dates: string) {
  const match = dates.match(
    /^(\d{4}-\d{2}-\d{2})\s*[–-]\s*(\d{4}-\d{2}-\d{2})$/,
  );
  return match ? formatTripDates(match[1], match[2]) : dates;
}

function cityFlag(city: string) {
  if (city.includes("Прага")) return "🇨🇿";
  if (city.includes("Зальцбург")) return "🇦🇹";
  if (city.includes("Мюнхен") || city.includes("Равенсбург")) return "🇩🇪";
  if (
    [
      "Верона",
      "Рим",
      "Пиза",
      "Фильине",
      "Сан-Марино",
      "Кьоджа",
      "Милан",
      "Вальдидентро",
      "Флоренция",
      "Венеция",
    ].some((name) => city.includes(name))
  )
    return "🇮🇹";
  return "";
}

type PhotoMetadata = { date?: string; city?: string };

function photoCity(latitude?: number, longitude?: number) {
  if (latitude === undefined || longitude === undefined) return undefined;
  const cities = [
    [41.9, 12.5, "Рим"],
    [43.77, 11.25, "Флоренция"],
    [45.44, 12.33, "Венеция"],
  ] as const;
  return cities
    .map(([lat, lng, city]) => ({
      city,
      distance: Math.hypot(latitude - lat, longitude - lng),
    }))
    .sort((a, b) => a.distance - b.distance)[0]?.distance < 0.55
    ? cities
        .map(([lat, lng, city]) => ({
          city,
          distance: Math.hypot(latitude - lat, longitude - lng),
        }))
        .sort((a, b) => a.distance - b.distance)[0]?.city
    : undefined;
}

async function readPhotoMetadata(file: File): Promise<PhotoMetadata> {
  if (!file.type.includes("jpeg")) return {};
  const data = new DataView(await file.arrayBuffer());
  let offset = 2;
  while (offset + 4 < data.byteLength) {
    if (data.getUint8(offset) !== 0xff) break;
    const marker = data.getUint8(offset + 1);
    const size = data.getUint16(offset + 2, false);
    if (marker !== 0xe1 || data.getUint32(offset + 4, false) !== 0x45786966) {
      offset += size + 2;
      continue;
    }
    const tiff = offset + 10;
    const little = data.getUint16(tiff, false) === 0x4949;
    const u16 = (position: number) => data.getUint16(position, little);
    const u32 = (position: number) => data.getUint32(position, little);
    const ascii = (position: number, length: number) =>
      String.fromCharCode(
        ...Array.from({ length: length - 1 }, (_, index) =>
          data.getUint8(position + index),
        ),
      );
    const ifd = (position: number) => {
      const count = u16(position);
      return Array.from({ length: count }, (_, index) => {
        const entry = position + 2 + index * 12;
        return {
          tag: u16(entry),
          type: u16(entry + 2),
          count: u32(entry + 4),
          value: entry + 8,
          offset: u32(entry + 8),
        };
      });
    };
    const base = tiff;
    const entries = ifd(base + u32(base + 4));
    const dateEntry = entries.find(
      (entry) => entry.tag === 0x9003 || entry.tag === 0x0132,
    );
    const gpsEntry = entries.find((entry) => entry.tag === 0x8825);
    const date = dateEntry
      ? ascii(base + dateEntry.offset, dateEntry.count)
          .replace(/:/g, ".")
          .replace(" ", " · ")
      : undefined;
    if (!gpsEntry) return { date };
    const gps = ifd(base + gpsEntry.offset);
    const gpsValue = (tag: number) => gps.find((entry) => entry.tag === tag);
    const rational = (entry?: { offset: number; count: number }) =>
      entry
        ? Array.from({ length: entry.count }, (_, index) => {
            const point = base + entry.offset + index * 8;
            return u32(point) / u32(point + 4);
          })
        : [];
    const latitudeParts = rational(gpsValue(2));
    const longitudeParts = rational(gpsValue(4));
    const latitude =
      latitudeParts.length === 3
        ? latitudeParts[0] + latitudeParts[1] / 60 + latitudeParts[2] / 3600
        : undefined;
    const longitude =
      longitudeParts.length === 3
        ? longitudeParts[0] + longitudeParts[1] / 60 + longitudeParts[2] / 3600
        : undefined;
    const ref = (tag: number) => {
      const entry = gpsValue(tag);
      return entry
        ? String.fromCharCode(data.getUint8(base + entry.offset))
        : "";
    };
    return {
      date,
      city: photoCity(
        ref(1) === "S" && latitude ? -latitude : latitude,
        ref(3) === "W" && longitude ? -longitude : longitude,
      ),
    };
  }
  return {};
}

function savedTrip(payload: StoredTripPayload): TripSummary | null {
  const storedDays = payload.data?.days;
  if (!storedDays?.length) return null;
  const days = storedDays.map((day, index) => {
    const [from = "", to = ""] = (day.city || "")
      .split("→")
      .map((city) => city.trim());
    const completed =
      day.completed ||
      day.items?.flatMap((item) => {
        if (!item.done) return [];
        if (item.title?.startsWith("Выезд")) return ["departure"];
        if (item.title?.startsWith("Заселение")) return ["check-in"];
        if (item.title?.startsWith("Выселение")) return ["check-out"];
        return [];
      }) ||
      [];
    return {
      id: day.id || `saved-day-${index + 1}`,
      places: day.items?.map((item) => item.title || "").filter(Boolean) || [],
      roadLeg:
        from || to
          ? {
              from,
              to,
              checkInFrom: day.checkInFrom || "",
              checkInTo: day.checkInTo || "",
              checkOutFrom: day.checkOutFrom || "",
              checkOutTo: day.checkOutTo || "",
              notes: "",
              mapsUrl: day.dayMapUrl,
              completed,
            }
          : undefined,
    };
  });
  const start = payload.data?.trip?.start;
  const end = payload.data?.trip?.end;
  return {
    id: "supabase-main",
    title: payload.data?.trip?.title || "Путешествие",
    dates: formatTripDates(start, end),
    startDate: start,
    endDate: end,
    cities: storedDays
      .map((day) => day.city)
      .filter(Boolean)
      .slice(0, 3)
      .join(" · "),
    status:
      payload.data?.trip?.status ||
      (payload.data?.trip?.isDraft === false ? "Предстоящее" : "Черновик"),
    progress: 0,
    tone: "stone",
    // List status must not switch the route into a different interface.
    isDraft: true,
    coverImage: payload.data?.trip?.coverImage,
    coverPhotos: payload.data?.trip?.coverPhotos,
    coverTextColor: payload.data?.trip?.coverTextColor,
    overviewMapPoints: payload.data?.trip?.overviewMapPoints,
    sights: payload.data?.sights,
    sightDays: payload.data?.trip?.sightDays,
    sightDaysVersion: payload.data?.trip?.sightDaysVersion,
    sightNotes: payload.data?.trip?.sightNotes,
    members: payload.data?.trip?.members,
    publicLinkEnabled: payload.data?.trip?.publicLinkEnabled,
    published: payload.data?.trip?.published,
    days,
  };
}

function tripFromRow(row: TripRow): TripSummary | null {
  if (!row.payload || typeof row.payload.title !== "string") return null;
  const days = row.payload.days?.map((day) =>
    day.roadLeg && !Array.isArray(day.roadLeg.completed)
      ? { ...day, roadLeg: { ...day.roadLeg, completed: [] } }
      : day,
  );
  return {
    ...row.payload,
    days,
    id: row.id,
    dates: normalizeTripDates(row.payload.dates),
    isDraft: true,
  } satisfies TripSummary;
}

async function saveUserData(key: string, value: unknown) {
  const {
    data: { session },
  } = await supabase.auth.getSession();
  if (!session?.user) return;
  const { error } = await supabase.from("user_data").upsert(
    { user_id: session.user.id, key, value },
    { onConflict: "user_id,key" },
  );
  if (error) console.error(`Could not save ${key}.`, error);
}

const tripSaveQueues = new globalThis.Map<string, Promise<void>>();

function saveTripToSupabase(trip: TripSummary) {
  const previous = tripSaveQueues.get(trip.id) || Promise.resolve();
  const next = previous
    .catch(() => undefined)
    .then(async () => {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session?.user) return;
      const { error } = await supabase.from("trips").upsert(
        { id: trip.id, owner_id: session.user.id, payload: trip },
        { onConflict: "id" },
      );
      if (error) throw error;
    })
    .catch((error) => console.error("Could not save the trip.", error))
    .finally(() => {
      if (tripSaveQueues.get(trip.id) === next) tripSaveQueues.delete(trip.id);
    });
  tripSaveQueues.set(trip.id, next);
}

const trips: TripSummary[] = [
  {
    id: "sample-italy",
    title: "Италия",
    dates: "12–19 сентября 2026 · 8 дней",
    cities: "Рим · Флоренция · Венеция",
    status: "Активное",
    progress: 78,
    tone: "sand",
  },
];

const days = [
  {
    city: "Рим",
    date: "12 сен",
    distance: "5,4 км",
    places: [
      "Завтрак у Пантеона",
      "Колизей",
      "Римский форум и Палатин",
      "Обед · Trattoria",
      "Фонтан Треви",
    ],
  },
  {
    city: "Рим",
    date: "13 сен",
    distance: "6,1 км",
    places: [
      "Музеи Ватикана",
      "Собор Св. Петра",
      "Замок Св. Ангела",
      "Ужин в Трастевере",
    ],
  },
  {
    city: "Флоренция",
    date: "14 сен",
    distance: "переезд",
    places: [
      "Поезд Рим → Флоренция",
      "Заселение · B&B Fiori",
      "Собор Санта-Мария-дель-Фьоре",
      "Галерея Уффици",
    ],
  },
  {
    city: "Флоренция",
    date: "15 сен",
    distance: "4,8 км",
    places: [
      "Галерея Академии",
      "Понте Веккьо",
      "Сады Боболи",
      "Пьяццале Микеланджело",
    ],
  },
  {
    city: "Венеция",
    date: "16 сен",
    distance: "переезд",
    places: [
      "Поезд Флоренция → Венеция",
      "Гранд-канал",
      "Площадь Сан-Марко",
      "Дворец Дожей",
    ],
  },
  {
    city: "Венеция",
    date: "17 сен",
    distance: "острова",
    places: ["Остров Мурано", "Остров Бурано", "Ужин · морепродукты"],
  },
  {
    city: "Венеция",
    date: "18 сен",
    distance: "3,2 км",
    places: ["Прогулка по Дорсодуро", "Галерея Академии", "Гондола на закате"],
  },
  {
    city: "Отъезд",
    date: "19 сен",
    distance: "—",
    places: ["Завтрак и сборы", "Трансфер в аэропорт", "Вылет домой"],
  },
];

const catalog = [
  [
    "Классическая Италия",
    "Рим · Флоренция · Венеция",
    "8 дней",
    "Анна С.",
    "342",
    "sand",
  ],
];

const mapLocations: Record<string, [number, number]> = {
  Прага: [14.4378, 50.0755],
  Зальцбург: [13.045, 47.8095],
  Мюнхен: [11.582, 48.1351],
  Равенсбург: [9.611, 47.781],
  Верона: [10.9916, 45.4384],
  Рим: [12.4964, 41.9028],
  Пиза: [10.4017, 43.7228],
  "Фильине-Вальдарно": [11.469, 43.62],
  "Сан-Марино": [12.4578, 43.9424],
  Кьоджа: [12.278, 45.219],
  Милан: [9.19, 45.4642],
  Вальдидентро: [10.3, 46.49],
  Флоренция: [11.2558, 43.7696],
  Венеция: [12.3155, 45.4408],
  Москва: [37.6173, 55.7558],
};

const accommodationCities = [
  "Амстердам, Нидерланды",
  "Барселона, Испания",
  "Берлин, Германия",
  "Болонья, Италия",
  "Будапешт, Венгрия",
  "Венеция, Италия",
  "Верона, Италия",
  "Вена, Австрия",
  "Гамбург, Германия",
  "Генуя, Италия",
  "Дубай, ОАЭ",
  "Кёльн, Германия",
  "Кьоджа, Италия",
  "Лиссабон, Португалия",
  "Лондон, Великобритания",
  "Мадрид, Испания",
  "Милан, Италия",
  "Мюнхен, Германия",
  "Неаполь, Италия",
  "Париж, Франция",
  "Прага, Чехия",
  "Равенсбург, Германия",
  "Рим, Италия",
  "Сан-Марино, Сан-Марино",
  "Стамбул, Турция",
  "Турин, Италия",
  "Фильине-Вальдарно, Италия",
  "Флоренция, Италия",
  "Цюрих, Швейцария",
  "Зальцбург, Австрия",
];

function externalUrl(value: string) {
  if (!value) return "#";
  return /^https?:\/\//i.test(value) ? value : `https://${value}`;
}

function accommodationDateParts(value?: string) {
  const [checkIn = "2026-09-27", checkOut = "2026-09-30"] =
    value?.split(/\s+[–-]\s+/) || [];
  return { checkIn, checkOut };
}

function formatAccommodationDates(value: string) {
  const [startValue, endValue] = value.split(/\s+[–-]\s+/);
  const start = new Date(`${startValue}T00:00:00Z`);
  const end = new Date(`${endValue}T00:00:00Z`);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return value;
  const nights = Math.max(1, Math.round((end.getTime() - start.getTime()) / 86400000));
  const month = new Intl.DateTimeFormat("ru-RU", {
    month: "short",
    timeZone: "UTC",
  }).format;
  const range =
    start.getUTCMonth() === end.getUTCMonth()
      ? `${start.getUTCDate()}–${end.getUTCDate()} ${month(end).replace(".", "")}`
      : `${start.getUTCDate()} ${month(start).replace(".", "")} – ${end.getUTCDate()} ${month(end).replace(".", "")}`;
  const nightLabel =
    nights % 10 === 1 && nights % 100 !== 11
      ? "ночь"
      : nights % 10 >= 2 && nights % 10 <= 4 && (nights % 100 < 10 || nights % 100 >= 20)
        ? "ночи"
        : "ночей";
  return `${range} · ${nights} ${nightLabel}`;
}

function formatAccommodationPrice(value: string) {
  const amount = value.trim();
  if (!amount) return "";
  return /^[€$£₽]/.test(amount) || /[€$£₽]$/.test(amount)
    ? amount
    : `€${amount}`;
}

function mapStyle() {
  return "mapbox://styles/mapbox/streets-v12";
}

const sightImageCache = new globalThis.Map<string, string>();

function SightCardImage({ sight }: { sight: StoredSight }) {
  const cacheKey = `${sight.name}|${sight.city}`;
  const [image, setImage] = useState(
    () => sight.photo || sightImageCache.get(cacheKey) || "",
  );
  useEffect(() => {
    if (sight.photo) {
      setImage(sight.photo);
      return;
    }
    const cached = sightImageCache.get(cacheKey);
    if (cached) {
      setImage(cached);
      return;
    }
    const controller = new AbortController();
    const params = new URLSearchParams({
      action: "query",
      generator: "search",
      gsrsearch: `${sight.name} ${sight.city}`,
      gsrnamespace: "6",
      prop: "imageinfo",
      iiprop: "url",
      iiurlwidth: "900",
      format: "json",
      origin: "*",
    });
    void fetch(`https://commons.wikimedia.org/w/api.php?${params}`, {
      signal: controller.signal,
    })
      .then((response) => response.json())
      .then((data: {
        query?: {
          pages?: Record<
            string,
            { index?: number; imageinfo?: { thumburl?: string }[] }
          >;
        };
      }) => {
        const photo = Object.values(data.query?.pages || {})
          .sort((first, second) => (first.index || 0) - (second.index || 0))
          .find((page) => page.imageinfo?.[0]?.thumburl)?.imageinfo?.[0]
          ?.thumburl;
        if (!photo) return;
        sightImageCache.set(cacheKey, photo);
        setImage(photo);
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, [cacheKey, sight.city, sight.name, sight.photo]);
  if (!image) return null;
  return (
    <img
      src={image}
      alt={`${sight.name}, ${sight.city}`}
      style={{ objectPosition: `center ${sight.photoPosition ?? 50}%` }}
    />
  );
}

function mapLocation(city: string) {
  return Object.entries(mapLocations).find(([name]) =>
    city.includes(name),
  )?.[1];
}

function routeCoordinatesFor(days: DraftDay[]) {
  return days
    .flatMap((day, index) => {
      const leg = day.roadLeg;
      if (!leg) return [];
      return index === 0 ? [leg.from, leg.to] : [leg.to];
    })
    .map(mapLocation)
    .filter((coordinate): coordinate is [number, number] =>
      Boolean(coordinate),
    );
}

function routeSegment(coordinates: [number, number][], day: number) {
  return coordinates.slice(day, day + 2);
}

const winterPhotoCaptions = [
  [
    "Мюнхен",
    "Столица Баварии в декабре превращается в светящуюся рождественскую сцену. Готические башни Новой ратуши возвышаются над ярмаркой на Мариенплац.",
  ],
  [
    "Верона",
    "Зимняя Пьяцца Бра сияет огнями рождественской ярмарки у стен древней Арены. Вечерняя прогулка здесь соединяет итальянскую историю и праздничное настроение.",
  ],
  [
    "Рим",
    "Вечный город зимой становится спокойнее, но не теряет своего характера. Тёплый свет площадей делает вечерние прогулки особенно красивыми.",
  ],
  [
    "Кьоджа",
    "Небольшой город у лагуны хранит морской ритм и тихие каналы. Здесь удобно замедлиться перед дорогой в Венецию.",
  ],
  [
    "Венеция",
    "Город каналов зимой звучит тише: туман, вода и старые фасады создают почти кинематографичное настроение.",
  ],
  [
    "Милан",
    "Милан соединяет праздничные витрины, современный ритм и классическую итальянскую архитектуру. Вечерний город особенно живой.",
  ],
  [
    "Равенсбург",
    "Средневековые башни и цветные фасады делают Равенсбург уютной остановкой на зимнем маршруте.",
  ],
  [
    "Прага",
    "Прага в праздничный сезон сияет огнями Старого города. Каменные мосты и черепичные крыши создают атмосферу зимней сказки.",
  ],
] as const;

const munichDayOneSights: StoredSight[] = [
  {
    id: "munich-karlsplatz",
    name: "Karlsplatz (Stachus)",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 0,
    lnglat: [11.5659, 48.1391],
    duration: "20 мин",
    description:
      "Оживлённая площадь у западного входа в исторический центр Мюнхена.",
  },
  {
    id: "munich-neuhauser",
    name: "Neuhauser Straße",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 1,
    lnglat: [11.5685, 48.1385],
    duration: "30 мин",
    description:
      "Пешеходная улица с рождественскими витринами, гирляндами и праздничными украшениями.",
  },
  {
    id: "munich-karlstor",
    name: "Karlstor",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 2,
    lnglat: [11.5656, 48.1389],
    duration: "15 мин",
    description:
      "Средневековые городские ворота, открывающие путь в Старый город.",
  },
  {
    id: "munich-marienplatz",
    name: "Marienplatz",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 3,
    lnglat: [11.5755, 48.1374],
    duration: "30 мин",
    description:
      "Главная площадь Мюнхена и сердце праздничного Старого города.",
  },
  {
    id: "munich-neues-rathaus",
    name: "Новая ратуша (Neues Rathaus)",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 4,
    lnglat: [11.5756, 48.1376],
    duration: "30 мин",
    description:
      "Неоготическая ратуша с башней, часами и знаменитым Глокеншпилем.",
  },
  {
    id: "munich-christkindlmarkt",
    name: "Christkindlmarkt",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 5,
    lnglat: [11.5752, 48.1372],
    duration: "1,5 ч",
    description:
      "Главная рождественская ярмарка города с ремесленными лавками и баварскими угощениями.",
  },
  {
    id: "munich-frauenkirche",
    name: "Frauenkirche",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 6,
    lnglat: [11.5734, 48.1386],
    duration: "30 мин",
    description:
      "Кафедральный собор и один из главных архитектурных символов Мюнхена.",
  },
  {
    id: "munich-kaufingerstrasse",
    name: "Kaufingerstraße",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 7,
    lnglat: [11.5712, 48.1379],
    duration: "30 мин",
    description:
      "Праздничная торговая улица, особенно красивая в вечерней подсветке.",
  },
  {
    id: "munich-residenz-weihnachtsdorf",
    name: "Residenz Weihnachtsdorf",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 8,
    lnglat: [11.5784, 48.1411],
    duration: "1 ч",
    description:
      "Уютная рождественская деревня во дворе Мюнхенской резиденции.",
  },
  {
    id: "munich-max-joseph-platz",
    name: "Max-Joseph-Platz",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 9,
    lnglat: [11.5789, 48.1398],
    duration: "20 мин",
    description:
      "Парадная площадь перед Баварской государственной оперой и Резиденцией.",
  },
  {
    id: "munich-odeonsplatz",
    name: "Odeonsplatz",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 10,
    lnglat: [11.5777, 48.1421],
    duration: "25 мин",
    description:
      "Монументальная площадь на границе Старого города и дворцового квартала.",
  },
  {
    id: "munich-feldherrnhalle",
    name: "Feldherrnhalle",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 11,
    lnglat: [11.5778, 48.1424],
    duration: "15 мин",
    description: "Аркада XIX века, вдохновлённая флорентийской Лоджией Ланци.",
  },
  {
    id: "munich-theatinerkirche",
    name: "Theatinerkirche",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 12,
    lnglat: [11.5768, 48.1422],
    duration: "25 мин",
    description:
      "Барочная церковь с выразительным жёлтым фасадом и красивой вечерней подсветкой.",
  },
  {
    id: "munich-hofgarten",
    name: "Hofgarten",
    city: "Мюнхен",
    walkDay: 1,
    walkOrder: 13,
    lnglat: [11.5808, 48.1426],
    duration: "30 мин",
    description:
      "Спокойный придворный сад рядом с Резиденцией, завершающий прогулку.",
  },
];

const munichDayOneNotes = `🎅 Что обязательно попробовать
🍷 Glühwein
🌰 Жареный миндаль
🍪 Lebkuchen
🥔 Картофельные оладьи (Kartoffelpuffer)
🌭 Баварские сосиски`;

const legacyVeronaDayTwoNotes = `🎅 Что я бы обязательно попробовала за один вечер
⭐ Pandoro (обязательно — это родина десерта).
🍷 Vin Brulé.
🌰 Жареные каштаны.
🥜 Карамелизированный миндаль.
🍫 Горячий итальянский шоколад.`;

const veronaDayTwoNotes = `🎄 Pandoro (главный рождественский кекс, родом из Вероны)
🍷 Vin Brulé (итальянский глинтвейн)
🌰 Жареные каштаны (Caldarroste)
🥜 Карамелизированный миндаль (Mandorle Pralinate)
🍫 Горячий итальянский шоколад (Cioccolata Calda)
🍪 Рождественские имбирные пряники (Lebkuchen)
⭐ Рождественское печенье и марципан
🧀 Местные сыры (Monte Veronese)
🍷 Бокал вина Amarone della Valpolicella
🎁 Рождественские сладости и деликатесы на ярмарке Christkindlmarkt`;

const romeDayThreeNotes = `🎅 Что обязательно попробовать в Риме перед Рождеством
🎄 Panettone
🎄 Pandoro
🎄 Горячий шоколад (Cioccolata Calda)
🎄 Maritozzo con panna
🎄 Жареные каштаны (Caldarroste)
🎄 Supplì
🎄 Cacio e Pepe
🎄 Carbonara
🎄 Saltimbocca alla Romana
🎄 Тирамису`;

const pisaDaySixNotes = `🎄🍴 Что обязательно попробовать
☕ Горячий шоколад (Cioccolata Calda) — густой итальянский горячий шоколад
🍰 Buccellato Toscano — традиционный тосканский рождественский кекс
🍪 Ricciarelli — мягкое миндальное рождественское печенье
🍫 Panforte — пряный рождественский десерт с орехами и цукатами
🥐 Panettone — классический итальянский рождественский кулич
🍦 Джелато — даже зимой в Италии его едят круглый год
☕ Эспрессо или капучино в уютном кафе с видом на исторический центр
🌰 Жареные каштаны (если продаются на рождественских ярмарках)`;

const sanMarinoDaySevenNotes = `🎄🍴 Что обязательно попробовать
☕ Горячий шоколад (Cioccolata Calda) — идеален для прогулки по зимнему Сан-Марино
🍷 Vin Brulé — горячее пряное вино
🍰 Torta Tre Monti — самый знаменитый десерт Сан-Марино с вафлями, шоколадом и ореховым кремом
🍪 Panettone — классическая рождественская выпечка
🧀 Пьядина (Piadina) — лепешка с прошутто, сыром или рукколой
🥩 Тальятелле с рагу — одно из традиционных блюд региона
🧀 Местные сыры и салями — отличный вариант для перекуса
🍦 Джелато — если погода позволит
🍬 Купить Torta Tre Monti в подарок домой
🎁 Заглянуть в магазины за местными ликерами, шоколадом и рождественскими сувенирами`;

const chioggiaDayEightNotes = `🎄🍴 Что обязательно попробовать
🦑 Fritto Misto di Mare — ассорти из жареных морепродуктов
🦪 Moeche — знаменитые мягкопанцирные крабы (если будут в сезон)
🐙 Sarde in Saor — сардины в кисло-сладком маринаде
🦐 Ризотто с морепродуктами
🦀 Spaghetti alle Vongole — паста с моллюсками
🐟 Grigliata di Pesce — ассорти из рыбы и морепродуктов на гриле
🍷 Spritz Aperol или Spritz Select
☕ Горячий шоколад (Cioccolata Calda)
🍰 Panettone
🍪 Pandoro — традиционный рождественский десерт региона Венето
🌰 Жареные каштаны
🍦 Джелато
🍋 Limoncello или местный ликер после ужина`;

const veniceDayNineNotes = `🎄🍴 Что обязательно попробовать
☕ Горячий шоколад (Cioccolata Calda) — густой и насыщенный
🍷 Vin Brulé — горячее пряное вино
🥪 Cicchetti — знаменитые венецианские закуски
🦑 Черное ризотто с каракатицей (Risotto al Nero di Seppia)
🦀 Spaghetti alle Vongole — паста с моллюсками
🐟 Baccalà Mantecato — крем из соленой трески на тосте
🦐 Fritto Misto di Mare — жареные морепродукты
🍰 Pandoro — рождественский десерт родом из региона Венето
🍞 Panettone — классическая рождественская выпечка
🍪 Baicoli — традиционное венецианское печенье
🌰 Жареные каштаны
🍦 Джелато
☕ Выпить эспрессо в историческом кафе
🎁 Купить венецианское печенье, шоколад или рождественские сладости в подарок`;

const milanDayTenNotes = `🎄🍴 Что обязательно попробовать
🍰 Panettone — главный рождественский десерт Милана
🍞 Pandoro — традиционный итальянский рождественский кекс
☕ Горячий шоколад (Cioccolata Calda)
🌰 Жареные каштаны
🍪 Amaretti — миндальное печенье
🥩 Cotoletta alla Milanese — знаменитая миланская отбивная
🍚 Risotto alla Milanese — ризотто с шафраном
🧀 Panzerotti — жареные пирожки с начинкой
🍦 Джелато
🍷 Vin Brulé — горячее пряное вино
☕ Эспрессо в историческом кафе
🎁 Купить Panettone, рождественские сладости или итальянский шоколад домой`;

const pragueNotes = `🎄🍴 Что обязательно попробовать
🍷 Svařák — чешский глинтвейн
☕ Horká čokoláda — густой горячий шоколад
🥐 Trdelník — традиционная сладкая выпечка
🧀 Smažený sýr — жареный сыр
🥩 Svíčková na smetaně — говядина в сливочном соусе
🍖 Vepřové koleno — запеченная свиная рулька
🥟 Bramborák — картофельные драники
🌭 Pražská klobása — пражская колбаска с рождественской ярмарки
🥔 Bramborové spirály — картофельные спирали
🍯 Medovina — горячая медовуха
🍪 Perníčky — рождественские пряники
🍰 Vánočka — чешский рождественский сладкий хлеб
🍺 Чешское крафтовое или традиционное пиво`;

const veronaDayTwoSights: StoredSight[] = [
  {
    id: "verona-piazza-bra",
    name: "Piazza Bra",
    city: "Верона",
    walkDay: 2,
    walkOrder: 0,
    lnglat: [10.9915, 45.4384],
    duration: "25 мин",
  },
  {
    id: "verona-arena",
    name: "Арена Вероны (Arena di Verona)",
    city: "Верона",
    walkDay: 2,
    walkOrder: 1,
    lnglat: [10.9942, 45.438],
    duration: "45 мин",
  },
  {
    id: "verona-rigoletto",
    name: "Рождественская звезда Rigoletto",
    city: "Верона",
    walkDay: 2,
    walkOrder: 2,
    lnglat: [10.9923, 45.4384],
    duration: "20 мин",
  },
  {
    id: "verona-mazzini",
    name: "Via Giuseppe Mazzini",
    city: "Верона",
    walkDay: 2,
    walkOrder: 3,
    lnglat: [10.9958, 45.4401],
    duration: "30 мин",
  },
  {
    id: "verona-erbe",
    name: "Piazza delle Erbe",
    city: "Верона",
    walkDay: 2,
    walkOrder: 4,
    lnglat: [10.9972, 45.4431],
    duration: "30 мин",
  },
  {
    id: "verona-signori",
    name: "Piazza dei Signori",
    city: "Верона",
    walkDay: 2,
    walkOrder: 5,
    lnglat: [10.9983, 45.4425],
    duration: "25 мин",
  },
  {
    id: "verona-christkindlmarkt",
    name: "Рождественская ярмарка Christkindlmarkt",
    city: "Верона",
    walkDay: 2,
    walkOrder: 6,
    lnglat: [10.998, 45.4427],
    duration: "1 ч",
  },
  {
    id: "verona-juliet",
    name: "Дворик Джульетты (Casa di Giulietta)",
    city: "Верона",
    walkDay: 2,
    walkOrder: 7,
    lnglat: [10.9994, 45.4429],
    duration: "30 мин",
  },
  {
    id: "verona-ponte-pietra",
    name: "Ponte Pietra",
    city: "Верона",
    walkDay: 2,
    walkOrder: 8,
    lnglat: [11.0053, 45.4472],
    duration: "35 мин",
  },
  {
    id: "verona-adige",
    name: "Набережная реки Адидже",
    city: "Верона",
    walkDay: 2,
    walkOrder: 9,
    lnglat: [11.0037, 45.4465],
    duration: "30 мин",
  },
];

const romeDayThreeSights: StoredSight[] = [
  {
    id: "rome-navona",
    name: "Piazza Navona (рождественская ярмарка)",
    city: "Рим",
    walkDay: 3,
    walkOrder: 0,
    lnglat: [12.4731, 41.8992],
    duration: "1 ч",
  },
  {
    id: "rome-four-rivers",
    name: "Фонтан Четырех рек",
    city: "Рим",
    walkDay: 3,
    walkOrder: 1,
    lnglat: [12.4733, 41.8992],
    duration: "20 мин",
  },
  {
    id: "rome-sant-agnese",
    name: "Церковь Sant'Agnese in Agone",
    city: "Рим",
    walkDay: 3,
    walkOrder: 2,
    lnglat: [12.4735, 41.8991],
    duration: "25 мин",
  },
  {
    id: "rome-pantheon",
    name: "Пантеон",
    city: "Рим",
    walkDay: 3,
    walkOrder: 3,
    lnglat: [12.4769, 41.8986],
    duration: "40 мин",
  },
  {
    id: "rome-rotonda",
    name: "Piazza della Rotonda",
    city: "Рим",
    walkDay: 3,
    walkOrder: 4,
    lnglat: [12.4767, 41.899],
    duration: "20 мин",
  },
  {
    id: "rome-hadrian",
    name: "Храм Адриана",
    city: "Рим",
    walkDay: 3,
    walkOrder: 5,
    lnglat: [12.4793, 41.9002],
    duration: "20 мин",
  },
  {
    id: "rome-colonna",
    name: "Piazza Colonna",
    city: "Рим",
    walkDay: 3,
    walkOrder: 6,
    lnglat: [12.4792, 41.901],
    duration: "20 мин",
  },
  {
    id: "rome-marcus-aurelius",
    name: "Колонна Марка Аврелия",
    city: "Рим",
    walkDay: 3,
    walkOrder: 7,
    lnglat: [12.4794, 41.9014],
    duration: "15 мин",
  },
  {
    id: "rome-trevi",
    name: "Фонтан Треви",
    city: "Рим",
    walkDay: 3,
    walkOrder: 8,
    lnglat: [12.4833, 41.9009],
    duration: "30 мин",
  },
  {
    id: "rome-spagna",
    name: "Piazza di Spagna",
    city: "Рим",
    walkDay: 3,
    walkOrder: 9,
    lnglat: [12.4824, 41.906],
    duration: "20 мин",
  },
  {
    id: "rome-spanish-steps",
    name: "Испанская лестница",
    city: "Рим",
    walkDay: 3,
    walkOrder: 10,
    lnglat: [12.4828, 41.906],
    duration: "30 мин",
  },
  {
    id: "rome-spagna-tree",
    name: "Рождественская елка на Piazza di Spagna",
    city: "Рим",
    walkDay: 3,
    walkOrder: 11,
    lnglat: [12.4824, 41.906],
    duration: "20 мин",
  },
  {
    id: "rome-condotti",
    name: "Via Condotti",
    city: "Рим",
    walkDay: 3,
    walkOrder: 12,
    lnglat: [12.4798, 41.9055],
    duration: "30 мин",
  },
  {
    id: "rome-corso",
    name: "Via del Corso",
    city: "Рим",
    walkDay: 3,
    walkOrder: 13,
    lnglat: [12.4793, 41.9014],
    duration: "30 мин",
  },
];

const romeDayFourSights: StoredSight[] = [
  {
    id: "rome-colosseum",
    name: "Колизей",
    city: "Рим",
    walkDay: 4,
    walkOrder: 0,
    lnglat: [12.4922, 41.8902],
    duration: "1 ч",
  },
  {
    id: "rome-constantine",
    name: "Арка Константина",
    city: "Рим",
    walkDay: 4,
    walkOrder: 1,
    lnglat: [12.4909, 41.8899],
    duration: "20 мин",
  },
  {
    id: "rome-forum",
    name: "Римский форум",
    city: "Рим",
    walkDay: 4,
    walkOrder: 2,
    lnglat: [12.4853, 41.8925],
    duration: "1,5 ч",
  },
  {
    id: "rome-palatine",
    name: "Палатинский холм",
    city: "Рим",
    walkDay: 4,
    walkOrder: 3,
    lnglat: [12.4882, 41.889],
    duration: "1 ч",
  },
  {
    id: "rome-palatine-view",
    name: "Холм Палатин (смотровые площадки)",
    city: "Рим",
    walkDay: 4,
    walkOrder: 4,
    lnglat: [12.4871, 41.8898],
    duration: "30 мин",
  },
  {
    id: "rome-capitoline",
    name: "Капитолийская площадь",
    city: "Рим",
    walkDay: 4,
    walkOrder: 5,
    lnglat: [12.4828, 41.8933],
    duration: "30 мин",
  },
  {
    id: "rome-marcus-statue",
    name: "Статуя Марка Аврелия",
    city: "Рим",
    walkDay: 4,
    walkOrder: 6,
    lnglat: [12.4829, 41.8934],
    duration: "15 мин",
  },
  {
    id: "rome-forum-view",
    name: "Смотровая площадка на Форум",
    city: "Рим",
    walkDay: 4,
    walkOrder: 7,
    lnglat: [12.4835, 41.8927],
    duration: "25 мин",
  },
  {
    id: "rome-venezia",
    name: "Piazza Venezia",
    city: "Рим",
    walkDay: 4,
    walkOrder: 8,
    lnglat: [12.4828, 41.8962],
    duration: "25 мин",
  },
  {
    id: "rome-vittoriano",
    name: "Монумент Виктору Эммануилу II",
    city: "Рим",
    walkDay: 4,
    walkOrder: 9,
    lnglat: [12.4826, 41.8947],
    duration: "40 мин",
  },
  {
    id: "rome-altare-terrazza",
    name: "Панорамная терраса Altare della Patria",
    city: "Рим",
    walkDay: 4,
    walkOrder: 10,
    lnglat: [12.4824, 41.8949],
    duration: "30 мин",
  },
  {
    id: "rome-marcellus",
    name: "Театр Марцелла",
    city: "Рим",
    walkDay: 4,
    walkOrder: 11,
    lnglat: [12.4788, 41.8919],
    duration: "25 мин",
  },
  {
    id: "rome-octavia",
    name: "Портик Октавии",
    city: "Рим",
    walkDay: 4,
    walkOrder: 12,
    lnglat: [12.4778, 41.8924],
    duration: "20 мин",
  },
  {
    id: "rome-ghetto",
    name: "Еврейский квартал",
    city: "Рим",
    walkDay: 4,
    walkOrder: 13,
    lnglat: [12.4774, 41.8921],
    duration: "45 мин",
  },
  {
    id: "rome-campo",
    name: "Campo de' Fiori",
    city: "Рим",
    walkDay: 4,
    walkOrder: 14,
    lnglat: [12.4722, 41.8957],
    duration: "30 мин",
  },
  {
    id: "rome-popolo",
    name: "Piazza del Popolo",
    city: "Рим",
    walkDay: 4,
    walkOrder: 15,
    lnglat: [12.4769, 41.91],
    duration: "30 мин",
  },
  {
    id: "rome-pincio",
    name: "Терраса Pincio",
    city: "Рим",
    walkDay: 4,
    walkOrder: 16,
    lnglat: [12.4778, 41.9122],
    duration: "35 мин",
  },
];

const romeDayFiveSights: StoredSight[] = [
  {
    id: "rome-st-peter-square",
    name: "Площадь Святого Петра",
    city: "Рим",
    walkDay: 5,
    walkOrder: 0,
    lnglat: [12.4539, 41.9022],
    duration: "30 мин",
  },
  {
    id: "rome-st-peter-basilica",
    name: "Собор Святого Петра",
    city: "Рим",
    walkDay: 5,
    walkOrder: 1,
    lnglat: [12.4539, 41.9022],
    duration: "1,5 ч",
  },
  {
    id: "rome-vatican-tree",
    name: "Главная рождественская елка Ватикана",
    city: "Рим",
    walkDay: 5,
    walkOrder: 2,
    lnglat: [12.4538, 41.9023],
    duration: "20 мин",
  },
  {
    id: "rome-vatican-nativity",
    name: "Рождественский вертеп",
    city: "Рим",
    walkDay: 5,
    walkOrder: 3,
    lnglat: [12.4536, 41.9024],
    duration: "20 мин",
  },
  {
    id: "rome-conciliazione",
    name: "Via della Conciliazione",
    city: "Рим",
    walkDay: 5,
    walkOrder: 4,
    lnglat: [12.4595, 41.902],
    duration: "30 мин",
  },
  {
    id: "rome-borgo-pio",
    name: "Район Borgo Pio",
    city: "Рим",
    walkDay: 5,
    walkOrder: 5,
    lnglat: [12.4574, 41.904],
    duration: "40 мин",
  },
  {
    id: "rome-umberto",
    name: "Мост Умберто I",
    city: "Рим",
    walkDay: 5,
    walkOrder: 6,
    lnglat: [12.4752, 41.903],
    duration: "30 мин",
  },
  {
    id: "rome-tiber-walk",
    name: "Прогулка по набережной Тибра",
    city: "Рим",
    walkDay: 5,
    walkOrder: 7,
    lnglat: [12.471, 41.899],
    duration: "45 мин",
  },
  {
    id: "rome-tiberina",
    name: "Остров Тиберина",
    city: "Рим",
    walkDay: 5,
    walkOrder: 8,
    lnglat: [12.4781, 41.8932],
    duration: "30 мин",
  },
  {
    id: "rome-trastevere",
    name: "Район Трастевере",
    city: "Рим",
    walkDay: 5,
    walkOrder: 9,
    lnglat: [12.4699, 41.888],
    duration: "1 ч",
  },
  {
    id: "rome-santa-maria",
    name: "Базилика Santa Maria in Trastevere",
    city: "Рим",
    walkDay: 5,
    walkOrder: 10,
    lnglat: [12.4708, 41.8896],
    duration: "30 мин",
  },
  {
    id: "rome-santa-maria-square",
    name: "Piazza Santa Maria in Trastevere",
    city: "Рим",
    walkDay: 5,
    walkOrder: 11,
    lnglat: [12.4707, 41.8897],
    duration: "25 мин",
  },
  {
    id: "rome-janiculum",
    name: "Холм Джаниколо (Janiculum Hill)",
    city: "Рим",
    walkDay: 5,
    walkOrder: 12,
    lnglat: [12.4608, 41.8934],
    duration: "45 мин",
  },
  {
    id: "rome-acqua-paola",
    name: "Фонтан Аква Паола",
    city: "Рим",
    walkDay: 5,
    walkOrder: 13,
    lnglat: [12.456, 41.8893],
    duration: "25 мин",
  },
];

const sanMarinoDaySixSights: StoredSight[] = [
  {
    id: "san-marino-porta",
    name: "Porta San Francesco",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 0,
    lnglat: [12.4475, 43.9357],
    duration: "15 мин",
  },
  {
    id: "san-marino-streets",
    name: "Средневековые улочки исторического центра",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 1,
    lnglat: [12.4478, 43.9354],
    duration: "20 мин",
  },
  {
    id: "san-marino-basilica",
    name: "Базилика Сан-Марино",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 2,
    lnglat: [12.4473, 43.9361],
    duration: "20 мин",
  },
  {
    id: "san-marino-liberty",
    name: "Piazza della Libertà",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 3,
    lnglat: [12.447, 43.9363],
    duration: "20 мин",
  },
  {
    id: "san-marino-guard",
    name: "Смена караула у Правительственного дворца",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 4,
    lnglat: [12.447, 43.9363],
    duration: "15 мин",
  },
  {
    id: "san-marino-palazzo",
    name: "Palazzo Pubblico",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 5,
    lnglat: [12.447, 43.9363],
    duration: "20 мин",
  },
  {
    id: "san-marino-liberty-view",
    name: "Смотровая площадка Piazza della Libertà",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 6,
    lnglat: [12.4468, 43.9364],
    duration: "15 мин",
  },
  {
    id: "san-marino-eugippo",
    name: "Via Eugippo и Contrada del Collegio",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 7,
    lnglat: [12.4462, 43.9354],
    duration: "20 мин",
  },
  {
    id: "san-marino-guaita",
    name: "Первая башня Гуаита (Guaita)",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 8,
    lnglat: [12.4477, 43.934],
    duration: "25 мин",
  },
  {
    id: "san-marino-passo",
    name: "Панорамная тропа Passo delle Streghe",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 9,
    lnglat: [12.4487, 43.9327],
    duration: "25 мин",
  },
  {
    id: "san-marino-cesta",
    name: "Виды на Вторую башню Честа",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 10,
    lnglat: [12.4504, 43.9316],
    duration: "20 мин",
  },
  {
    id: "san-marino-panorama",
    name: "Панорамные виды на Апеннины и побережье Адриатики",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 11,
    lnglat: [12.4495, 43.932],
    duration: "20 мин",
  },
  {
    id: "san-marino-tree",
    name: "Главная рождественская елка на Piazza della Libertà",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 12,
    lnglat: [12.447, 43.9363],
    duration: "15 мин",
  },
  {
    id: "san-marino-lights",
    name: "Рождественская иллюминация исторического центра",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 13,
    lnglat: [12.4474, 43.9358],
    duration: "20 мин",
  },
  {
    id: "san-marino-market",
    name: "Рождественские ярмарочные домики",
    city: "Сан-Марино",
    walkDay: 7,
    walkOrder: 14,
    lnglat: [12.4471, 43.936],
    duration: "20 мин",
  },
];

const pisaDaySixSights: StoredSight[] = [
  {
    id: "pisa-miracoli",
    name: "Piazza dei Miracoli",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 0,
    lnglat: [10.3966, 43.723],
    duration: "30 мин",
  },
  {
    id: "pisa-tower",
    name: "Пизанская башня",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 1,
    lnglat: [10.3966, 43.723],
    duration: "45 мин",
  },
  {
    id: "pisa-cathedral",
    name: "Кафедральный собор Пизы",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 2,
    lnglat: [10.3958, 43.723],
    duration: "30 мин",
  },
  {
    id: "pisa-baptistery",
    name: "Баптистерий Святого Иоанна",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 3,
    lnglat: [10.3949, 43.723],
    duration: "30 мин",
  },
  {
    id: "pisa-camposanto",
    name: "Монументальное кладбище Кампосанто",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 4,
    lnglat: [10.3946, 43.724],
    duration: "30 мин",
  },
  {
    id: "pisa-photo",
    name: "Классическое фото с башней",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 5,
    lnglat: [10.3964, 43.7228],
    duration: "20 мин",
  },
  {
    id: "pisa-lights",
    name: "Прогулка по Piazza dei Miracoli",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 6,
    lnglat: [10.3961, 43.7234],
    duration: "30 мин",
  },
  {
    id: "pisa-cavalieri",
    name: "Piazza dei Cavalieri",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 7,
    lnglat: [10.4011, 43.7197],
    duration: "25 мин",
  },
  {
    id: "pisa-santo-stefano",
    name: "Церковь Santo Stefano dei Cavalieri",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 8,
    lnglat: [10.401, 43.7194],
    duration: "20 мин",
  },
  {
    id: "pisa-borgo",
    name: "Borgo Stretto",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 9,
    lnglat: [10.4057, 43.7177],
    duration: "35 мин",
  },
  {
    id: "pisa-garibaldi",
    name: "Piazza Garibaldi",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 10,
    lnglat: [10.4057, 43.7157],
    duration: "20 мин",
  },
  {
    id: "pisa-ponte-mezzo",
    name: "Мост Ponte di Mezzo",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 11,
    lnglat: [10.4055, 43.715],
    duration: "20 мин",
  },
  {
    id: "pisa-arno",
    name: "Набережная реки Арно",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 12,
    lnglat: [10.4053, 43.7147],
    duration: "30 мин",
  },
  {
    id: "pisa-tree",
    name: "Главная рождественская елка",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 13,
    lnglat: [10.4002, 43.708],
    duration: "20 мин",
  },
  {
    id: "pisa-corso",
    name: "Corso Italia",
    city: "Пиза",
    walkDay: 6,
    walkOrder: 14,
    lnglat: [10.402, 43.71],
    duration: "40 мин",
  },
];

const chioggiaDayEightSights: StoredSight[] = [
  {
    id: "chioggia-ponte-vigo",
    name: "Ponte Vigo",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 0,
    lnglat: [12.2783, 45.2197],
    duration: "20 мин",
  },
  {
    id: "chioggia-mark",
    name: "Колонна Святого Марка",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 1,
    lnglat: [12.2783, 45.2197],
    duration: "15 мин",
  },
  {
    id: "chioggia-vena",
    name: "Канал Vena",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 2,
    lnglat: [12.279, 45.219],
    duration: "30 мин",
  },
  {
    id: "chioggia-corso",
    name: "Corso del Popolo",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 3,
    lnglat: [12.279, 45.218],
    duration: "35 мин",
  },
  {
    id: "chioggia-cathedral",
    name: "Кафедральный собор Santa Maria Assunta",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 4,
    lnglat: [12.2792, 45.2185],
    duration: "25 мин",
  },
  {
    id: "chioggia-clock",
    name: "Torre dell'Orologio",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 5,
    lnglat: [12.2789, 45.2184],
    duration: "15 мин",
  },
  {
    id: "chioggia-palazzo",
    name: "Palazzo Comunale",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 6,
    lnglat: [12.279, 45.2182],
    duration: "15 мин",
  },
  {
    id: "chioggia-andrea",
    name: "Церковь Sant'Andrea",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 7,
    lnglat: [12.2802, 45.219],
    duration: "20 мин",
  },
  {
    id: "chioggia-garibaldi",
    name: "Porta Garibaldi",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 8,
    lnglat: [12.2808, 45.2167],
    duration: "15 мин",
  },
  {
    id: "chioggia-bridges",
    name: "Мостики через канал Vena",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 9,
    lnglat: [12.2794, 45.2192],
    duration: "30 мин",
  },
  {
    id: "chioggia-boats",
    name: "Рыбацкие домики и пришвартованные лодки",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 10,
    lnglat: [12.2785, 45.2203],
    duration: "25 мин",
  },
  {
    id: "chioggia-lagoon",
    name: "Набережная лагуны",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 11,
    lnglat: [12.279, 45.221],
    duration: "30 мин",
  },
  {
    id: "chioggia-port",
    name: "Порт Кьоджи",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 12,
    lnglat: [12.284, 45.221],
    duration: "25 мин",
  },
  {
    id: "chioggia-diga",
    name: "Прогулка по дамбе Diga Sottomarina",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 13,
    lnglat: [12.306, 45.211],
    duration: "45 мин",
  },
  {
    id: "chioggia-beach",
    name: "Пляж Sottomarina",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 14,
    lnglat: [12.3, 45.208],
    duration: "30 мин",
  },
  {
    id: "chioggia-view",
    name: "Панорамные виды на лагуну",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 15,
    lnglat: [12.302, 45.211],
    duration: "20 мин",
  },
  {
    id: "chioggia-tree",
    name: "Главная рождественская елка города",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 16,
    lnglat: [12.279, 45.218],
    duration: "15 мин",
  },
  {
    id: "chioggia-lights",
    name: "Рождественская иллюминация Corso del Popolo",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 17,
    lnglat: [12.279, 45.218],
    duration: "20 мин",
  },
  {
    id: "chioggia-market",
    name: "Рождественские ярмарочные домики",
    city: "Кьоджа",
    walkDay: 8,
    walkOrder: 18,
    lnglat: [12.2787, 45.2187],
    duration: "20 мин",
  },
];

const veniceDayNineSights: StoredSight[] = [
  {
    id: "venice-scalzi",
    name: "Мост Скальци",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 0,
    lnglat: [12.3212, 45.4411],
    duration: "15 мин",
  },
  {
    id: "venice-grand-canal-walk",
    name: "Прогулка вдоль Гранд-канала",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 1,
    lnglat: [12.326, 45.4385],
    duration: "30 мин",
  },
  {
    id: "venice-vaporetto",
    name: "Вапоретто по Гранд-каналу",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 2,
    lnglat: [12.327, 45.439],
    duration: "45 мин",
  },
  {
    id: "venice-rialto",
    name: "Мост Риальто",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 3,
    lnglat: [12.3358, 45.438],
    duration: "25 мин",
  },
  {
    id: "venice-market",
    name: "Рынок Риальто",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 4,
    lnglat: [12.3339, 45.4392],
    duration: "30 мин",
  },
  {
    id: "venice-san-polo",
    name: "Улочки района Сан-Поло",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 5,
    lnglat: [12.331, 45.437],
    duration: "35 мин",
  },
  {
    id: "venice-frari",
    name: "Базилика Санта-Мария-Глориоза-деи-Фрари",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 6,
    lnglat: [12.3275, 45.437],
    duration: "30 мин",
  },
  {
    id: "venice-acqua-alta",
    name: "Libreria Acqua Alta",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 7,
    lnglat: [12.3427, 45.4389],
    duration: "25 мин",
  },
  {
    id: "venice-sighs",
    name: "Мост Вздохов",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 8,
    lnglat: [12.3416, 45.4341],
    duration: "15 мин",
  },
  {
    id: "venice-san-marco",
    name: "Площадь Сан-Марко",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 9,
    lnglat: [12.338, 45.434],
    duration: "30 мин",
  },
  {
    id: "venice-basilica",
    name: "Собор Святого Марка",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 10,
    lnglat: [12.3398, 45.4345],
    duration: "30 мин",
  },
  {
    id: "venice-clock",
    name: "Часовая башня Святого Марка",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 11,
    lnglat: [12.3386, 45.4344],
    duration: "15 мин",
  },
  {
    id: "venice-doge",
    name: "Дворец дожей",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 12,
    lnglat: [12.3404, 45.4337],
    duration: "1 ч",
  },
  {
    id: "venice-riva",
    name: "Набережная Riva degli Schiavoni",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 13,
    lnglat: [12.343, 45.433],
    duration: "30 мин",
  },
  {
    id: "venice-dogana",
    name: "Punta della Dogana",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 14,
    lnglat: [12.3349, 45.4294],
    duration: "25 мин",
  },
  {
    id: "venice-salute",
    name: "Базилика Santa Maria della Salute",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 15,
    lnglat: [12.336, 45.4306],
    duration: "30 мин",
  },
  {
    id: "venice-academy",
    name: "Мост Академии",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 16,
    lnglat: [12.3285, 45.4318],
    duration: "20 мин",
  },
  {
    id: "venice-academy-view",
    name: "Смотровая площадка у моста Академии",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 17,
    lnglat: [12.3285, 45.4318],
    duration: "20 мин",
  },
  {
    id: "venice-tree",
    name: "Главная рождественская елка на Piazza San Marco",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 18,
    lnglat: [12.338, 45.434],
    duration: "15 мин",
  },
  {
    id: "venice-lights",
    name: "Рождественская подсветка площади Сан-Марко",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 19,
    lnglat: [12.338, 45.434],
    duration: "20 мин",
  },
  {
    id: "venice-fairs",
    name: "Рождественские ярмарки Campo Santo Stefano и Campo San Polo",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 20,
    lnglat: [12.3305, 45.435],
    duration: "40 мин",
  },
  {
    id: "venice-mercerie",
    name: "Бутики и праздничные витрины на Mercerie",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 21,
    lnglat: [12.3388, 45.4355],
    duration: "30 мин",
  },
  {
    id: "venice-evening",
    name: "Вечерняя прогулка по освещенным каналам",
    city: "Венеция",
    walkDay: 9,
    walkOrder: 22,
    lnglat: [12.334, 45.436],
    duration: "45 мин",
  },
];

const milanDayTenSights: StoredSight[] = [
  {
    id: "milan-centrale",
    name: "Milano Centrale",
    city: "Милан",
    walkDay: 10,
    walkOrder: 0,
    lnglat: [9.2044, 45.4859],
    duration: "30 мин",
  },
  {
    id: "milan-centrale-hall",
    name: "Главный зал и фасад вокзала",
    city: "Милан",
    walkDay: 10,
    walkOrder: 1,
    lnglat: [9.2044, 45.4859],
    duration: "20 мин",
  },
  {
    id: "milan-buenos-aires",
    name: "Corso Buenos Aires",
    city: "Милан",
    walkDay: 10,
    walkOrder: 2,
    lnglat: [9.214, 45.478],
    duration: "40 мин",
  },
  {
    id: "milan-duomo",
    name: "Миланский собор (Duomo di Milano)",
    city: "Милан",
    walkDay: 10,
    walkOrder: 3,
    lnglat: [9.19, 45.4641],
    duration: "1 ч",
  },
  {
    id: "milan-duomo-square",
    name: "Piazza del Duomo",
    city: "Милан",
    walkDay: 10,
    walkOrder: 4,
    lnglat: [9.19, 45.4641],
    duration: "30 мин",
  },
  {
    id: "milan-tree",
    name: "Главная рождественская елка Милана",
    city: "Милан",
    walkDay: 10,
    walkOrder: 5,
    lnglat: [9.19, 45.4641],
    duration: "15 мин",
  },
  {
    id: "milan-lights",
    name: "Рождественская иллюминация площади",
    city: "Милан",
    walkDay: 10,
    walkOrder: 6,
    lnglat: [9.19, 45.4641],
    duration: "20 мин",
  },
  {
    id: "milan-galleria",
    name: "Galleria Vittorio Emanuele II",
    city: "Милан",
    walkDay: 10,
    walkOrder: 7,
    lnglat: [9.1919, 45.4658],
    duration: "30 мин",
  },
  {
    id: "milan-coffee",
    name: "Историческое кафе в галерее",
    city: "Милан",
    walkDay: 10,
    walkOrder: 8,
    lnglat: [9.1919, 45.4658],
    duration: "30 мин",
  },
  {
    id: "milan-scala-square",
    name: "Piazza della Scala",
    city: "Милан",
    walkDay: 10,
    walkOrder: 9,
    lnglat: [9.1897, 45.4663],
    duration: "15 мин",
  },
  {
    id: "milan-scala",
    name: "Театр Ла Скала",
    city: "Милан",
    walkDay: 10,
    walkOrder: 10,
    lnglat: [9.1899, 45.4662],
    duration: "20 мин",
  },
  {
    id: "milan-mercanti",
    name: "Via Mercanti",
    city: "Милан",
    walkDay: 10,
    walkOrder: 11,
    lnglat: [9.1883, 45.464],
    duration: "20 мин",
  },
  {
    id: "milan-sforza",
    name: "Замок Сфорца",
    city: "Милан",
    walkDay: 10,
    walkOrder: 12,
    lnglat: [9.1797, 45.4705],
    duration: "45 мин",
  },
  {
    id: "milan-sempione",
    name: "Парк Семпионе",
    city: "Милан",
    walkDay: 10,
    walkOrder: 13,
    lnglat: [9.176, 45.4725],
    duration: "40 мин",
  },
  {
    id: "milan-pace",
    name: "Арка Мира (Arco della Pace)",
    city: "Милан",
    walkDay: 10,
    walkOrder: 14,
    lnglat: [9.1728, 45.476],
    duration: "20 мин",
  },
  {
    id: "milan-dante",
    name: "Via Dante",
    city: "Милан",
    walkDay: 10,
    walkOrder: 15,
    lnglat: [9.184, 45.467],
    duration: "25 мин",
  },
  {
    id: "milan-market",
    name: "Рождественские ярмарки и праздничные лавки",
    city: "Милан",
    walkDay: 10,
    walkOrder: 16,
    lnglat: [9.188, 45.465],
    duration: "30 мин",
  },
  {
    id: "milan-monte",
    name: "Via Monte Napoleone",
    city: "Милан",
    walkDay: 10,
    walkOrder: 17,
    lnglat: [9.195, 45.469],
    duration: "30 мин",
  },
  {
    id: "milan-quadrilatero",
    name: "Quadrilatero della Moda",
    city: "Милан",
    walkDay: 10,
    walkOrder: 18,
    lnglat: [9.196, 45.469],
    duration: "30 мин",
  },
];

const ravensburgDayElevenSights: StoredSight[] = [
  {
    id: "ravens-marienplatz",
    name: "Marienplatz",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 0,
    lnglat: [9.6119, 47.782],
    duration: "20 мин",
  },
  {
    id: "ravens-market",
    name: "Рождественская ярмарка Ravensburger Christkindlesmarkt",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 1,
    lnglat: [9.6119, 47.782],
    duration: "1 ч",
  },
  {
    id: "ravens-mehlsack",
    name: "Mehlsack",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 2,
    lnglat: [9.6112, 47.7833],
    duration: "20 мин",
  },
  {
    id: "ravens-veitsburg",
    name: "Veitsburg",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 3,
    lnglat: [9.6146, 47.787],
    duration: "45 мин",
  },
  {
    id: "ravens-liebfrauen",
    name: "Liebfrauenkirche",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 4,
    lnglat: [9.6124, 47.783],
    duration: "20 мин",
  },
  {
    id: "ravens-stadtkirche",
    name: "Evangelische Stadtkirche",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 5,
    lnglat: [9.6121, 47.7818],
    duration: "20 мин",
  },
  {
    id: "ravens-old-town",
    name: "Исторический Старый город",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 6,
    lnglat: [9.612, 47.782],
    duration: "40 мин",
  },
  {
    id: "ravens-marktstrasse",
    name: "Marktstraße",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 7,
    lnglat: [9.612, 47.7822],
    duration: "25 мин",
  },
  {
    id: "ravens-kirchstrasse",
    name: "Kirchstraße",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 8,
    lnglat: [9.6106, 47.7822],
    duration: "20 мин",
  },
  {
    id: "ravens-store",
    name: "Музей и фирменный магазин Ravensburger",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 9,
    lnglat: [9.6134, 47.7813],
    duration: "40 мин",
  },
  {
    id: "ravens-obertor",
    name: "Obertor",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 10,
    lnglat: [9.611, 47.7801],
    duration: "15 мин",
  },
  {
    id: "ravens-frauentor",
    name: "Frauentor",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 11,
    lnglat: [9.6142, 47.7823],
    duration: "15 мин",
  },
  {
    id: "ravens-gruner",
    name: "Grüner Turm",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 12,
    lnglat: [9.6098, 47.7828],
    duration: "15 мин",
  },
  {
    id: "ravens-waaghaus",
    name: "Waaghaus",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 13,
    lnglat: [9.6115, 47.7818],
    duration: "15 мин",
  },
  {
    id: "ravens-park",
    name: "Veitsburg Park",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 14,
    lnglat: [9.6142, 47.7863],
    duration: "30 мин",
  },
  {
    id: "ravens-view",
    name: "Смотровая площадка Veitsburg",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 15,
    lnglat: [9.6146, 47.787],
    duration: "20 мин",
  },
  {
    id: "ravens-houses",
    name: "Средневековые фахверковые дома Равенсбурга",
    city: "Равенсбург",
    walkDay: 11,
    walkOrder: 16,
    lnglat: [9.612, 47.782],
    duration: "25 мин",
  },
];

const pragueDayTwelveSights: StoredSight[] = [
  {
    id: "prague-square",
    name: "Староместская площадь",
    city: "Прага",
    walkDay: 12,
    walkOrder: 0,
    lnglat: [14.4208, 50.087],
    duration: "30 мин",
  },
  {
    id: "prague-market",
    name: "Главная рождественская ярмарка",
    city: "Прага",
    walkDay: 12,
    walkOrder: 1,
    lnglat: [14.4208, 50.087],
    duration: "1 ч",
  },
  {
    id: "prague-tree",
    name: "Главная рождественская елка Праги",
    city: "Прага",
    walkDay: 12,
    walkOrder: 2,
    lnglat: [14.4208, 50.087],
    duration: "15 мин",
  },
  {
    id: "prague-tyn",
    name: "Храм Девы Марии перед Тыном",
    city: "Прага",
    walkDay: 12,
    walkOrder: 3,
    lnglat: [14.4224, 50.0872],
    duration: "25 мин",
  },
  {
    id: "prague-orloj",
    name: "Пражские куранты (Орлой)",
    city: "Прага",
    walkDay: 12,
    walkOrder: 4,
    lnglat: [14.4206, 50.0869],
    duration: "20 мин",
  },
  {
    id: "prague-nicholas",
    name: "Костел Святого Николая",
    city: "Прага",
    walkDay: 12,
    walkOrder: 5,
    lnglat: [14.4039, 50.088],
    duration: "25 мин",
  },
  {
    id: "prague-hall",
    name: "Староместская ратуша",
    city: "Прага",
    walkDay: 12,
    walkOrder: 6,
    lnglat: [14.4206, 50.0869],
    duration: "30 мин",
  },
  {
    id: "prague-karlova",
    name: "Карлова улица",
    city: "Прага",
    walkDay: 12,
    walkOrder: 7,
    lnglat: [14.4168, 50.086],
    duration: "25 мин",
  },
  {
    id: "prague-bridge",
    name: "Карлов мост",
    city: "Прага",
    walkDay: 12,
    walkOrder: 8,
    lnglat: [14.4114, 50.0865],
    duration: "30 мин",
  },
  {
    id: "prague-statues",
    name: "Статуи Карлова моста",
    city: "Прага",
    walkDay: 12,
    walkOrder: 9,
    lnglat: [14.4108, 50.0865],
    duration: "20 мин",
  },
  {
    id: "prague-kampa",
    name: "Остров Кампа",
    city: "Прага",
    walkDay: 12,
    walkOrder: 10,
    lnglat: [14.407, 50.0845],
    duration: "30 мин",
  },
  {
    id: "prague-lennon",
    name: "Стена Джона Леннона",
    city: "Прага",
    walkDay: 12,
    walkOrder: 11,
    lnglat: [14.4044, 50.0854],
    duration: "20 мин",
  },
  {
    id: "prague-evening",
    name: "Вечерняя прогулка по освещенному Карлову мосту",
    city: "Прага",
    walkDay: 12,
    walkOrder: 12,
    lnglat: [14.4114, 50.0865],
    duration: "30 мин",
  },
];

const pragueDayFourteenSights: StoredSight[] = [
  {
    id: "prague-wenceslas",
    name: "Вацлавская площадь",
    city: "Прага",
    walkDay: 14,
    walkOrder: 0,
    lnglat: [14.4281, 50.081],
    duration: "30 мин",
  },
  {
    id: "prague-wenceslas-market",
    name: "Рождественская ярмарка на Вацлавской площади",
    city: "Прага",
    walkDay: 14,
    walkOrder: 1,
    lnglat: [14.4281, 50.081],
    duration: "1 ч",
  },
  {
    id: "prague-prikope",
    name: "Торговая улица Na Příkopě",
    city: "Прага",
    walkDay: 14,
    walkOrder: 2,
    lnglat: [14.427, 50.085],
    duration: "30 мин",
  },
  {
    id: "prague-palladium",
    name: "Торговый центр Palladium",
    city: "Прага",
    walkDay: 14,
    walkOrder: 3,
    lnglat: [14.4282, 50.089],
    duration: "30 мин",
  },
  {
    id: "prague-republic-market",
    name: "Рождественский рынок на площади Республики",
    city: "Прага",
    walkDay: 14,
    walkOrder: 4,
    lnglat: [14.4288, 50.089],
    duration: "30 мин",
  },
  {
    id: "prague-powder",
    name: "Пороховая башня",
    city: "Прага",
    walkDay: 14,
    walkOrder: 5,
    lnglat: [14.4275, 50.0875],
    duration: "20 мин",
  },
  {
    id: "prague-municipal",
    name: "Общественный дом",
    city: "Прага",
    walkDay: 14,
    walkOrder: 6,
    lnglat: [14.4292, 50.0878],
    duration: "20 мин",
  },
  {
    id: "prague-letna",
    name: "Парк Летна",
    city: "Прага",
    walkDay: 14,
    walkOrder: 7,
    lnglat: [14.424, 50.096],
    duration: "30 мин",
  },
  {
    id: "prague-metronome",
    name: "Летенский метроном",
    city: "Прага",
    walkDay: 14,
    walkOrder: 8,
    lnglat: [14.424, 50.0965],
    duration: "20 мин",
  },
  {
    id: "prague-view",
    name: "Панорамный вид на Прагу",
    city: "Прага",
    walkDay: 14,
    walkOrder: 9,
    lnglat: [14.4237, 50.0962],
    duration: "20 мин",
  },
  {
    id: "prague-cech",
    name: "Чехов мост",
    city: "Прага",
    walkDay: 14,
    walkOrder: 10,
    lnglat: [14.417, 50.093],
    duration: "20 мин",
  },
  {
    id: "prague-vltava",
    name: "Вечерняя прогулка по набережной Влтавы",
    city: "Прага",
    walkDay: 14,
    walkOrder: 11,
    lnglat: [14.417, 50.091],
    duration: "30 мин",
  },
  {
    id: "prague-final",
    name: "Финальная прогулка по Старому городу в рождественской подсветке",
    city: "Прага",
    walkDay: 14,
    walkOrder: 12,
    lnglat: [14.421, 50.087],
    duration: "40 мин",
  },
];

const pragueDayThirteenSights: StoredSight[] = [
  {
    id: "prague-castle",
    name: "Пражский Град",
    city: "Прага",
    walkDay: 13,
    walkOrder: 0,
    lnglat: [14.4, 50.091],
    duration: "1 ч",
  },
  {
    id: "prague-vit",
    name: "Собор Святого Вита",
    city: "Прага",
    walkDay: 13,
    walkOrder: 1,
    lnglat: [14.4, 50.0909],
    duration: "40 мин",
  },
  {
    id: "prague-palace",
    name: "Старый королевский дворец",
    city: "Прага",
    walkDay: 13,
    walkOrder: 2,
    lnglat: [14.3998, 50.0913],
    duration: "30 мин",
  },
  {
    id: "prague-george",
    name: "Базилика Святого Георгия",
    city: "Прага",
    walkDay: 13,
    walkOrder: 3,
    lnglat: [14.403, 50.0912],
    duration: "25 мин",
  },
  {
    id: "prague-golden",
    name: "Золотая улочка",
    city: "Прага",
    walkDay: 13,
    walkOrder: 4,
    lnglat: [14.405, 50.091],
    duration: "30 мин",
  },
  {
    id: "prague-castle-view",
    name: "Смотровая площадка у Пражского Града",
    city: "Прага",
    walkDay: 13,
    walkOrder: 5,
    lnglat: [14.3975, 50.0916],
    duration: "20 мин",
  },
  {
    id: "prague-hrad",
    name: "Градчанская площадь",
    city: "Прага",
    walkDay: 13,
    walkOrder: 6,
    lnglat: [14.3965, 50.0895],
    duration: "20 мин",
  },
  {
    id: "prague-stairs",
    name: "Новая Замковая лестница",
    city: "Прага",
    walkDay: 13,
    walkOrder: 7,
    lnglat: [14.4025, 50.089],
    duration: "20 мин",
  },
  {
    id: "prague-nicholas-mala",
    name: "Церковь Святого Николая (Мала Страна)",
    city: "Прага",
    walkDay: 13,
    walkOrder: 8,
    lnglat: [14.404, 50.088],
    duration: "25 мин",
  },
  {
    id: "prague-vltava-bank",
    name: "Набережная Влтавы",
    city: "Прага",
    walkDay: 13,
    walkOrder: 9,
    lnglat: [14.407, 50.086],
    duration: "30 мин",
  },
  {
    id: "prague-miru-market",
    name: "Рождественская ярмарка на площади Мира или у Пражского Града",
    city: "Прага",
    walkDay: 13,
    walkOrder: 10,
    lnglat: [14.437, 50.075],
    duration: "45 мин",
  },
];

function compressCoverPhoto(file: File) {
  return new Promise<string>((resolve, reject) => {
    const source = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      const scale = Math.min(1, 900 / Math.max(image.width, image.height));
      const canvas = document.createElement("canvas");
      canvas.width = Math.round(image.width * scale);
      canvas.height = Math.round(image.height * scale);
      canvas
        .getContext("2d")
        ?.drawImage(image, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(source);
      resolve(canvas.toDataURL("image/jpeg", 0.68));
    };
    image.onerror = () => {
      URL.revokeObjectURL(source);
      reject(new Error("Image decoding failed"));
    };
    image.src = source;
  });
}

function TripMap({
  city,
  places = emptyPlaces,
  routeDays = emptyRouteDays,
  activeDay,
}: {
  city?: string;
  places?: string[];
  routeDays?: DraftDay[];
  activeDay?: number;
}) {
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const markerElements = useRef<HTMLSpanElement[]>([]);
  const location = city ? mapLocation(city) : undefined;
  const displayedRouteDays = routeDays;
  const routeKey = displayedRouteDays
    .map((day) =>
      [
        day.id,
        day.roadLeg?.from,
        day.roadLeg?.to,
        day.roadLeg?.mapsUrl,
      ].join(":"),
    )
    .join("|");
  const locationKey = location?.join(",") || "";
  const placesKey = places.join("|");

  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    if (!container.current || !token) return;
    let disposed = false;
    let map: Map | undefined;
    let resizeObserver: ResizeObserver | undefined;

    void import("mapbox-gl").then(({ default: mapboxgl }) => {
      if (disposed || !container.current) return;
      mapboxgl.accessToken = token;
      const routeCoordinates = routeCoordinatesFor(displayedRouteDays);
      map = new mapboxgl.Map({
        container: container.current,
        style: mapStyle(),
        center: routeCoordinates[0] ?? location ?? mapLocations["Москва"],
        zoom: routeCoordinates.length ? 5 : location ? 12 : 3,
        attributionControl: false,
      });
      mapRef.current = map;
      resizeObserver = new ResizeObserver(() => map?.resize());
      resizeObserver.observe(container.current);
      map.addControl(
        new mapboxgl.NavigationControl({ showCompass: false }),
        "top-right",
      );

      if (routeCoordinates.length > 1) {
        markerElements.current = [];
        routeCoordinates.forEach((coordinate, index) => {
          const element = document.createElement("span");
          element.className =
            index === activeDay ? "map-marker active" : "map-marker";
          element.textContent = String(index + 1);
          markerElements.current.push(element);
          new mapboxgl.Marker({ element }).setLngLat(coordinate).addTo(map!);
        });
        map.on("load", () => {
          map!.addSource("route", {
            type: "geojson",
            data: {
              type: "Feature",
              properties: {},
              geometry: { type: "LineString", coordinates: routeCoordinates },
            },
          });
          map!.addLayer({
            id: "route",
            type: "line",
            source: "route",
            paint: {
              "line-color": "#4c46d6",
              "line-width": 3,
              "line-opacity": 0.72,
            },
          });
          const activeSegment = routeSegment(routeCoordinates, activeDay || 0);
          if (activeSegment.length > 1) {
            map!.addSource("active-route", {
              type: "geojson",
              data: {
                type: "Feature",
                properties: {},
                geometry: { type: "LineString", coordinates: activeSegment },
              },
            });
            map!.addLayer({
              id: "active-route",
              type: "line",
              source: "active-route",
              paint: {
                "line-color": "#ff7a45",
                "line-width": 6,
                "line-opacity": 0.95,
              },
            });
          }
          const bounds = new mapboxgl.LngLatBounds(
            routeCoordinates[0],
            routeCoordinates[0],
          );
          routeCoordinates
            .slice(1)
            .forEach((coordinate) => bounds.extend(coordinate));
          map!.fitBounds(bounds, { padding: 42, maxZoom: 8 });
        });
      } else if (location && places.length) {
        const coordinates = places.map(
          (_, index) =>
            [
              location[0] + (index - 2) * 0.012,
              location[1] + (index % 2 ? 1 : -1) * (index + 1) * 0.006,
            ] as [number, number],
        );
        coordinates.forEach((coordinate, index) => {
          const element = document.createElement("span");
          element.className = "map-marker";
          element.textContent = String(index + 1);
          new mapboxgl.Marker({ element }).setLngLat(coordinate).addTo(map!);
        });
        map.on("load", () => {
          map!.addSource("route", {
            type: "geojson",
            data: {
              type: "Feature",
              properties: {},
              geometry: { type: "LineString", coordinates },
            },
          });
          map!.addLayer({
            id: "route",
            type: "line",
            source: "route",
            paint: {
              "line-color": "#4c46d6",
              "line-width": 3,
              "line-opacity": 0.72,
            },
          });
        });
      }
    });

    return () => {
      disposed = true;
      resizeObserver?.disconnect();
      map?.remove();
      mapRef.current = null;
      markerElements.current = [];
    };
  }, [city, locationKey, placesKey, routeKey]);

  useEffect(() => {
    const coordinate =
      activeDay === undefined
        ? undefined
        : routeCoordinatesFor(displayedRouteDays)[activeDay];
    if (!coordinate || !mapRef.current) return;
    markerElements.current.forEach((element, index) =>
      element.classList.toggle("active", index === activeDay),
    );
    mapRef.current.flyTo({
      center: coordinate,
      zoom: 8,
      duration: 900,
      essential: true,
    });
    const source = mapRef.current.getSource("active-route") as
      { setData: (data: object) => void } | undefined;
    const activeSegment = routeSegment(
      routeCoordinatesFor(displayedRouteDays),
      activeDay ?? 0,
    );
    if (source && activeSegment.length > 1)
      source.setData({
        type: "Feature",
        properties: {},
        geometry: { type: "LineString", coordinates: activeSegment },
      });
  }, [activeDay, routeKey]);

  if (!import.meta.env.VITE_MAPBOX_ACCESS_TOKEN) {
    return (
      <div className="map map-unavailable">
        Карта станет доступна после настройки Mapbox.
      </div>
    );
  }

  return (
    <div
      ref={container}
      className="map"
      aria-label={city ? `Карта ${city}` : "Карта путешествия"}
    />
  );
}

function Avatar({
  children,
  tone = "sand",
}: {
  children: ReactNode;
  tone?: "sand" | "green" | "blue";
}) {
  return <span className={`avatar ${tone}`}>{children}</span>;
}

function DatePicker({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  const selected = value ? new Date(`${value}T12:00:00`) : null;
  const [open, setOpen] = useState(false);
  const [month, setMonth] = useState(() =>
    selected
      ? new Date(selected.getFullYear(), selected.getMonth(), 1)
      : new Date(),
  );
  const weekdayLabels = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"];
  const monthStart = (month.getDay() + 6) % 7;
  const daysInMonth = new Date(
    month.getFullYear(),
    month.getMonth() + 1,
    0,
  ).getDate();
  const formatted = selected
    ? new Intl.DateTimeFormat("ru-RU", {
        day: "numeric",
        month: "long",
        year: "numeric",
      }).format(selected)
    : "Выберите дату";
  const chooseDay = (day: number) => {
    const next = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    onChange(next);
    setOpen(false);
  };
  return (
    <label className="date-field">
      {label}
      <div className="date-picker">
        <button
          type="button"
          className="date-trigger"
          onClick={() => setOpen(!open)}
        >
          <span className={value ? "" : "placeholder"}>{formatted}</span>
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <rect x="4" y="5" width="16" height="15" rx="2" />
            <path d="M8 3v4m8-4v4M4 10h16" />
          </svg>
        </button>
        {open && (
          <div className="calendar-popover">
            <div className="calendar-header">
              <button
                type="button"
                onClick={() =>
                  setMonth(
                    new Date(month.getFullYear(), month.getMonth() - 1, 1),
                  )
                }
              >
                ‹
              </button>
              <b>
                {new Intl.DateTimeFormat("ru-RU", {
                  month: "long",
                  year: "numeric",
                }).format(month)}
              </b>
              <button
                type="button"
                onClick={() =>
                  setMonth(
                    new Date(month.getFullYear(), month.getMonth() + 1, 1),
                  )
                }
              >
                ›
              </button>
            </div>
            <div className="calendar-grid calendar-weekdays">
              {weekdayLabels.map((day) => (
                <span key={day}>{day}</span>
              ))}
            </div>
            <div className="calendar-grid">
              {Array.from({ length: monthStart }, (_, index) => (
                <span key={`empty-${index}`} />
              ))}
              {Array.from({ length: daysInMonth }, (_, index) => {
                const day = index + 1;
                const isSelected =
                  selected?.getFullYear() === month.getFullYear() &&
                  selected?.getMonth() === month.getMonth() &&
                  selected?.getDate() === day;
                return (
                  <button
                    type="button"
                    className={isSelected ? "selected" : ""}
                    onClick={() => chooseDay(day)}
                    key={day}
                  >
                    {day}
                  </button>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </label>
  );
}

function Sidebar({
  view,
  go,
  open,
  close,
  profileName,
}: {
  view: View;
  go: (view: View) => void;
  open: boolean;
  close: () => void;
  profileName: string;
}) {
  const [settings, setSettings] = useState(false);
  const [panel, setPanel] = useState<"photo" | "password" | null>(null);
  const closeSettings = () => {
    setSettings(false);
    setPanel(null);
  };
  const closeSidebar = () => {
    closeSettings();
    close();
  };
  return (
    <>
      <button
        className={`scrim ${open ? "show" : ""}`}
        onClick={closeSidebar}
        aria-label="Закрыть меню"
      />
      <aside className={`sidebar ${open ? "open" : ""}`}>
        <div className="brand">
          <span>R</span>
          <b>Ramingo</b>
          <button onClick={closeSidebar}>×</button>
        </div>
        <button className="primary" onClick={() => go("create")}>
          <span>＋</span> Новое путешествие
        </button>
        <p className="nav-label">Навигация</p>
        <nav>
          <button
            className={
              view === "trips" || view === "trip" || view === "create"
                ? "active"
                : ""
            }
            onClick={() => go("trips")}
          >
            <i>◇</i>Мои путешествия
          </button>
          <button
            className={view === "catalog" || view === "public" ? "active" : ""}
            onClick={() => go("catalog")}
          >
            <i>✦</i>Каталог маршрутов
          </button>
        </nav>
        <div className="account-wrap">
          {settings && (
            <div className="settings-popover">
              <button
                className="settings-close"
                onClick={closeSettings}
                aria-label="Закрыть настройки"
              >
                ×
              </button>
              <b>Язык интерфейса</b>
              <div className="language-switch">
                <button className="active">RU</button>
                <button>EN</button>
                <button>ES</button>
                <button>DE</button>
              </div>
              <button
                onClick={() => setPanel(panel === "photo" ? null : "photo")}
              >
                ▦ Сменить фото профиля
              </button>
              {panel === "photo" && (
                <div className="settings-panel">
                  <div className="mini-upload">
                    ↑<small>Перетащите фото</small>
                  </div>
                  <button className="accent">Сохранить фото</button>
                </div>
              )}
              <button
                onClick={() =>
                  setPanel(panel === "password" ? null : "password")
                }
              >
                ⚿ Сменить пароль
              </button>
              {panel === "password" && (
                <div className="settings-panel">
                  <input type="password" placeholder="Текущий пароль" />
                  <input type="password" placeholder="Новый пароль" />
                  <button className="accent">Обновить пароль</button>
                </div>
              )}
              <hr />
              <button>◷ Часовой пояс и валюта</button>
              <button>◔ Уведомления</button>
              <button
                className="logout"
                onClick={async () => {
                  await supabase.auth.signOut();
                  setSettings(false);
                  close();
                  go("auth");
                }}
              >
                → Выйти
              </button>
            </div>
          )}
          <button
            className="account"
            onClick={() => {
              setSettings(!settings);
              if (settings) setPanel(null);
            }}
          >
            <Avatar>
              {profileName
                .split(" ")
                .map((part) => part[0])
                .join("")
                .slice(0, 2)
                .toUpperCase()}
            </Avatar>
            <span>
              <b>{profileName}</b>
              <small>Личный кабинет · RU</small>
            </span>
            <i>⚙</i>
          </button>
        </div>
      </aside>
    </>
  );
}

function Trips({
  go,
  profileName,
  drafts,
  onOpenTrip,
  onUpdateTrip,
}: {
  go: (view: View) => void;
  profileName: string;
  drafts: TripSummary[];
  onOpenTrip: (trip: TripSummary) => void;
  onUpdateTrip: (trip: TripSummary) => void;
}) {
  const [filter, setFilter] = useState("all");
  const [cardMenuTripId, setCardMenuTripId] = useState<string | null>(null);
  const [editingTrip, setEditingTrip] = useState<TripSummary | null>(null);
  const allTrips = drafts;
  const filters = [
    ["all", `Все · ${allTrips.length}`],
    ["upcoming", "Предстоящие"],
    ["draft", "Черновики"],
    ["completed", "Завершённые"],
  ];
  const statusByFilter: Record<string, string> = {
    upcoming: "Предстоящее",
    draft: "Черновик",
    completed: "Завершённое",
  };
  const filteredTrips =
    filter === "all"
      ? allTrips
      : allTrips.filter((trip) => trip.status === statusByFilter[filter]);
  return (
    <div className="page wide">
      <header className="page-title">
        <div>
          <p>Добро пожаловать, {profileName.split(" ")[0]}</p>
          <h1>Мои путешествия</h1>
        </div>
        <button className="secondary" onClick={() => go("create")}>
          ＋ Создать
        </button>
      </header>
      <div className="chips">
        {filters.map(([value, label]) => (
          <button
            className={filter === value ? "selected" : ""}
            onClick={() => setFilter(value)}
            key={value}
          >
            {label}
          </button>
        ))}
      </div>
      <div className="trip-grid">
        {filteredTrips.map((trip) => (
          <article
            className="trip-card"
            key={trip.title}
            onClick={() => onOpenTrip(trip)}
          >
            <div
              className={`cover ${trip.tone} ${trip.coverImage ? "has-image" : ""}`}
              style={
                trip.coverImage
                  ? { backgroundImage: `url(${trip.coverImage})` }
                  : undefined
              }
            >
              <div
                className="trip-card-actions"
                onClick={(event) => event.stopPropagation()}
              >
                <button
                  type="button"
                  className="trip-card-menu-trigger"
                  onClick={() =>
                    setCardMenuTripId((id) => (id === trip.id ? null : trip.id))
                  }
                  aria-label={`Настройки: ${trip.title}`}
                  aria-expanded={cardMenuTripId === trip.id}
                  aria-haspopup="menu"
                >
                  <i />
                  <i />
                  <i />
                </button>
                {cardMenuTripId === trip.id && (
                  <div className="trip-card-menu" role="menu">
                    <button
                      type="button"
                      role="menuitem"
                      onClick={() => {
                        setCardMenuTripId(null);
                        setEditingTrip(trip);
                      }}
                    >
                      <span>Редактировать</span>
                      <small>Название, даты и обложка</small>
                    </button>
                  </div>
                )}
              </div>
              <span className="status">● {trip.status}</span>
            </div>
            <div className="trip-info">
              <h2>{trip.title}</h2>
              <p>{trip.dates}</p>
              <div className="progress">
                <i style={{ width: `${trip.progress}%` }} />
              </div>
              <small>
                <span>Маршрут заполнен на {trip.progress}%</span>
                <span>{trip.cities}</span>
              </small>
            </div>
          </article>
        ))}
        {filteredTrips.length === 0 && (
          <div className="empty-state">
            В этой категории пока нет путешествий.
          </div>
        )}
        <button className="new-card" onClick={() => go("create")}>
          <i>＋</i>
          <b>Новое путешествие</b>
          <span>С нуля или из шаблона</span>
        </button>
      </div>
      {editingTrip && (
        <TripCardEditor
          trip={editingTrip}
          onUpdateTrip={onUpdateTrip}
          onClose={() => setEditingTrip(null)}
        />
      )}
    </div>
  );
}

function CreateTrip({
  go,
  onCreate,
}: {
  go: (view: View) => void;
  onCreate: (trip: TripSummary) => void;
}) {
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteName, setInviteName] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [invitees, setInvitees] = useState<{ name: string; email: string }[]>(
    [],
  );
  const [inviteMessage, setInviteMessage] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [coverImage, setCoverImage] = useState("");
  const photoInputRef = useRef<HTMLInputElement>(null);
  const addInvitee = async () => {
    const email = inviteEmail.trim().toLowerCase();
    const name = inviteName.trim() || email;
    if (!email || invitees.some((person) => person.email === email)) return;
    const {
      data: { session },
    } = await supabase.auth.getSession();
    if (!session) {
      setInviteMessage("Сессия истекла. Войдите в аккаунт ещё раз.");
      return;
    }
    const response = await fetch(
      `${import.meta.env.VITE_SUPABASE_URL}/functions/v1/dynamic-function`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${session.access_token}`,
          apikey: import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          name,
          redirectTo: `${window.location.origin}${import.meta.env.BASE_URL}?invite=trip`,
        }),
      },
    );
    if (!response.ok) {
      const payload = (await response.json().catch(() => null)) as {
        error?: string;
      } | null;
      const message = payload?.error || `Ошибка отправки (${response.status})`;
      setInviteMessage(`Не удалось отправить приглашение: ${message}`);
      return;
    }
    setInvitees([...invitees, { name, email }]);
    setInviteName("");
    setInviteEmail("");
    setInviteOpen(false);
    setInviteMessage(`Приглашение отправлено на ${email}`);
  };
  const selectCoverImage = (file?: File) => {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setCoverImage(String(reader.result));
    reader.readAsDataURL(file);
  };
  return (
    <div className="page form-page">
      <button
        className="back back-icon"
        onClick={() => go("trips")}
        aria-label="Вернуться к моим путешествиям"
        title="Мои путешествия"
      >
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path d="M19 12H5m0 0 6-6m-6 6 6 6" />
        </svg>
      </button>
      <h1>Новое путешествие</h1>
      <p className="lead">
        Заполните основное — детали маршрута добавите позже.
      </p>
      <form
        className="create-form"
        onSubmit={(event) => {
          event.preventDefault();
          const formData = new FormData(event.currentTarget);
          const title =
            String(formData.get("title") || "").trim() || "Без названия";
          const cities = String(formData.get("cities") || "").trim();
          onCreate({
            id: crypto.randomUUID(),
            title,
            cities,
            dates:
              startDate && endDate
                ? formatTripDates(startDate, endDate)
                : "Даты не выбраны · черновик",
            startDate,
            endDate,
            status: "Черновик",
            progress: 0,
            tone: "stone",
            isDraft: true,
            coverImage,
          });
        }}
      >
        <label>
          Название
          <input name="title" placeholder="Например, Италия" />
        </label>
        <div className="form-row">
          <label>
            Страна / направление
            <input name="destination" placeholder="Страна или направление" />
          </label>
          <label>
            Города
            <input name="cities" placeholder="Города маршрута" />
          </label>
        </div>
        <div className="form-row">
          <DatePicker
            label="Дата начала"
            value={startDate}
            onChange={setStartDate}
          />
          <DatePicker
            label="Дата окончания"
            value={endDate}
            onChange={setEndDate}
          />
        </div>
        <label>
          Участники
          <div className="people">
            {invitees.map((person) => (
              <span
                className="participant-chip"
                key={person.email}
                title={person.email}
              >
                <Avatar tone="blue">
                  {person.name
                    .split(" ")
                    .map((part) => part[0])
                    .join("")
                    .slice(0, 2)
                    .toUpperCase()}
                </Avatar>
                <b>{person.name}</b>
                <button
                  type="button"
                  className="remove-invite"
                  onClick={() =>
                    setInvitees(
                      invitees.filter((item) => item.email !== person.email),
                    )
                  }
                >
                  ×
                </button>
              </span>
            ))}
            {inviteOpen ? (
              <div className="invite-person">
                <input
                  type="text"
                  value={inviteName}
                  onChange={(event) => setInviteName(event.target.value)}
                  placeholder="Имя"
                  autoFocus
                />
                <input
                  type="email"
                  value={inviteEmail}
                  onChange={(event) => setInviteEmail(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      event.preventDefault();
                      addInvitee();
                    }
                  }}
                  placeholder="name@example.com"
                />
                <button type="button" onClick={addInvitee}>
                  Пригласить
                </button>
                <button
                  type="button"
                  className="cancel-invite"
                  onClick={() => {
                    setInviteOpen(false);
                    setInviteName("");
                    setInviteEmail("");
                  }}
                >
                  ×
                </button>
              </div>
            ) : (
              <button type="button" onClick={() => setInviteOpen(true)}>
                ＋ Пригласить по e-mail
              </button>
            )}
          </div>
          {inviteMessage && (
            <small className="invite-message">{inviteMessage}</small>
          )}
        </label>
        <label>
          Обложка
          <input
            ref={photoInputRef}
            className="cover-file-input"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => selectCoverImage(event.target.files?.[0])}
          />
          <button
            type="button"
            className={`upload ${coverImage ? "has-cover" : ""}`}
            style={
              coverImage
                ? {
                    backgroundImage: `linear-gradient(rgba(27, 28, 31, 0.28), rgba(27, 28, 31, 0.28)), url(${coverImage})`,
                  }
                : undefined
            }
            onClick={() => photoInputRef.current?.click()}
          >
            {coverImage ? (
              <span className="upload-photo-button">Сменить фото</span>
            ) : (
              <>
                <b>↑</b>
                <span>Перетащите фото или выберите</span>
                <small>1600×900 · jpg / png</small>
                <span className="upload-photo-button">Загрузить фото</span>
              </>
            )}
          </button>
        </label>
        <div className="form-actions">
          <button
            type="button"
            className="secondary"
            onClick={() => go("trips")}
          >
            Отмена
          </button>
          <button className="accent">Создать путешествие</button>
        </div>
      </form>
    </div>
  );
}

function PlaceRow({ place, index }: { place: string; index: number }) {
  const times = ["08:30", "09:30", "12:00", "14:30", "17:00"];
  const isFood = index % 3 === 0;
  return (
    <div className="place-row">
      <span className="place-number">{index + 1}</span>
      <div>
        <b>{place}</b>
        <small className={isFood ? "food" : "sight"}>
          {isFood ? "Еда" : "Достопримечательность"}
        </small>
      </div>
      <time>{times[index] ?? "12:00"}</time>
    </div>
  );
}

function RoadLegEditor({
  roadLeg,
  onChange,
  onSave,
  onCancel,
}: {
  roadLeg?: RoadLeg;
  onChange: (roadLeg: RoadLeg) => void;
  onSave: (roadLeg: RoadLeg) => void;
  onCancel: () => void;
}) {
  const [from, setFrom] = useState(roadLeg?.from || "");
  const [to, setTo] = useState(roadLeg?.to || "");
  const [checkInFrom, setCheckInFrom] = useState(roadLeg?.checkInFrom || "");
  const [checkInTo, setCheckInTo] = useState(roadLeg?.checkInTo || "");
  const [checkOutFrom, setCheckOutFrom] = useState(roadLeg?.checkOutFrom || "");
  const [checkOutTo, setCheckOutTo] = useState(roadLeg?.checkOutTo || "");
  const [notes, setNotes] = useState(roadLeg?.notes || "");
  const [customMapsUrl, setCustomMapsUrl] = useState(roadLeg?.mapsUrl || "");
  const generatedMapsUrl =
    from.trim() && to.trim() ? mapsUrl(from.trim(), to.trim()) : "";
  const routeMapsUrl = customMapsUrl.trim() || generatedMapsUrl;
  const emitChange = useEffectEvent(onChange);
  const latestRoadLeg = useRef<RoadLeg | null>(null);
  latestRoadLeg.current = {
    from: from.trim(),
    to: to.trim(),
    checkInFrom,
    checkInTo,
    checkOutFrom,
    checkOutTo,
    notes: notes.trim(),
    mapsUrl: customMapsUrl.trim() || undefined,
  };
  useEffect(() => {
    if (
      ![
        from,
        to,
        checkInFrom,
        checkInTo,
        checkOutFrom,
        checkOutTo,
        notes,
        customMapsUrl,
      ].some((value) => value.trim())
    )
      return;
    const timeout = window.setTimeout(
      () =>
        emitChange({
          from: from.trim(),
          to: to.trim(),
          checkInFrom,
          checkInTo,
          checkOutFrom,
          checkOutTo,
          notes: notes.trim(),
          mapsUrl: customMapsUrl.trim() || undefined,
        }),
      500,
    );
    return () => window.clearTimeout(timeout);
  }, [
    from,
    to,
    checkInFrom,
    checkInTo,
    checkOutFrom,
    checkOutTo,
    notes,
    customMapsUrl,
  ]);
  useEffect(
    () => () => {
      const latest = latestRoadLeg.current;
      if (latest && Object.values(latest).some(Boolean)) emitChange(latest);
    },
    [],
  );
  return (
    <form
      className="road-leg-editor"
      onSubmit={(event) => {
        event.preventDefault();
        if (!from.trim() || !to.trim()) return;
        onSave({
          from: from.trim(),
          to: to.trim(),
          checkInFrom,
          checkInTo,
          checkOutFrom,
          checkOutTo,
          notes: notes.trim(),
          mapsUrl: customMapsUrl.trim() || undefined,
        });
      }}
    >
      <div className="road-leg-editor-title">
        <b>Автомобильный маршрут</b>
        <span>Заполните переезд на этот день</span>
      </div>
      <div className="road-leg-fields">
        <label>
          Откуда
          <input
            value={from}
            onChange={(event) => setFrom(event.target.value)}
            placeholder="Например, Мюнхен"
            autoFocus
          />
        </label>
        <label>
          Куда
          <input
            value={to}
            onChange={(event) => setTo(event.target.value)}
            placeholder="Например, Верона"
          />
        </label>
      </div>
      <div className="road-leg-fields road-leg-times">
        <label>
          Заселение: с
          <input
            type="time"
            value={checkInFrom}
            onChange={(event) => setCheckInFrom(event.target.value)}
          />
        </label>
        <label>
          Заселение: до
          <input
            type="time"
            value={checkInTo}
            onChange={(event) => setCheckInTo(event.target.value)}
          />
        </label>
        <label>
          Выселение: с
          <input
            type="time"
            value={checkOutFrom}
            onChange={(event) => setCheckOutFrom(event.target.value)}
          />
        </label>
        <label>
          Выселение: до
          <input
            type="time"
            value={checkOutTo}
            onChange={(event) => setCheckOutTo(event.target.value)}
          />
        </label>
      </div>
      <label className="road-notes">
        Заметки
        <textarea
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          placeholder="Например, заправиться перед выездом"
        />
      </label>
      <label className="road-notes">
        Ссылка Google Maps
        <input
          type="url"
          value={customMapsUrl}
          onChange={(event) => setCustomMapsUrl(event.target.value)}
          placeholder="https://maps.app.goo.gl/..."
        />
      </label>
      {routeMapsUrl && <GoogleMapsLink url={routeMapsUrl} />}
      <div className="road-leg-actions">
        <button type="button" className="secondary" onClick={onCancel}>
          Отмена
        </button>
        <button className="accent">Сохранить маршрут</button>
      </div>
    </form>
  );
}

function GoogleMapsLink({ url }: { url: string }) {
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    await navigator.clipboard.writeText(url).catch(() => undefined);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };
  return (
    <div className="google-maps-link">
      <span>
        <b>Google Maps</b>
        <small>Автомобильный маршрут</small>
      </span>
      <a href={url} target="_blank" rel="noreferrer">
        Открыть ↗
      </a>
      <button onClick={copy}>{copied ? "Скопировано" : "Копировать"}</button>
    </div>
  );
}

function DraftRouteCard({
  day,
  index,
  editing,
  selected,
  onSelect,
  onEdit,
  onChange,
  onSave,
  onCancel,
}: {
  day: DraftDay;
  index: number;
  editing: boolean;
  selected: boolean;
  onSelect: () => void;
  onEdit: () => void;
  onChange: (roadLeg: RoadLeg) => void;
  onSave: (roadLeg: RoadLeg) => void;
  onCancel: () => void;
}) {
  const roadLeg = day.roadLeg;
  const routeMapsUrl = roadLeg
    ? roadLeg.mapsUrl || mapsUrl(roadLeg.from, roadLeg.to)
    : "";
  const checkIn = [roadLeg?.checkInFrom, roadLeg?.checkInTo]
    .filter(Boolean)
    .join(" - ");
  const checkOut = [roadLeg?.checkOutFrom, roadLeg?.checkOutTo]
    .filter(Boolean)
    .join(" - ");
  const itemCount = roadLeg
    ? 1 +
      Number(Boolean(checkIn)) +
      Number(Boolean(checkOut)) +
      Number(Boolean(roadLeg.notes))
    : 0;
  const checklist = [
    { id: "departure", label: `Выезд из ${roadLeg?.from || "города"}` },
    {
      id: "check-in",
      label: `Заселение в отель${checkIn ? ` ${checkIn}` : ""}`,
    },
    {
      id: "check-out",
      label: `Выселение из отеля${checkOut ? ` ${checkOut}` : ""}`,
    },
  ];
  return (
    <article
      className={selected ? "draft-route-card selected" : "draft-route-card"}
    >
      <header onClick={onSelect}>
        <div className="draft-day-number">
          <b>{index + 1}</b>
          <span>ДЕНЬ</span>
        </div>
        <div className="draft-route-title">
          <h2>
            {roadLeg ? (
              <>
                {cityFlag(roadLeg.from)} {roadLeg.from || "Откуда"} <b>→</b>{" "}
                {cityFlag(roadLeg.to)} {roadLeg.to || "Куда"}
              </>
            ) : (
              "Новый автопереезд"
            )}
          </h2>
          <span>
            {itemCount}/{roadLeg ? itemCount : 4} пунктов
          </span>
        </div>
        <div className="draft-route-actions">
          {roadLeg && (
            <a href={routeMapsUrl} target="_blank" rel="noreferrer">
              ↗ Карта
            </a>
          )}
          <button onClick={onEdit}>
            {roadLeg ? "Изменить" : "＋ Маршрут"}
          </button>
        </div>
      </header>
      {editing ? (
        <RoadLegEditor
          roadLeg={roadLeg}
          onChange={onChange}
          onSave={onSave}
          onCancel={onCancel}
        />
      ) : roadLeg ? (
        <>
          <div className="route-checklist">
            {checklist.map((item) => (
              <label
                className={
                  roadLeg.completed?.includes(item.id) ? "completed" : ""
                }
                key={item.id}
              >
                <input
                  type="checkbox"
                  checked={roadLeg.completed?.includes(item.id) || false}
                  onChange={() =>
                    onChange({
                      ...roadLeg,
                      completed: roadLeg.completed?.includes(item.id)
                        ? roadLeg.completed.filter((id) => id !== item.id)
                        : [...(roadLeg.completed || []), item.id],
                    })
                  }
                />
                <span>{item.label}</span>
              </label>
            ))}
            {roadLeg.notes && (
              <p>
                <i />
                {roadLeg.notes}
              </p>
            )}
          </div>
          <GoogleMapsLink url={routeMapsUrl} />
        </>
      ) : (
        <div className="route-card-empty">
          Добавьте направление, время заселения и дорожные заметки.
        </div>
      )}
    </article>
  );
}

function RouteTab({
  isDraft = false,
  draftDays = [],
  editingRoadDay = null,
  onEditingRoadDayChange,
  onAddDraftDay,
  onUpdateDraftDay,
}: {
  isDraft?: boolean;
  draftDays?: DraftDay[];
  editingRoadDay?: number | null;
  onEditingRoadDayChange?: (day: number | null) => void;
  onAddDraftDay?: () => void;
  onUpdateDraftDay?: (day: number, changes: Partial<DraftDay>) => void;
}) {
  const [day, setDay] = useState(0);
  const [selectedRouteDay, setSelectedRouteDay] = useState(0);
  const [routeTotals, setRouteTotals] = useState<{
    distance: number;
    duration: number;
  } | null>(null);
  const [variant, setVariant] = useState<"rail" | "tabs" | "feed">("rail");
  useEffect(
    () =>
      setDay((current) => Math.min(current, Math.max(0, draftDays.length - 1))),
    [draftDays.length],
  );
  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    const legs = draftDays
      .flatMap((day) =>
        day.roadLeg
          ? [[mapLocation(day.roadLeg.from), mapLocation(day.roadLeg.to)]]
          : [],
      )
      .filter((leg): leg is [[number, number], [number, number]] =>
        Boolean(leg[0] && leg[1]),
      );
    if (!token || !legs.length) {
      setRouteTotals(null);
      return;
    }
    let cancelled = false;
    void Promise.all(
      legs.map(async ([from, to]) => {
        const response = await fetch(
          `https://api.mapbox.com/directions/v5/mapbox/driving/${from.join(",")};${to.join(",")}?overview=false&access_token=${token}`,
        );
        const data = (await response.json()) as {
          routes?: { distance: number; duration: number }[];
        };
        return data.routes?.[0];
      }),
    )
      .then((routes) => {
        const validRoutes = routes.filter(
          (route): route is { distance: number; duration: number } =>
            Boolean(route),
        );
        if (cancelled || validRoutes.length !== legs.length) return;
        setRouteTotals(
          validRoutes.reduce<{ distance: number; duration: number }>(
            (total, route) => ({
              distance: total.distance + route.distance,
              duration: total.duration + route.duration,
            }),
            { distance: 0, duration: 0 },
          ),
        );
      })
      .catch(() => {
        if (!cancelled) setRouteTotals(null);
      });
    return () => {
      cancelled = true;
    };
  }, [draftDays]);
  const currentDraftDay: DraftDay = draftDays[day] || {
    id: "day-1",
    places: [],
  };
  if (isDraft)
    return (
      <div className="draft-route-with-map">
        <div className="draft-route-cards">
          <div className="route-toolbar">
            <span>
              Планирование по дням · добавляйте автопереезды и дорожные заметки
            </span>
          </div>
          {draftDays.map((draftDay, index) => (
            <DraftRouteCard
              day={draftDay}
              index={index}
              editing={editingRoadDay === index}
              selected={selectedRouteDay === index}
              onSelect={() => setSelectedRouteDay(index)}
              onEdit={() => onEditingRoadDayChange?.(index)}
              onChange={(roadLeg) => onUpdateDraftDay?.(index, { roadLeg })}
              onSave={(roadLeg) => {
                onUpdateDraftDay?.(index, { roadLeg });
                onEditingRoadDayChange?.(null);
              }}
              onCancel={() => onEditingRoadDayChange?.(null)}
              key={draftDay.id}
            />
          ))}
          <button className="add-route-day" onClick={onAddDraftDay}>
            ＋ Добавить день
          </button>
        </div>
        <aside className="map-card">
          <TripMap routeDays={draftDays} activeDay={selectedRouteDay} />
          <footer>
            <span>Общий маршрут</span>
            <b>
              {draftDays.length} дней
              {routeTotals &&
                ` · ${Math.round(routeTotals.distance / 1000).toLocaleString("ru-RU")} км · ${Math.round(routeTotals.duration / 3600)} ч`}
            </b>
          </footer>
        </aside>
      </div>
    );
  const current = days[day];
  const daySelector = (
    <div className={`day-rail ${variant === "tabs" ? "horizontal" : ""}`}>
      {days.map((item, index) => (
        <button
          className={index === day ? "active" : ""}
          onClick={() => setDay(index)}
          key={item.date}
        >
          <small>
            {variant === "tabs" ? `Д${index + 1}` : `День ${index + 1}`}
          </small>
          <b>{item.city}</b>
          <span>
            {item.date} · {item.places.length} мест
          </span>
        </button>
      ))}
    </div>
  );
  const plan = (
    <section className="day-plan">
      <header>
        <h2>
          День {day + 1} · {current.city}
        </h2>
        <span>{current.date}</span>
      </header>
      <div className={variant === "tabs" ? "place-cards" : ""}>
        {current.places.map((place, index) => (
          <PlaceRow place={place} index={index} key={place} />
        ))}
      </div>
      <button className="add-place">＋ Добавить место в этот день</button>
    </section>
  );
  const map = (
    <aside className="map-card">
      <TripMap city={current.city} places={current.places} />
      <footer>
        <span>Маршрут дня</span>
        <b>
          ≈ {current.distance} · {current.places.length} точек
        </b>
      </footer>
    </aside>
  );
  return (
    <>
      <div className="route-toolbar">
        <span>Планирование по дням · перетаскивайте места между днями</span>
        <div className="view-switch">
          <span>Вид маршрута</span>
          {(["rail", "tabs", "feed"] as const).map((value) => (
            <button
              className={variant === value ? "active" : ""}
              onClick={() => setVariant(value)}
              key={value}
            >
              {{ rail: "Дни-рейл", tabs: "Вкладки", feed: "Лента" }[value]}
            </button>
          ))}
        </div>
      </div>
      {variant === "feed" ? (
        <div className="feed-layout">
          <section className="day-feed">
            {days.map((item, index) => (
              <article key={item.date} className={index === day ? "open" : ""}>
                <button onClick={() => setDay(index)}>
                  <span>{index + 1}</span>
                  <b>
                    {item.city}
                    <small>
                      {item.date} · {item.places.length} мест
                    </small>
                  </b>
                  <i>⌄</i>
                </button>
                {index === day && (
                  <div>
                    {item.places.map((place, placeIndex) => (
                      <PlaceRow place={place} index={placeIndex} key={place} />
                    ))}
                  </div>
                )}
              </article>
            ))}
          </section>
          {map}
        </div>
      ) : (
        <div
          className={`route-layout ${variant === "tabs" ? "tab-layout" : ""}`}
        >
          {variant === "rail" && daySelector}
          {variant === "tabs" && (
            <div className="tabs-selector">{daySelector}</div>
          )}
          {plan}
          {map}
        </div>
      )}
    </>
  );
}

function RestaurantPage({
  places,
  onChange,
}: {
  places: ImportedRestaurant[];
  onChange: (restaurants: ImportedRestaurant[]) => void;
}) {
  const [city, setCity] = useState("Все города");
  const [status, setStatus] = useState("Все статусы");
  const [query, setQuery] = useState("");
  const [openFilter, setOpenFilter] = useState<"city" | "status" | null>(null);
  const [activePhotos, setActivePhotos] = useState<Record<string, number>>({});
  const [expandedPhoto, setExpandedPhoto] = useState<{
    photos: string[];
    index: number;
  } | null>(null);
  const [editing, setEditing] = useState<ImportedRestaurant | null>(null);
  const cities = Array.from(
    new Set(places.map((place) => place.city).filter(Boolean)),
  ).sort();
  const statusLabels: Record<string, string> = {
    "Все статусы": "Все статусы",
    хочу: "Хочу",
    бронь: "Забронировано",
    были: "Были",
  };
  const visible = places.filter(
    (place) =>
      (city === "Все города" || place.city === city) &&
      (status === "Все статусы" || place.status === status) &&
      `${place.name} ${place.city}`.toLowerCase().includes(query.trim().toLowerCase()),
  );
  const choose = (kind: "city" | "status", value: string) => {
    if (kind === "city") setCity(value);
    else setStatus(value);
    setOpenFilter(null);
  };
  const preloadPhotos = (photos: string[]) => {
    photos.forEach((photo) => {
      const image = new Image();
      image.src = photo;
    });
  };
  const saveRestaurant = (restaurant: ImportedRestaurant) => {
    onChange(
      places.map((place) =>
        place.id === restaurant.id ? restaurant : place,
      ),
    );
    setEditing(null);
  };
  const deleteRestaurant = (restaurantId: string) => {
    onChange(places.filter((place) => place.id !== restaurantId));
    setEditing(null);
  };
  return (
    <>
    <section className="restaurants-page">
      <header>
        <div>
          <p className="eyebrow">РЕСТОРАНЫ ПО МАРШРУТУ</p>
          <h2>Рестораны</h2>
        </div>
      </header>
      <div className="restaurant-dropdowns">
        <div className="restaurant-dropdown">
          <span>ГОРОД</span>
          <button
            className="restaurant-dropdown-trigger"
            aria-expanded={openFilter === "city"}
            onClick={() => setOpenFilter(openFilter === "city" ? null : "city")}
          >
            {city}
            <i>⌄</i>
          </button>
          {openFilter === "city" && (
            <div className="restaurant-dropdown-menu">
              {["Все города", ...cities].map((item) => (
                <button
                  className={city === item ? "active" : ""}
                  onClick={() => choose("city", item)}
                  key={item}
                >
                  {item}
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="restaurant-dropdown">
          <span>ФИЛЬТРЫ</span>
          <button
            className="restaurant-dropdown-trigger"
            aria-expanded={openFilter === "status"}
            onClick={() =>
              setOpenFilter(openFilter === "status" ? null : "status")
            }
          >
            {statusLabels[status]}
            <i>⌄</i>
          </button>
          {openFilter === "status" && (
            <div className="restaurant-dropdown-menu">
              {Object.keys(statusLabels).map((item) => (
                <button
                  className={status === item ? "active" : ""}
                  onClick={() => choose("status", item)}
                  key={item}
                >
                  {statusLabels[item]}
                </button>
              ))}
            </div>
          )}
        </div>
        </div>
        <label className="restaurant-search">
          <span>⌕</span>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Поиск по ресторану или городу"
          />
        </label>
        <div className="restaurant-grid">
        {visible.map((place, index) => {
          const photos = place.photos?.filter(Boolean) || [];
          const photoIndex = (activePhotos[place.id] || 0) % Math.max(photos.length, 1);
          const changePhoto = (offset: number) => {
            if (photos.length < 2) return;
            setActivePhotos((current) => ({
              ...current,
              [place.id]: (photoIndex + offset + photos.length) % photos.length,
            }));
          };
          return (
            <article className={`restaurant-card c${index % 6}`} key={place.id}>
            <div
              className="restaurant-photo"
              onMouseEnter={() => preloadPhotos(photos)}
              onTouchStart={() => preloadPhotos(photos)}
              onClick={() => {
                if (photos[photoIndex]) {
                  setExpandedPhoto({ photos, index: photoIndex });
                }
              }}
            >
              {photos[photoIndex] && (
                <img src={photos[photoIndex]} alt="" loading="lazy" />
              )}
              <span>{place.status}</span>
              <button
                type="button"
                className="restaurant-edit"
                aria-label="Редактировать ресторан"
                onClick={(event) => {
                  event.stopPropagation();
                  setEditing(place);
                }}
              >
                ⋯
              </button>
              {photos.length > 1 && (
                <>
                  <button
                    type="button"
                    className="restaurant-photo-previous"
                    aria-label="Предыдущее фото"
                    onClick={(event) => {
                      event.stopPropagation();
                      changePhoto(-1);
                    }}
                  >
                    ‹
                  </button>
                  <button
                    type="button"
                    className="restaurant-photo-next"
                    aria-label="Следующее фото"
                    onClick={(event) => {
                      event.stopPropagation();
                      changePhoto(1);
                    }}
                  >
                    ›
                  </button>
                  <i>{photos.map((_, photo) => photo === photoIndex ? "●" : "○").join(" ")}</i>
                </>
              )}
            </div>
            <div>
              <div className="restaurant-meta">
                {place.googleRating !== undefined && (
                  <span className="google-rating">
                    <b>Google</b> {place.googleRating.toFixed(1)}
                    <i>★</i>
                    {place.googleReviews?.toLocaleString("ru-RU")}
                  </span>
                )}
                {place.price && <span className="restaurant-price">{place.price}</span>}
              </div>
              <p>
                {cityFlag(place.city)} {place.city}
              </p>
              <h3>{place.name}</h3>
              {place.note && <small>{place.note}</small>}
              <footer>
                {place.link ? (
                  <a href={place.link} target="_blank" rel="noreferrer">
                    Открыть →
                  </a>
                ) : (
                  "Ресторан"
                )}
                <i>♡</i>
              </footer>
            </div>
            </article>
          );
        })}
      </div>
    </section>
    {expandedPhoto && (
      <div
        className="accommodation-photo-lightbox"
        role="dialog"
        aria-modal="true"
        aria-label="Просмотр фотографии ресторана"
        onClick={() => setExpandedPhoto(null)}
      >
        <img
          src={expandedPhoto.photos[expandedPhoto.index]}
          alt="Фотография ресторана"
          onClick={(event) => event.stopPropagation()}
        />
        {expandedPhoto.photos.length > 1 && (
          <>
            <button
              className="lightbox-previous"
              type="button"
              aria-label="Предыдущее фото"
              onClick={(event) => {
                event.stopPropagation();
                setExpandedPhoto((current) => current && {
                  ...current,
                  index: (current.index - 1 + current.photos.length) % current.photos.length,
                });
              }}
            >
              ‹
            </button>
            <button
              className="lightbox-next"
              type="button"
              aria-label="Следующее фото"
              onClick={(event) => {
                event.stopPropagation();
                setExpandedPhoto((current) => current && {
                  ...current,
                  index: (current.index + 1) % current.photos.length,
                });
              }}
            >
              ›
            </button>
          </>
        )}
        <button className="lightbox-close" type="button" onClick={() => setExpandedPhoto(null)}>
          ×
        </button>
      </div>
    )}
    {editing && (
      <RestaurantEditor
        restaurant={editing}
        onClose={() => setEditing(null)}
        onSave={saveRestaurant}
        onDelete={() => deleteRestaurant(editing.id)}
      />
    )}
    </>
  );
}

function RestaurantForm({ onClose }: { onClose: () => void }) {
  const [status, setStatus] = useState("хочу");
  const [priority, setPriority] = useState(false);
  const [price, setPrice] = useState("€€");
  const [photos, setPhotos] = useState(["", "", ""]);
  const selectPhoto = (file: File | undefined, index: number) => {
    if (!file) return;
    if (!file.type.match(/^image\/(jpeg|png|webp)$/)) {
      window.alert("Выберите изображение JPG, PNG или WebP.");
      return;
    }
    const preview = URL.createObjectURL(file);
    setPhotos((current) =>
      current.map((photo, photoIndex) =>
        photoIndex === index ? preview : photo,
      ),
    );
  };
  return (
    <div className="restaurant-modal-backdrop" onClick={onClose}>
      <form
        className="restaurant-modal restaurant-full-form"
        onSubmit={(event) => {
          event.preventDefault();
          onClose();
        }}
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <h2>Новый ресторан</h2>
          <button type="button" onClick={onClose}>
            ×
          </button>
        </header>
        <div className="restaurant-upload">
          {photos.map((photo, index) => (
            <label
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => {
                event.preventDefault();
                selectPhoto(event.dataTransfer.files[0], index);
              }}
              key={index}
            >
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={(event) => {
                  selectPhoto(event.target.files?.[0], index);
                  event.target.value = "";
                }}
              />
              {photo ? (
                <img src={photo} alt={index === 0 ? "Обложка ресторана" : "Фото ресторана"} />
              ) : (
                <span>
                  {index === 0 ? "Обложка — перетащите фото" : "＋ Фото"}
                  <br />
                  <u>or browse files</u>
                </span>
              )}
            </label>
          ))}
        </div>
        <label>
          Название
          <input defaultValue="Trattoria Mario" />
        </label>
        <div className="restaurant-form-grid">
          <label>
            Город
            <select defaultValue="Флоренция">
              <option>Флоренция</option>
              <option>Рим</option>
              <option>Венеция</option>
            </select>
          </label>
          <label>
            Кухня
            <input defaultValue="Тоскана" />
          </label>
          <label>
            Дата и время
            <select defaultValue="15 сент · 13:00">
              <option>15 сент · 13:00</option>
              <option>15 сент · 20:00</option>
              <option>16 сент · 13:00</option>
            </select>
          </label>
          <label>
            Средний чек
            <div className="price-options">
              {["€€", "€€€", "€€€€"].map((item) => (
                <button
                  type="button"
                  className={price === item ? "selected" : ""}
                  onClick={() => setPrice(item)}
                  key={item}
                >
                  {item}
                </button>
              ))}
            </div>
          </label>
        </div>
        <label>
          Адрес
          <input defaultValue="Via Rosina, 2" />
        </label>
        <section className="restaurant-status">
          <b>Статус</b>
          <div>
            {["хочу", "бронь", "были"].map((item) => (
              <button
                type="button"
                className={status === item ? "active" : ""}
                onClick={() => setStatus(item)}
                key={item}
              >
                {item}
              </button>
            ))}
            <button
              type="button"
              className={priority ? "active" : ""}
              onClick={() => setPriority((current) => !current)}
            >
              🔥 Приоритет
            </button>
          </div>
        </section>
        <footer>
          <button type="button" onClick={onClose}>
            Отмена
          </button>
          <button className="accent">Сохранить</button>
        </footer>
      </form>
    </div>
  );
}

function RestaurantEditor({
  restaurant,
  onClose,
  onSave,
  onDelete,
}: {
  restaurant: ImportedRestaurant;
  onClose: () => void;
  onSave: (restaurant: ImportedRestaurant) => void;
  onDelete: () => void;
}) {
  const [name, setName] = useState(restaurant.name);
  const [city, setCity] = useState(restaurant.city);
  const [note, setNote] = useState(restaurant.note || "");
  const [link, setLink] = useState(restaurant.link || "");
  const [status, setStatus] = useState(restaurant.status || "хочу");
  const [price, setPrice] = useState(restaurant.price || "€€");
  const [placeType, setPlaceType] = useState(restaurant.placeType || "ресторан");
  const [categories, setCategories] = useState(restaurant.categories || []);
  const [priority, setPriority] = useState(Boolean(restaurant.priority));
  const [dogFriendly, setDogFriendly] = useState(Boolean(restaurant.dogFriendly));
  const [photos, setPhotos] = useState(() => [
    ...(restaurant.photos || []),
    ...Array(3).fill(""),
  ].slice(0, 3));
  const [uploading, setUploading] = useState<number | null>(null);
  const uploadPhoto = async (file: File | undefined, index: number) => {
    if (!file) return;
    if (!file.type.match(/^image\/(jpeg|png|webp)$/) || file.size > 10 * 1024 * 1024) {
      window.alert("Выберите JPG, PNG или WebP до 10 МБ.");
      return;
    }
    setUploading(index);
    try {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session) throw new Error("No active session");
      const extension = file.name.split(".").pop()?.toLowerCase() || "jpg";
      const path = `${session.user.id}/restaurants/${crypto.randomUUID()}.${extension}`;
      const { error } = await supabase.storage.from("trip-photos").upload(path, file, {
        cacheControl: "31536000",
        contentType: file.type,
        upsert: false,
      });
      if (error) throw error;
      const url = supabase.storage.from("trip-photos").getPublicUrl(path).data.publicUrl;
      setPhotos((current) => current.map((photo, photoIndex) => photoIndex === index ? url : photo));
    } catch {
      window.alert("Не удалось загрузить фотографию. Попробуйте ещё раз.");
    } finally {
      setUploading(null);
    }
  };
  const toggleCategory = (category: string) => {
    setCategories((current) =>
      current.includes(category)
        ? current.filter((item) => item !== category)
        : [...current, category],
    );
  };
  return (
    <div className="restaurant-modal-backdrop" onClick={onClose}>
      <form
        className="restaurant-modal restaurant-editor"
        onSubmit={(event) => {
          event.preventDefault();
          onSave({
            ...restaurant,
            name: name.trim() || restaurant.name,
            city: city.trim() || restaurant.city,
            note: note.trim(),
            link: link.trim(),
            status,
            price,
            placeType,
            categories,
            priority,
            dogFriendly,
            photos: photos.filter(Boolean),
          });
        }}
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <h2>Редактировать ресторан</h2>
          <button type="button" onClick={onClose}>×</button>
        </header>
        <label>Название<input value={name} onChange={(event) => setName(event.target.value)} /></label>
        <label>Город<input value={city} onChange={(event) => setCity(event.target.value)} /></label>
        <label>Кухня / что заказать<input value={note} onChange={(event) => setNote(event.target.value)} /></label>
        <label>Ссылка Google Maps<input type="url" value={link} onChange={(event) => setLink(event.target.value)} /></label>
        <section className="restaurant-editor-options">
          <b>Тип места</b>
          <div>{["ресторан", "кафе", "бар"].map((item) => <button className={placeType === item ? "active" : ""} type="button" onClick={() => setPlaceType(item)} key={item}>{item}</button>)}</div>
          <b>Категории</b>
          <div>{["пиццерия", "морепродукты", "желатерия", "бар", "ресторан", "кафе"].map((item) => <button className={categories.includes(item) ? "active" : ""} type="button" onClick={() => toggleCategory(item)} key={item}>{item}</button>)}</div>
          <b>Уровень цен</b>
          <div>{["€", "€€", "€€€", "€€€€"].map((item) => <button className={price === item ? "active" : ""} type="button" onClick={() => setPrice(item)} key={item}>{item}</button>)}</div>
          <b>Статус</b>
          <div>{["хочу", "бронь", "были"].map((item) => <button className={status === item ? "active" : ""} type="button" onClick={() => setStatus(item)} key={item}>{item}</button>)}<button className={priority ? "active" : ""} type="button" onClick={() => setPriority((current) => !current)}>🔥 приоритет</button><button className={dogFriendly ? "active" : ""} type="button" onClick={() => setDogFriendly((current) => !current)}>🐶 Можно с собакой</button></div>
        </section>
        <section className="restaurant-editor-photos">
          <b>Фотографии</b>
          <div>
            {photos.map((photo, index) => (
              <label key={index}>
                <input type="file" accept="image/jpeg,image/png,image/webp" disabled={uploading !== null} onChange={(event) => { void uploadPhoto(event.target.files?.[0], index); event.target.value = ""; }} />
                {photo ? <img src={photo} alt="" /> : <span>{uploading === index ? "Загрузка..." : "＋ Фото"}</span>}
              </label>
            ))}
          </div>
        </section>
        <footer>
          <button className="restaurant-delete" type="button" onClick={onDelete}>Удалить ресторан</button>
          <button type="button" onClick={onClose}>Отмена</button>
          <button className="accent" disabled={uploading !== null}>Сохранить</button>
        </footer>
      </form>
    </div>
  );
}

function Restaurants({
  trip,
  onUpdateTrip,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
}) {
  const [addingRestaurant, setAddingRestaurant] = useState(false);
  return (
    <div className="restaurants-with-add">
      <RestaurantPage
        places={trip.restaurants || []}
        onChange={(restaurants) => onUpdateTrip({ ...trip, restaurants })}
      />
      <button
        className="restaurant-add-button"
        onClick={() => setAddingRestaurant(true)}
      >
        ＋ Добавить
      </button>
      {addingRestaurant && (
        <RestaurantForm onClose={() => setAddingRestaurant(false)} />
      )}
    </div>
  );
  return (
    <div className="restaurants-with-add">
      <RestaurantPage
        places={trip.restaurants || []}
        onChange={(restaurants) => onUpdateTrip({ ...trip, restaurants })}
      />
      <button
        className="restaurant-add-button"
        onClick={() => setAddingRestaurant(true)}
      >
        ＋ Добавить
      </button>
      {addingRestaurant && (
        <div
          className="restaurant-modal-backdrop"
          onClick={() => setAddingRestaurant(false)}
        >
          <form
            className="restaurant-modal"
            onSubmit={(event) => {
              event.preventDefault();
              setAddingRestaurant(false);
            }}
            onClick={(event) => event.stopPropagation()}
          >
            <header>
              <h2>Новый ресторан</h2>
              <button type="button" onClick={() => setAddingRestaurant(false)}>
                ×
              </button>
            </header>
            <label>
              Название
              <input autoFocus placeholder="Например, Trattoria Mario" />
            </label>
            <div className="restaurant-form-grid">
              <label>
                Город
                <select defaultValue="Рим">
                  <option>Рим</option>
                  <option>Флоренция</option>
                  <option>Венеция</option>
                </select>
              </label>
              <label>
                Кухня
                <input placeholder="Например, итальянская" />
              </label>
              <label>
                Дата и время
                <input type="datetime-local" />
              </label>
              <label>
                Средний чек
                <input placeholder="€€" />
              </label>
            </div>
            <footer>
              <button type="button" onClick={() => setAddingRestaurant(false)}>
                Отмена
              </button>
              <button className="accent">Добавить</button>
            </footer>
          </form>
        </div>
      )}
    </div>
  );
  const [city, setCity] = useState("Все · 6");
  const [status, setStatus] = useState("Все статусы");
  const [adding, setAdding] = useState(false);
  const places = [
    [
      "Roscióli Salumeria",
      "Рим",
      "были",
      "4.7",
      "Via dei Giubbonari, 21",
      "13 сент · 21:00",
    ],
    [
      "Emma Pizzeria",
      "Рим",
      "бронь",
      "4.6",
      "Via del Monte della Farina, 28",
      "12 сент · 20:30",
    ],
    [
      "Trattoria Mario",
      "Флоренция",
      "хочу",
      "4.8",
      "Via Rosina, 2",
      "15 сент · 13:00",
    ],
    [
      "Caffè Gilli",
      "Флоренция",
      "бронь",
      "4.5",
      "Via Roma, 1",
      "15 сент · 10:30",
    ],
    [
      "Osteria alle Testiere",
      "Венеция",
      "хочу",
      "4.9",
      "Calle del Mondo Novo, 5801",
      "17 сент · 19:30",
    ],
    [
      "Trattoria da Remigio",
      "Венеция",
      "были",
      "4.4",
      "Castello, 3416",
      "18 сент · 20:00",
    ],
  ];
  const visible = places.filter(
    (place) =>
      (city === "Все · 6" || place[1] === city.split(" · ")[0]) &&
      (status === "Все статусы" || place[2] === status),
  );
  return (
    <>
      <section className="restaurants-page">
        <header>
          <div>
            <p className="eyebrow">ИТАЛИЯ · РИМ, ФЛОРЕНЦИЯ, ВЕНЕЦИЯ</p>
            <h2>Рестораны</h2>
          </div>
          <button className="accent" onClick={() => setAdding(true)}>
            ＋ Добавить
          </button>
        </header>
        <div className="restaurant-filters">
          <span>ГОРОД</span>
          {["Все · 6", "Рим · 2", "Флоренция · 2", "Венеция · 2"].map(
            (item) => (
              <button
                className={city === item ? "active" : ""}
                onClick={() => setCity(item)}
                key={item}
              >
                {item}
              </button>
            ),
          )}
        </div>
        <div className="restaurant-filters status">
          {["Все статусы", "хочу", "бронь", "были"].map((item) => (
            <button
              className={status === item ? "active" : ""}
              onClick={() => setStatus(item)}
              key={item}
            >
              {item}
            </button>
          ))}
        </div>
        <div className="restaurant-grid">
          {visible.map((place, index) => (
            <article className={`restaurant-card c${index % 6}`} key={place[0]}>
              <div className="restaurant-photo">
                <span>{place[2]}</span>
                <b>★ {place[3]}</b>
                <small>€€</small>
              </div>
              <div>
                <p>🇮🇹 {place[1]}</p>
                <h3>{place[0]}</h3>
                <small>◷ {place[5]}</small>
                <small>⌖ {place[4]}</small>
                <footer>
                  Забронировать стол → <i>♡</i>
                </footer>
              </div>
            </article>
          ))}
        </div>
      </section>
      {adding && (
        <div
          className="restaurant-modal-backdrop"
          onClick={() => setAdding(false)}
        >
          <form
            className="restaurant-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <header>
              <h2>Новый ресторан</h2>
              <button type="button" onClick={() => setAdding(false)}>
                ×
              </button>
            </header>
            <div className="restaurant-upload">
              <div>
                ▧<br />
                Обложка — перетащите фото
                <br />
                <u>or browse files</u>
              </div>
              <div>
                ▧<br />＋ Фото
                <br />
                <u>or browse files</u>
              </div>
              <div>
                ▧<br />＋ Фото
                <br />
                <u>or browse files</u>
              </div>
            </div>
            <label>
              Название
              <input defaultValue="Trattoria Mario" />
            </label>
            <div className="restaurant-form-grid">
              <label>
                Город
                <select defaultValue="Флоренция">
                  <option>Флоренция</option>
                  <option>Рим</option>
                  <option>Венеция</option>
                </select>
              </label>
              <label>
                Кухня
                <input defaultValue="Тоскана" />
              </label>
              <label>
                Дата и время
                <input defaultValue="15 сент · 13:00" />
              </label>
              <label>
                Средний чек
                <div className="price-options">
                  <button type="button" className="selected">
                    €€
                  </button>
                  <button type="button">€€€</button>
                  <button type="button">€€€€</button>
                </div>
              </label>
            </div>
            <label>
              Адрес
              <input defaultValue="Via Rosina, 2" />
            </label>
            <label>
              Статус
              <div className="price-options">
                <button type="button" className="selected">
                  хочу
                </button>
                <button type="button">бронь</button>
                <button type="button">были</button>
              </div>
            </label>
            <footer>
              <button type="button" onClick={() => setAdding(false)}>
                Отмена
              </button>
              <button
                className="accent"
                type="button"
                onClick={() => setAdding(false)}
              >
                Сохранить
              </button>
            </footer>
          </form>
        </div>
      )}
    </>
  );
}

function LegacyAccommodation() {
  const stays: {
    name: string;
    city: string;
    dates: string;
    price: string;
    status: string;
    details: string;
  }[] = [];
  const [filter, setFilter] = useState("Все");
  const [statuses, setStatuses] = useState<Record<string, string>>(() =>
    Object.fromEntries(stays.map((stay) => [stay.name, stay.status])),
  );
  const [adding, setAdding] = useState(false);
  const visible = stays.filter(
    (stay) => filter === "Все" || statuses[stay.name] === filter,
  );
  const statusLabels = ["хочу", "бронь", "оплачено", "пожили"];
  return (
    <>
      <section className="accommodation-page">
        <header className="accommodation-heading">
          <h2>Жильё</h2>
          <button className="accent" onClick={() => setAdding(true)}>
            ＋ Добавить жильё
          </button>
        </header>
        <div className="accommodation-tabs">
          <button className="active">Список жилья</button>
          <button onClick={() => setFilter("отмена")}>Отмена</button>
        </div>
        <div className="accommodation-filters">
          {["Все", "хочу", "бронь", "оплачено"].map((item) => (
            <button
              className={filter === item ? "active" : ""}
              onClick={() => setFilter(item)}
              key={item}
            >
              {item === "Все"
                ? `Все · ${stays.length}`
                : item === "бронь"
                  ? "Забронировано"
                  : item[0].toUpperCase() + item.slice(1)}
            </button>
          ))}
        </div>
        <div className="accommodation-grid">
          {visible.map((stay, index) => (
            <article
              className={`accommodation-card c${index % 6}`}
              key={stay.name}
            >
              <div className="accommodation-photo">
                <span className={`stay-badge ${statuses[stay.name]}`}>
                  {statuses[stay.name]}
                </span>
                <button aria-label="Предыдущее фото">‹</button>
                <button aria-label="Следующее фото">›</button>
                <i>● ● ●</i>
              </div>
              <div className="accommodation-body">
                <p>
                  {cityFlag(stay.city)} {stay.city}
                </p>
                <h3>{stay.name}</h3>
                <div className="stay-price">
                  <span>{formatAccommodationDates(stay.dates)}</span>
                  <b>{formatAccommodationPrice(stay.price)}</b>
                </div>
                <div className="stay-statuses">
                  {statusLabels.map((item) => (
                    <button
                      className={
                        statuses[stay.name] === item ? `active ${item}` : ""
                      }
                      onClick={() =>
                        setStatuses({ ...statuses, [stay.name]: item })
                      }
                      key={item}
                    >
                      {item}
                    </button>
                  ))}
                </div>
                <small>{stay.details}</small>
                <footer>
                  <a
                    href="https://www.booking.com/"
                    target="_blank"
                    rel="noreferrer"
                  >
                    Ссылка на Букинг →
                  </a>
                  <button>удалить</button>
                </footer>
              </div>
            </article>
          ))}
        </div>
      </section>
      {adding && (
        <div
          className="restaurant-modal-backdrop"
          onClick={() => setAdding(false)}
        >
          <form
            className="restaurant-modal"
            onClick={(event) => event.stopPropagation()}
          >
            <header>
              <h2>Новое жильё</h2>
              <button type="button" onClick={() => setAdding(false)}>
                ×
              </button>
            </header>
            <label>
              Название
              <input autoFocus placeholder="Например, Hotel Artemide" />
            </label>
            <div className="restaurant-form-grid">
              <label>
                Город
                <input placeholder="Рим" />
              </label>
              <label>
                Даты
                <input placeholder="12–15 сентября" />
              </label>
            </div>
            <footer>
              <button type="button" onClick={() => setAdding(false)}>
                Отмена
              </button>
              <button className="accent">Сохранить жильё</button>
            </footer>
          </form>
        </div>
      )}
    </>
  );
}

function LegacyAccommodationForm({ onClose }: { onClose: () => void }) {
  const [status, setStatus] = useState("бронь");
  return (
    <div className="accommodation-modal-backdrop" onClick={onClose}>
      <form
        className="accommodation-modal"
        onSubmit={(event) => {
          event.preventDefault();
          onClose();
        }}
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <h2>Новое жильё</h2>
          <button type="button" onClick={onClose}>
            ×
          </button>
        </header>
        <section>
          <b>Фотографии</b>
          <div className="accommodation-upload">
            <div className="accommodation-cover">
              ▧
              <span>
                Обложка — перетащите фото
                <br />
                <u>or browse files</u>
              </span>
            </div>
            <div>
              ▧
              <span>
                ＋ Фото
                <br />
                <u>or browse files</u>
              </span>
            </div>
            <div>
              ▧
              <span>
                ＋ Фото
                <br />
                <u>or browse files</u>
              </span>
            </div>
          </div>
        </section>
        <section>
          <b>Статус</b>
          <div className="accommodation-form-statuses">
            {["хочу", "бронь", "оплачено", "пожили"].map((item) => (
              <button
                type="button"
                className={status === item ? "active" : ""}
                onClick={() => setStatus(item)}
                key={item}
              >
                {item}
              </button>
            ))}
          </div>
        </section>
        <label>
          Название
          <input defaultValue="La mia casa al @Pigneto" />
        </label>
        <div className="accommodation-form-grid">
          <label>
            Город
            <select defaultValue="Рим, Италия">
              <option>Рим, Италия</option>
              <option>Флоренция, Италия</option>
              <option>Венеция, Италия</option>
            </select>
          </label>
          <label>
            Цена
            <input defaultValue="€434,98" />
          </label>
          <label>
            Заезд
            <select defaultValue="27 сен">
              <option>27 сен</option>
              <option>28 сен</option>
              <option>29 сен</option>
            </select>
          </label>
          <label>
            Выезд
            <select defaultValue="30 сен">
              <option>30 сен</option>
              <option>1 окт</option>
              <option>2 окт</option>
            </select>
          </label>
        </div>
        <label>
          Ссылка на Booking
          <input defaultValue="https://booking.com/..." />
        </label>
        <label>
          Адрес / заметка
          <textarea defaultValue="Апартаменты с 1 спальней, до 4 гостей." />
        </label>
        <footer>
          <button type="button" onClick={onClose}>
            Отмена
          </button>
          <button className="accent">Сохранить жильё</button>
        </footer>
      </form>
    </div>
  );
}

function AccommodationForm({
  onClose,
  onSaved,
  initial,
}: {
  onClose: () => void;
  onSaved?: (accommodation: SavedAccommodation) => void;
  initial?: SavedAccommodation;
}) {
  const dateParts = accommodationDateParts(initial?.dates);
  const [status, setStatus] = useState(initial?.status || "бронь");
  const [name, setName] = useState(initial?.name || "La mia casa al @Pigneto");
  const [city, setCity] = useState(initial?.city || "Рим, Италия");
  const [photos, setPhotos] = useState(() => [
    ...(initial?.photos || []),
    ...Array(3).fill(""),
  ].slice(0, 3));
  const [photoTransforms, setPhotoTransforms] = useState(() =>
    Array.from({ length: 3 }, (_, index) => ({
      offsetX: initial?.photoTransforms?.[index]?.offsetX ?? 50,
      offsetY: initial?.photoTransforms?.[index]?.offsetY ?? 50,
    })),
  );
  const [draggedPhoto, setDraggedPhoto] = useState<number | null>(null);
  const [uploadingPhotos, setUploadingPhotos] = useState([false, false, false]);
  const isUploading = uploadingPhotos.some(Boolean);
  const uploadPhoto = async (file: File | undefined, index: number) => {
    if (!file) return;
    if (!file.type.match(/^image\/(jpeg|png|webp)$/) || file.size > 10 * 1024 * 1024) {
      window.alert("Выберите JPG, PNG или WebP до 10 МБ.");
      return;
    }
    setUploadingPhotos((current) =>
      current.map((uploading, photoIndex) =>
        photoIndex === index ? true : uploading,
      ),
    );
    try {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session) throw new Error("No active session");
      const extension = file.name.split(".").pop()?.toLowerCase() || "jpg";
      const path = `${session.user.id}/accommodations/${crypto.randomUUID()}.${extension}`;
      const { error } = await supabase.storage
        .from("trip-photos")
        .upload(path, file, {
          cacheControl: "31536000",
          contentType: file.type,
          upsert: false,
        });
      if (error) throw error;
      const publicUrl = supabase.storage.from("trip-photos").getPublicUrl(path)
        .data.publicUrl;
      setPhotos((current) =>
        current.map((photo, photoIndex) =>
          photoIndex === index ? publicUrl : photo,
        ),
      );
    } catch {
      window.alert("Не удалось загрузить фотографию. Попробуйте ещё раз.");
    } finally {
      setUploadingPhotos((current) =>
        current.map((uploading, photoIndex) =>
          photoIndex === index ? false : uploading,
        ),
      );
    }
  };
  const movePhoto = (from: number, direction: -1 | 1) => {
    const to = from + direction;
    if (to < 0 || to >= photos.length || !photos[from] || !photos[to]) return;
    setPhotos((current) => {
      const next = [...current];
      [next[from], next[to]] = [next[to], next[from]];
      return next;
    });
  };
  const swapPhotos = (from: number, to: number) => {
    if (from === to || !photos[from] || !photos[to]) return;
    setPhotos((current) => {
      const next = [...current];
      [next[from], next[to]] = [next[to], next[from]];
      return next;
    });
    setPhotoTransforms((current) => {
      const next = [...current];
      [next[from], next[to]] = [next[to], next[from]];
      return next;
    });
  };
  const updatePhotoTransform = (
    index: number,
    field: "offsetX" | "offsetY",
    value: number,
  ) => {
    setPhotoTransforms((current) =>
      current.map((transform, transformIndex) =>
        transformIndex === index ? { ...transform, [field]: value } : transform,
      ),
    );
  };
  return (
    <div className="accommodation-modal-backdrop">
      <form
        className="accommodation-modal"
        onSubmit={(event) => {
          event.preventDefault();
          if (isUploading) return;
          const data = new FormData(event.currentTarget);
          const cancellation: SavedAccommodation = {
            id: initial?.id || crypto.randomUUID(),
            name: name || "Новое жильё",
            city: city || "Город не указан",
            dates: `${data.get("checkIn")} – ${data.get("checkOut")}`,
            days: 30,
            deadline: String(data.get("freeCancellation") || ""),
            progress: 42,
            status,
            price: String(data.get("price") || ""),
            bookingUrl: String(data.get("bookingUrl") || ""),
            details: String(data.get("details") || ""),
            photos: photos.filter(Boolean),
            photoTransforms: photos.flatMap((photo, index) =>
              photo ? [photoTransforms[index]] : [],
            ),
          };
          onSaved?.(cancellation);
          onClose();
        }}
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <h2>{initial ? "Редактировать жильё" : "Новое жильё"}</h2>
          <button type="button" onClick={onClose} disabled={isUploading}>
            ×
          </button>
        </header>
        <section>
          <b>Фотографии</b>
          <div className="accommodation-upload">
            {photos.map((photo, index) => (
              <label
                className={`${index === 0 ? "accommodation-cover " : ""}${draggedPhoto === index ? "dragging" : ""}`}
                draggable={Boolean(photo)}
                onDragStart={(event) => {
                  if (!photo) return;
                  setDraggedPhoto(index);
                  event.dataTransfer.effectAllowed = "move";
                }}
                onDragEnd={() => setDraggedPhoto(null)}
                onDragOver={(event) => {
                  event.preventDefault();
                  event.dataTransfer.dropEffect = draggedPhoto === null ? "copy" : "move";
                }}
                onDrop={(event) => {
                  event.preventDefault();
                  if (draggedPhoto !== null) swapPhotos(draggedPhoto, index);
                  else void uploadPhoto(event.dataTransfer.files[0], index);
                  setDraggedPhoto(null);
                }}
                key={index}
              >
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={(event) => {
                    void uploadPhoto(event.target.files?.[0], index);
                    event.target.value = "";
                  }}
                />
                {photo ? (
                  <span
                    className="accommodation-preview-frame"
                    style={{
                      backgroundImage: `url(${photo})`,
                      backgroundPosition: `${photoTransforms[index].offsetX}% ${photoTransforms[index].offsetY}%`,
                    }}
                  />
                ) : (
                  <>
                    ▧
                    <span>
                      {uploadingPhotos[index]
                        ? "Загружаем..."
                        : index === 0
                          ? "Обложка — перетащите фото"
                          : "＋ Фото"}
                      <br />
                      <u>or browse files</u>
                    </span>
                  </>
                )}
              </label>
            ))}
          </div>
          {photos.some(Boolean) && (
            <div className="accommodation-photo-order">
              {photos.map(
                (photo, index) =>
                  photo && (
                    <div key={photo}>
                      <img src={photo} alt={`Фото ${index + 1}`} />
                      <span>{index === 0 ? "Обложка" : `Фото ${index + 1}`}</span>
                      <button
                        type="button"
                        onClick={() => movePhoto(index, -1)}
                        disabled={index === 0 || !photos[index - 1]}
                      >
                        ←
                      </button>
                      <button
                        type="button"
                        onClick={() => movePhoto(index, 1)}
                        disabled={index === photos.length - 1 || !photos[index + 1]}
                      >
                        →
                      </button>
                      <label>
                        По горизонтали
                        <input
                          type="range"
                          min="0"
                          max="100"
                          value={photoTransforms[index].offsetX}
                          onChange={(event) =>
                            updatePhotoTransform(
                              index,
                              "offsetX",
                              Number(event.target.value),
                            )
                          }
                        />
                      </label>
                      <label>
                        По вертикали
                        <input
                          type="range"
                          min="0"
                          max="100"
                          value={photoTransforms[index].offsetY}
                          onChange={(event) =>
                            updatePhotoTransform(
                              index,
                              "offsetY",
                              Number(event.target.value),
                            )
                          }
                        />
                      </label>
                    </div>
                  ),
              )}
            </div>
          )}
        </section>
        <section>
          <b>Статус</b>
          <div className="accommodation-form-statuses">
            {["хочу", "бронь", "оплачено", "пожили"].map((item) => (
              <button
                type="button"
                className={status === item ? "active" : ""}
                onClick={() => setStatus(item)}
                key={item}
              >
                {item}
              </button>
            ))}
          </div>
        </section>
        <label>
          Название
          <input
            name="name"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </label>
        <div className="accommodation-form-grid">
          <label>
            Город
            <input
              name="city"
              list="accommodation-cities"
              value={city}
              onChange={(event) => setCity(event.target.value)}
              placeholder="Начните вводить город"
              autoComplete="off"
            />
            <datalist id="accommodation-cities">
              {accommodationCities.map((item) => (
                <option value={item} key={item} />
              ))}
            </datalist>
          </label>
          <label>
            Цена
            <input name="price" defaultValue={initial?.price || "€434,98"} />
          </label>
          <label>
            Заезд
            <input name="checkIn" type="date" defaultValue={dateParts.checkIn} />
          </label>
          <label>
            Выезд
            <input name="checkOut" type="date" defaultValue={dateParts.checkOut} />
          </label>
        </div>
        <label>
          Бесплатная отмена до
          <input
            name="freeCancellation"
            type="date"
            defaultValue={initial?.deadline || "2026-08-25"}
          />
        </label>
        <label>
          Ссылка на Booking
          <input name="bookingUrl" defaultValue={initial?.bookingUrl || "https://booking.com/..."} />
        </label>
        <label>
          Адрес / заметка
          <textarea
            name="details"
            defaultValue={initial?.details || "Апартаменты с 1 спальней, до 4 гостей."}
          />
        </label>
        <footer>
          <button type="button" onClick={onClose} disabled={isUploading}>
            Отмена
          </button>
          <button className="accent" disabled={isUploading}>
            {isUploading ? "Загружаем фото..." : "Сохранить жильё"}
          </button>
        </footer>
      </form>
    </div>
  );
}

function AccommodationList({
  stays,
  onChange,
}: {
  stays: SavedAccommodation[];
  onChange: (accommodations: SavedAccommodation[]) => void;
}) {
  const [filter, setFilter] = useState("Все");
  const [activePhotos, setActivePhotos] = useState<Record<string, number>>({});
  const [expandedPhoto, setExpandedPhoto] = useState<{
    photos: string[];
    index: number;
  } | null>(null);
  const [statuses, setStatuses] = useState<Record<string, string>>(() =>
    Object.fromEntries(stays.map((stay) => [stay.name, stay.status])),
  );
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<SavedAccommodation | null>(null);
  const saveStay = (stay: SavedAccommodation) => {
    const index = stays.findIndex((item) => item.id === stay.id);
    onChange(
      index === -1
        ? [...stays, stay]
        : stays.map((item) => (item.id === stay.id ? stay : item)),
    );
    setStatuses((current) => ({ ...current, [stay.name]: stay.status }));
  };
  const visible = stays.filter(
    (stay) => filter === "Все" || statuses[stay.name] === filter,
  );
  const statusLabels = ["хочу", "бронь", "оплачено", "пожили"];
  return (
    <>
      <section className="accommodation-page">
        <header className="accommodation-heading">
          <h2>Жильё</h2>
          <button className="accent" onClick={() => setAdding(true)}>
            ＋ Добавить жильё
          </button>
        </header>
        <div className="accommodation-tabs">
          <button className="active">Список жилья</button>
          <button onClick={() => setFilter("отмена")}>Отмена</button>
        </div>
        <div className="accommodation-filters">
          {["Все", "хочу", "бронь", "оплачено"].map((item) => (
            <button
              className={filter === item ? "active" : ""}
              onClick={() => setFilter(item)}
              key={item}
            >
              {item === "Все"
                ? `Все · ${stays.length}`
                : item === "бронь"
                  ? "Забронировано"
                  : item[0].toUpperCase() + item.slice(1)}
            </button>
          ))}
        </div>
        <div className="accommodation-grid">
          {visible.map((stay, index) => {
            const photos = stay.photos || [];
            const photoIndex = (activePhotos[stay.name] || 0) % Math.max(photos.length, 1);
            const photoTransform = stay.photoTransforms?.[photoIndex] || {
              offsetX: 50,
              offsetY: 50,
            };
            const changePhoto = (offset: number) => {
              if (photos.length < 2) return;
              setActivePhotos((current) => ({
                ...current,
                [stay.name]: (photoIndex + offset + photos.length) % photos.length,
              }));
            };
            return (
              <article
                className={`accommodation-card c${index % 6}`}
                key={stay.name}
              >
              <div
                className="accommodation-photo"
                onClick={() => {
                  if (photos[photoIndex])
                    setExpandedPhoto({ photos, index: photoIndex });
                }}
                style={
                  photos[photoIndex]
                    ? {
                        backgroundImage: `url(${photos[photoIndex]})`,
                        backgroundPosition: `${photoTransform.offsetX ?? 50}% ${photoTransform.offsetY ?? 50}%`,
                      }
                    : undefined
                }
              >
                <span className={`stay-badge ${statuses[stay.name]}`}>
                  {statuses[stay.name]}
                </span>
                <button
                  type="button"
                  className="accommodation-edit"
                  aria-label="Редактировать жильё"
                  onClick={(event) => {
                    event.stopPropagation();
                    setEditing(stay as SavedAccommodation);
                  }}
                >
                  ⋯
                </button>
                <button
                  type="button"
                  className="accommodation-photo-previous"
                  aria-label="Предыдущее фото"
                  onClick={(event) => {
                    event.stopPropagation();
                    changePhoto(-1);
                  }}
                  disabled={photos.length < 2}
                >
                  ‹
                </button>
                <button
                  type="button"
                  className="accommodation-photo-next"
                  aria-label="Следующее фото"
                  onClick={(event) => {
                    event.stopPropagation();
                    changePhoto(1);
                  }}
                  disabled={photos.length < 2}
                >
                  ›
                </button>
                {photos.length > 1 && (
                  <i>{photos.map((_, photo) => photo === photoIndex ? "●" : "○").join(" ")}</i>
                )}
              </div>
              <div className="accommodation-body">
                <p>
                  {cityFlag(stay.city)} {stay.city}
                </p>
                <h3>{stay.name}</h3>
                <div className="stay-price">
                  <span>{formatAccommodationDates(stay.dates)}</span>
                  <b>{formatAccommodationPrice(stay.price)}</b>
                </div>
                <div className="stay-statuses">
                  {statusLabels.map((item) => (
                    <button
                      className={
                        statuses[stay.name] === item ? `active ${item}` : ""
                      }
                      onClick={() =>
                        setStatuses({ ...statuses, [stay.name]: item })
                      }
                      key={item}
                    >
                      {item}
                    </button>
                  ))}
                </div>
                <small>{stay.details}</small>
                <footer>
                  <a
                    href={externalUrl(stay.bookingUrl || "")}
                    target="_blank"
                    rel="noreferrer"
                  >
                    Ссылка на Букинг →
                  </a>
                  <button
                    type="button"
                      onClick={() =>
                        onChange(
                          stays.filter(
                            (item) => item.id !== (stay as SavedAccommodation).id,
                          ),
                        )
                      }
                  >
                    удалить
                  </button>
                </footer>
              </div>
              </article>
            );
          })}
        </div>
        {!visible.length && (
          <p className="accommodation-empty">Жильё пока не добавлено.</p>
        )}
      </section>
      {adding && (
        <AccommodationForm
          onClose={() => setAdding(false)}
          onSaved={(stay) => {
            saveStay(stay);
          }}
        />
      )}
      {editing && (
        <AccommodationForm
          initial={editing}
          onClose={() => setEditing(null)}
          onSaved={(stay) => {
            saveStay(stay);
            setEditing(null);
          }}
        />
      )}
      {expandedPhoto && (
        <div
          className="accommodation-photo-lightbox"
          role="dialog"
          aria-modal="true"
          aria-label="Просмотр фотографии жилья"
          onClick={() => setExpandedPhoto(null)}
        >
          <img
            src={expandedPhoto.photos[expandedPhoto.index]}
            alt="Фотография жилья"
            onClick={(event) => event.stopPropagation()}
          />
          {expandedPhoto.photos.length > 1 && (
            <>
              <button
                className="lightbox-previous"
                type="button"
                aria-label="Предыдущее фото"
                onClick={(event) => {
                  event.stopPropagation();
                  setExpandedPhoto((current) =>
                    current
                      ? {
                          ...current,
                          index:
                            (current.index - 1 + current.photos.length) %
                            current.photos.length,
                        }
                      : null,
                  );
                }}
              >
                ‹
              </button>
              <button
                className="lightbox-next"
                type="button"
                aria-label="Следующее фото"
                onClick={(event) => {
                  event.stopPropagation();
                  setExpandedPhoto((current) =>
                    current
                      ? {
                          ...current,
                          index: (current.index + 1) % current.photos.length,
                        }
                      : null,
                  );
                }}
              >
                ›
              </button>
            </>
          )}
          <button
            className="lightbox-close"
            type="button"
            onClick={() => setExpandedPhoto(null)}
          >
            ×
          </button>
        </div>
      )}
    </>
  );
}

function CancellationPage({
  accommodations,
  onShowList,
  onAdd,
}: {
  accommodations: SavedAccommodation[];
  onShowList: () => void;
  onAdd: () => void;
}) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const cancellationList = accommodations
    .map((stay) => {
      const deadline = new Date(`${stay.deadline}T00:00:00`);
      const days = Number.isNaN(deadline.getTime())
        ? 0
        : Math.max(0, Math.ceil((deadline.getTime() - today.getTime()) / 86400000));
      return {
        ...stay,
        days,
        deadline: Number.isNaN(deadline.getTime())
          ? "Дата не указана"
          : `до ${new Intl.DateTimeFormat("ru-RU", {
              day: "numeric",
              month: "long",
              year: "numeric",
            }).format(deadline)}`,
        progress: Math.max(5, Math.min(100, (days / 90) * 100)),
      };
    })
    .sort((first, second) => first.days - second.days);
  const freeCount = cancellationList.filter((stay) => stay.days > 7).length;
  const soonCount = cancellationList.filter(
    (stay) => stay.days > 0 && stay.days <= 7,
  ).length;
  const paidCount = cancellationList.filter((stay) => stay.days === 0).length;
  return (
    <section className="accommodation-page cancellation-page">
      <header className="accommodation-heading">
        <h2>Жильё</h2>
        <button className="accent" onClick={onAdd}>
          ＋ Добавить жильё
        </button>
      </header>
      <div className="accommodation-tabs">
        <button onClick={onShowList}>Список жилья</button>
        <button className="active">Отмена</button>
      </div>
      <div className="cancellation-intro">
        <p>
          Сроки бесплатной отмены по каждому жилью — по возрастанию срочности.
        </p>
        <button>↕ Сначала ближайшие</button>
      </div>
      <div className="cancellation-summary">
        <article>
          <b>{freeCount}</b>
          <span className="free">● Бесплатно ещё</span>
        </article>
        <article>
          <b>{soonCount}</b>
          <span className="soon">● Скоро платно</span>
        </article>
        <article>
          <b>{paidCount}</b>
          <span className="paid">● Уже платно</span>
        </article>
      </div>
      <div className="cancellation-list">
        {cancellationList.map((stay) => (
          <article key={stay.name}>
            <div>
              <h3>
                {cityFlag(stay.city)} {stay.name}
              </h3>
              <p>
                {stay.city} · {stay.dates}
              </p>
            </div>
            <div className="cancellation-deadline">
              <b>
                {stay.days}
                <small>дн.</small>
              </b>
              <span>{stay.deadline}</span>
            </div>
            <footer>
              <i>
                <em style={{ width: `${stay.progress}%` }} />
              </i>
              <strong>БЕСПЛАТНАЯ ОТМЕНА</strong>
            </footer>
          </article>
        ))}
      </div>
    </section>
  );
}

function Accommodation({
  trip,
  onUpdateTrip,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
}) {
  const [showCancellation, setShowCancellation] = useState(false);
  const [adding, setAdding] = useState(false);
  if (showCancellation)
    return (
      <>
        {
          <CancellationPage
            accommodations={trip.accommodations || []}
            onShowList={() => setShowCancellation(false)}
            onAdd={() => setAdding(true)}
          />
        }
        {adding && <AccommodationForm onClose={() => setAdding(false)} />}
      </>
    );
  return (
    <div
      onClickCapture={(event) => {
        if (
          event.target instanceof HTMLButtonElement &&
          event.target.closest(".accommodation-tabs") &&
          event.target.textContent === "Отмена"
        )
          setShowCancellation(true);
      }}
    >
      <AccommodationList
        stays={trip.accommodations || []}
        onChange={(accommodations) =>
          onUpdateTrip({ ...trip, accommodations })
        }
      />
    </div>
  );
}

function Bookings() {
  const tickets = [
    ["Колизей", "13 сен · 09:00 · 3 взр.", "5 400 ₽"],
    ["Галерея Уффици", "15 сен · 16:00 · 3 взр.", "4 200 ₽"],
    ["Дворец Дожей", "17 сен · 16:30 · 3 взр.", "3 900 ₽"],
  ];
  return (
    <div className="stack">
      <SectionHead title="Жильё" />
      {[
        ["Hotel Artemide, Рим", "12–15 сен · 3 ночи", "42 300 ₽"],
        ["B&B Fiori, Флоренция", "15–17 сен · 2 ночи", "24 800 ₽"],
      ].map((item) => (
        <article className="booking" key={item[0]}>
          <div className="thumb" />
          <div>
            <h3>{item[0]}</h3>
            <p>{item[1]} · 2-местный номер</p>
            <small>
              Подтверждение <b>#ART-8842</b>　 Стоимость <b>{item[2]}</b>
            </small>
          </div>
          <span>Оплачено</span>
        </article>
      ))}
      <SectionHead title="Транспорт" />
      <div className="transport">
        {[
          [
            "Москва → Рим",
            "12 сен · 08:40–11:55 · Аэрофлот SU-2402",
            "31 200 ₽",
          ],
          [
            "Рим → Флоренция",
            "15 сен · 08:10–09:45 · Frecciarossa 9512",
            "3 900 ₽",
          ],
          [
            "Флоренция → Венеция",
            "17 сен · 09:20–11:25 · Frecciarossa 9420",
            "4 100 ₽",
          ],
        ].map(([route, details, price]) => (
          <div key={route}>
            <i />
            <span>
              <b>{route}</b>
              <small>{details}</small>
            </span>
            <b>{price}</b>
          </div>
        ))}
      </div>
      <SectionHead title="Билеты и брони" />
      <div className="ticket-grid">
        {tickets.map(([title, details, price]) => (
          <article key={title}>
            <h3>{title}</h3>
            <p>{details}</p>
            <b>{price}</b>
          </article>
        ))}
      </div>
    </div>
  );
}

function SectionHead({ title }: { title: string }) {
  return (
    <header className="section-head">
      <h2>{title}</h2>
      <button>＋ Добавить</button>
    </header>
  );
}

function LegacyBudget() {
  const [adding, setAdding] = useState(false);
  const cats = [
    ["Жильё", 0, "0 ₽"],
    ["Транспорт", 0, "0 ₽"],
    ["Еда и рестораны", 0, "0 ₽"],
    ["Активности и билеты", 0, "0 ₽"],
    ["Прочее", 0, "0 ₽"],
  ] as const;
  return (
    <>
      <div className="budget">
        <div className="budget-actions">
          <h2>Бюджет поездки</h2>
          <button className="accent" onClick={() => setAdding(true)}>
            ＋ Добавить трату
          </button>
        </div>
        <div className="budget-cards">
          <article>
            <span>Общий бюджет</span>
            <b>0 ₽</b>
          </article>
          <article className="accent-card">
            <span>Запланировано</span>
            <b>0 ₽</b>
            <small>Нет трат</small>
          </article>
          <article>
            <span>Осталось</span>
            <b>0 ₽</b>
            <small>Добавьте лимит бюджета</small>
          </article>
        </div>
        <div className="budget-grid">
          <article className="panel">
            <h2>По категориям</h2>
            {cats.map(([name, pct, amount]) => (
              <div className="budget-row" key={name}>
                <p>
                  <b>{name}</b>
                  <span>{amount}</span>
                </p>
                <div>
                  <i style={{ width: `${pct}%` }} />
                </div>
              </div>
            ))}
          </article>
          <article className="panel">
            <h2>Разделить расходы</h2>
            <p>Добавьте траты, чтобы рассчитать баланс участников.</p>
          </article>
        </div>
      </div>
      {adding && (
        <div
          className="restaurant-modal-backdrop"
          onClick={() => setAdding(false)}
        >
          <form
            className="restaurant-modal budget-modal"
            onSubmit={(event) => {
              event.preventDefault();
              setAdding(false);
            }}
            onClick={(event) => event.stopPropagation()}
          >
            <header>
              <h2>Новая трата</h2>
              <button type="button" onClick={() => setAdding(false)}>
                ×
              </button>
            </header>
            <label>
              Название
              <input autoFocus placeholder="Например, билеты в музей" />
            </label>
            <div className="restaurant-form-grid">
              <label>
                Категория
                <select defaultValue="Активности и билеты">
                  <option>Жильё</option>
                  <option>Транспорт</option>
                  <option>Еда и рестораны</option>
                  <option>Активности и билеты</option>
                  <option>Прочее</option>
                </select>
              </label>
              <label>
                Сумма
                <input placeholder="0 ₽" inputMode="numeric" />
              </label>
              <label>
                Оплатил
                <select defaultValue="Общее">
                  <option>Общее</option>
                  <option>Анна</option>
                  <option>Максим</option>
                  <option>Дарья</option>
                </select>
              </label>
              <label>
                Дата
                <input type="date" />
              </label>
            </div>
            <footer>
              <button type="button" onClick={() => setAdding(false)}>
                Отмена
              </button>
              <button className="accent">Добавить трату</button>
            </footer>
          </form>
        </div>
      )}
    </>
  );
}

function ExpenseForm({
  onClose,
  onSave,
  initial,
  currency,
}: {
  onClose: () => void;
  onSave: (expense: BudgetExpense) => void;
  initial?: BudgetExpense;
  currency: BudgetCurrency;
}) {
  const [category, setCategory] = useState(initial?.category || "Еда и рестораны");
  const [scope, setScope] = useState<BudgetScope>(initial?.scope || "общий");
  return (
    <div className="expense-modal-backdrop" onClick={onClose}>
      <form
        className="expense-modal"
        onSubmit={(event) => {
          event.preventDefault();
          const form = new FormData(event.currentTarget);
          const amount = Number(String(form.get("amount") || "").replace(",", "."));
          const name = String(form.get("name") || "").trim();
          if (!name || !Number.isFinite(amount) || amount <= 0) return;
          onSave({
            id: initial?.id || crypto.randomUUID(),
            name,
            amount: amount / budgetCurrencies[currency].rate,
            category,
            scope,
            paidBy: String(form.get("paidBy") || initial?.paidBy || "Общее").trim() || "Общее",
            date: String(form.get("date") || "") || undefined,
          });
          onClose();
        }}
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <h2>{initial ? "Редактировать трату" : "Новая трата"}</h2>
          <button type="button" onClick={onClose}>
            ×
          </button>
        </header>
        <label>
          Название<input name="name" autoFocus defaultValue={initial?.name} placeholder="Напр. жильё в Равенсбурге" />
        </label>
        <div className="expense-form-grid">
          <label>
            Сумма, {budgetCurrencies[currency].label}
            <input
              name="amount"
              inputMode="decimal"
              defaultValue={
                initial
                  ? initial.amount * budgetCurrencies[currency].rate
                  : undefined
              }
              placeholder="0"
            />
          </label>
          <label>
            Кто платил
            <input name="paidBy" defaultValue={initial?.paidBy || "Общее"} placeholder="Например, Анна" />
          </label>
          <label>Дата<input name="date" type="date" defaultValue={initial?.date} /></label>
        </div>
        <section>
          <b>Категория</b>
          <div>
            {["Жильё", "Транспорт", "Еда и рестораны", "Активности и билеты", "Прочее"].map(
              (item) => (
                <button
                  type="button"
                  className={category === item ? "active" : ""}
                  onClick={() => setCategory(item)}
                  key={item}
                >
                  {item}
                </button>
              ),
            )}
          </div>
        </section>
        <section>
          <b>Тип бюджета</b>
          <div>
            {(["общий", "семья", "личный"] as BudgetScope[]).map((item) => (
              <button
                type="button"
                className={scope === item ? "active" : ""}
                onClick={() => setScope(item)}
                key={item}
              >
                {item === "общий" ? "Общий" : item === "семья" ? "Семья" : "Личный"}
              </button>
            ))}
          </div>
        </section>
        <footer>
          <button type="button" onClick={onClose}>
            Отмена
          </button>
          <button className="accent">{initial ? "Сохранить" : "Добавить"}</button>
        </footer>
      </form>
    </div>
  );
}

function BudgetSplitForm({
  initial,
  total,
  currency,
  onClose,
  onSave,
}: {
  initial: BudgetSplit;
  total: number;
  currency: BudgetCurrency;
  onClose: () => void;
  onSave: (split: BudgetSplit) => void;
}) {
  const [groups, setGroups] = useState(initial.groups);
  const peopleTotal = Math.max(1, groups.reduce((sum, group) => sum + group.people, 0));
  return (
    <div className="expense-modal-backdrop" onClick={onClose}>
      <form
        className="expense-modal budget-split-modal"
        onSubmit={(event) => {
          event.preventDefault();
          onSave({ groups: groups.map((group) => ({ ...group, people: Math.max(1, group.people) })) });
          onClose();
        }}
        onClick={(event) => event.stopPropagation()}
      >
        <header><h2>Разделить бюджет</h2><button type="button" onClick={onClose}>×</button></header>
        <div className="budget-groups">
          {groups.map((group, index) => (
            <div className="expense-form-grid" key={group.id}>
              <label>{index + 1}-я группа<select value={group.name} onChange={(event) => setGroups((current) => current.map((item) => item.id === group.id ? { ...item, name: event.target.value } : item))}><option>Моя семья</option><option>Друг</option><option>Друзья</option><option>Родители</option><option>Другая группа</option></select></label>
              <label>Количество людей<input type="number" min="1" value={group.people} onChange={(event) => setGroups((current) => current.map((item) => item.id === group.id ? { ...item, people: Number(event.target.value) } : item))} /></label>
              {groups.length > 1 && <button type="button" className="budget-remove-group" aria-label="Удалить группу" onClick={() => setGroups((current) => current.filter((item) => item.id !== group.id))}>×</button>}
            </div>
          ))}
          <button type="button" className="budget-add-group" onClick={() => setGroups((current) => [...current, { id: crypto.randomUUID(), name: "Другая группа", people: 1 }])}>＋ Добавить группу</button>
        </div>
        <section className="budget-split-preview">
          {groups.map((group) => <span key={group.id}>{group.name} · {group.people} чел.<b>{formatBudgetAmount(total * group.people / peopleTotal, currency)}</b></span>)}
        </section>
        <footer><button type="button" onClick={onClose}>Отмена</button><button className="accent">Сохранить</button></footer>
      </form>
    </div>
  );
}

function Budget({
  trip,
  onUpdateTrip,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
}) {
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState<BudgetExpense | null>(null);
  const [splitting, setSplitting] = useState(false);
  const expenses = trip.budgetExpenses || [];
  const currency = trip.budgetCurrency || "EUR";
  const split = trip.budgetSplit || {
    groups: [
      { id: "family", name: "Моя семья", people: 2 },
      { id: "friend", name: "Друг", people: 1 },
    ],
  };
  const categories = ["Жильё", "Транспорт", "Еда и рестораны", "Активности и билеты", "Прочее"];
  const saveExpense = (expense: BudgetExpense) => {
    const next = expenses.some((item) => item.id === expense.id)
      ? expenses.map((item) => item.id === expense.id ? expense : item)
      : [...expenses, expense];
    onUpdateTrip({ ...trip, budgetExpenses: next });
  };
  const scopeTotal = (scope: BudgetScope) =>
    expenses.filter((expense) => expense.scope === scope).reduce((sum, expense) => sum + expense.amount, 0);
  const formatAmount = (amount: number) => formatBudgetAmount(amount, currency);
  const sharedTotal = scopeTotal("общий");
  const splitPeopleTotal = Math.max(1, split.groups.reduce((sum, group) => sum + group.people, 0));
  const saveSplit = (next: BudgetSplit) => {
    onUpdateTrip({ ...trip, budgetSplit: next });
  };
  return (
    <div className="budget">
      <div className="budget-actions">
        <h2>Бюджет поездки</h2>
        <div>
          <div className="budget-currency" role="group" aria-label="Валюта">
            {(Object.keys(budgetCurrencies) as BudgetCurrency[]).map((item) => (
              <button
                type="button"
                className={currency === item ? "active" : ""}
                onClick={() => onUpdateTrip({ ...trip, budgetCurrency: item })}
                key={item}
              >
                {budgetCurrencies[item].label}
              </button>
            ))}
          </div>
          <button className="secondary" onClick={() => setSplitting(true)}>Разделить бюджет</button>
          <button className="accent" onClick={() => setAdding(true)}>＋ Добавить трату</button>
        </div>
      </div>
      <div className="budget-cards">
        <article className="accent-card">
          <span>Общий бюджет</span>
          <b>{formatAmount(sharedTotal)}</b>
          <small>{expenses.filter((expense) => expense.scope === "общий").length} общих трат</small>
        </article>
        {split.groups.map((group) => <article key={group.id}><span>{group.name}</span><b>{formatAmount(sharedTotal * group.people / splitPeopleTotal)}</b><small>Доля из общих трат</small></article>)}
      </div>
      <div className="budget-grid">
        <article className="panel">
          <header className="budget-panel-heading">
            <h2>По категориям</h2>
            <button
              type="button"
              className="budget-panel-edit"
              aria-label="Редактировать трату"
              disabled={!expenses.length}
              onClick={() => setEditing(expenses[0])}
            >
              ✎
            </button>
          </header>
          {categories.map((category) => {
            const total = expenses.filter((expense) => expense.category === category).reduce((sum, expense) => sum + expense.amount, 0);
            const all = expenses.reduce((sum, expense) => sum + expense.amount, 0);
            return <div className="budget-row" key={category}><p><b>{category}</b><span>{formatAmount(total)}</span></p><div><i style={{ width: `${all ? (total / all) * 100 : 0}%` }} /></div></div>;
          })}
        </article>
        <article className="panel">
          <h2>Траты</h2>
          {expenses.length ? expenses.map((expense) => <div className="split" key={expense.id}><span><b>{expense.name}</b><small>{expense.scope === "общий" ? "Общий" : expense.scope === "семья" ? "Семья" : "Личный"} · {expense.paidBy}</small></span><b>{formatAmount(expense.amount)}</b></div>) : <p>Добавьте первую трату и выберите: общий, семейный или личный бюджет.</p>}
        </article>
      </div>
      {adding && <ExpenseForm currency={currency} onClose={() => setAdding(false)} onSave={saveExpense} />}
      {editing && <ExpenseForm currency={currency} initial={editing} onClose={() => setEditing(null)} onSave={saveExpense} />}
      {splitting && <BudgetSplitForm currency={currency} initial={split} total={sharedTotal} onClose={() => setSplitting(false)} onSave={saveSplit} />}
    </div>
  );
}

function Photos() {
  const [query, setQuery] = useState("");
  const [uploaded, setUploaded] = useState<
    { id: string; image: string; date?: string; city?: string }[]
  >([]);
  const input = useRef<HTMLInputElement>(null);
  const samples = [
    ["Колизей", "Рим", "13 сен"],
    ["Пантеон", "Рим", "13 сен"],
    ["Фонтан Треви", "Рим", "14 сен"],
    ["Санта-Мария-дель-Фьоре", "Флоренция", "15 сен"],
    ["Понте Веккьо", "Флоренция", "16 сен"],
    ["Сады Боболи", "Флоренция", "16 сен"],
    ["Площадь Сан-Марко", "Венеция", "17 сен"],
    ["Гранд-канал", "Венеция", "17 сен"],
    ["Остров Бурано", "Венеция", "18 сен"],
  ] as const;
  const uploadPhotos = async (files: FileList | null) => {
    if (!files?.length) return;
    const photos = await Promise.all(
      Array.from(files)
        .filter((file) => file.type.startsWith("image/"))
        .map(async (file) => ({
          id: crypto.randomUUID(),
          image: URL.createObjectURL(file),
          ...(await readPhotoMetadata(file)),
        })),
    );
    setUploaded((current) => [...photos, ...current]);
  };
  const visibleUploads = uploaded.filter((photo) =>
    `${photo.city || ""} ${photo.date || ""}`
      .toLowerCase()
      .includes(query.trim().toLowerCase()),
  );
  const visibleSamples = samples.filter(([name, place, date]) =>
    `${name} ${place} ${date}`
      .toLowerCase()
      .includes(query.trim().toLowerCase()),
  );
  return (
    <section className="photos-page">
      <input
        ref={input}
        className="photo-file-input"
        type="file"
        accept="image/*"
        multiple
        onChange={(event) => {
          void uploadPhotos(event.target.files);
          event.target.value = "";
        }}
      />
      <header className="photos-heading">
        <div>
          <h2>Фотоальбом</h2>
          <p>48 фото · снимки всех участников поездки</p>
        </div>
        <button className="accent" onClick={() => input.current?.click()}>
          ↑ Загрузить
        </button>
      </header>
      <label className="photo-search">
        <span>⌕</span>
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Поиск по дате или месту"
        />
      </label>
      <div className="photo-grid">
        {visibleUploads.map((photo) => (
          <div
            className="photo uploaded-photo"
            style={{ backgroundImage: `url(${photo.image})` }}
            key={photo.id}
          >
            <span>
              {photo.city || "Место не определено"}
              {photo.date ? ` · ${photo.date}` : " · дата не определена"}
            </span>
          </div>
        ))}
        {visibleSamples.map(([name, place, date], index) => (
          <div
            className={`photo p${index % 6} ${index === 0 ? "hero-photo" : ""}`}
            key={name}
          >
            <span className="photo-label">
              {place} · {date}
            </span>
          </div>
        ))}
      </div>
    </section>
  );
}

function Members({
  trip,
  onUpdateTrip,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
}) {
  const [people, setPeople] = useState<TripMember[]>(trip.members || []);
  const updatePeople = (update: (current: TripMember[]) => TripMember[]) => {
    setPeople((current) => {
      const next = update(current);
      onUpdateTrip({ ...trip, members: next });
      return next;
    });
  };
  const [inviteName, setInviteName] = useState("");
  const [email, setEmail] = useState("");
  const [inviteRole, setInviteRole] = useState<TripMember["role"]>("Редактор");
  const [inviteMessage, setInviteMessage] = useState("");
  const [sendingInvite, setSendingInvite] = useState(false);
  const [publicLinkEnabled, setPublicLinkEnabled] = useState(
    trip.publicLinkEnabled ?? true,
  );
  const [published, setPublished] = useState(trip.published ?? false);
  const [copyLabel, setCopyLabel] = useState("Копировать");
  const publicUrl = "ramingo.online/p/italy-8d-a1b2";
  const publicHref = `https://${publicUrl}`;
  const inviteMember = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedEmail = email.trim();
    if (!trimmedEmail) return;
    if (people.some((person) => person.email === trimmedEmail)) {
      setInviteMessage("Этот участник уже добавлен.");
      return;
    }
    setSendingInvite(true);
    setInviteMessage("");
    const name =
      inviteName.trim() || trimmedEmail.split("@")[0] || trimmedEmail;
    const redirectTo = `${window.location.origin}/?next=${encodeURIComponent(
      `/trips/${trip.id}/overview`,
    )}`;
    const addMember = () => {
      updatePeople((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          initials: name.slice(0, 2).toUpperCase(),
          name,
          email: trimmedEmail,
          role: inviteRole,
          tone: "blue",
        },
      ]);
      setInviteName("");
      setEmail("");
    };
    const {
      data: { session },
    } = await supabase.auth.getSession();
    if (!session) {
      setInviteMessage("Войдите в аккаунт, чтобы отправить приглашение.");
      setSendingInvite(false);
      return;
    }
    try {
      const { error } = await supabase.functions.invoke("send-invite", {
        body: {
          email: trimmedEmail,
          name,
          role: inviteRole,
          redirectTo,
          tripId: trip.id,
        },
      });
      if (error) {
        const response = error.context;
        const details = response instanceof Response
          ? await response.json().catch(() => null) as { error?: string } | null
          : null;
        throw new Error(details?.error || error.message);
      }
      addMember();
      setInviteMessage(`Приглашение отправлено на ${trimmedEmail}.`);
    } catch (error) {
      setInviteMessage(
        error instanceof Error
          ? error.message
          : "Не удалось отправить приглашение.",
      );
    } finally {
      setSendingInvite(false);
    }
  };
  const copyPublicLink = async () => {
    if (navigator.clipboard)
      await navigator.clipboard
        .writeText(publicHref)
        .catch(() => undefined);
    setCopyLabel("Скопировано");
    window.setTimeout(() => setCopyLabel("Копировать"), 1800);
  };
  return (
    <div className="members">
      <article className="panel">
        {people.map((person) => (
          <div className="member" key={person.id}>
            <Avatar tone={person.tone}>{person.initials}</Avatar>
            <span>
              <b>{person.name}</b>
              <small>{person.email}</small>
            </span>
            {person.role === "Владелец" ? (
              <span className="member-role">Владелец</span>
            ) : (
              <>
                <select
                  aria-label={`Роль ${person.name}`}
                  value={person.role}
                  onChange={(event) =>
                    updatePeople((current) =>
                      current.map((item) =>
                        item.id === person.id
                          ? {
                              ...item,
                              role: event.target.value as TripMember["role"],
                            }
                          : item,
                      ),
                    )
                  }
                >
                  <option>Редактор</option>
                  <option>Читатель</option>
                </select>
                <button
                  type="button"
                  className="member-remove"
                  onClick={() => {
                    updatePeople((current) =>
                      current.filter((item) => item.id !== person.id),
                    );
                    setInviteMessage(`${person.name} удалён из поездки.`);
                  }}
                >
                  Удалить
                </button>
              </>
            )}
          </div>
        ))}
        <form className="invite" onSubmit={(event) => void inviteMember(event)}>
          <input
            className="invite-name"
            value={inviteName}
            onChange={(event) => setInviteName(event.target.value)}
            placeholder="Имя участника"
            aria-label="Имя нового участника"
          />
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="e-mail нового участника"
            aria-label="E-mail нового участника"
          />
          <select
            value={inviteRole}
            onChange={(event) =>
              setInviteRole(event.target.value as TripMember["role"])
            }
            aria-label="Роль нового участника"
          >
            <option>Редактор</option>
            <option>Читатель</option>
          </select>
          <button className="accent" disabled={sendingInvite}>
            {sendingInvite ? "Отправляем..." : "Пригласить"}
          </button>
        </form>
        {inviteMessage && (
          <p className="member-invite-message" role="status">
            {inviteMessage}
          </p>
        )}
      </article>
      <article className="panel public-link">
        <h2>
          Публичная ссылка{" "}
          <button
            className={`link-toggle ${publicLinkEnabled ? "active" : ""}`}
            type="button"
            role="switch"
            aria-checked={publicLinkEnabled}
            aria-label="Включить публичную ссылку"
            onClick={() =>
              setPublicLinkEnabled((enabled) => {
                onUpdateTrip({ ...trip, publicLinkEnabled: !enabled });
                return !enabled;
              })
            }
          >
            <i />
          </button>
        </h2>
        <p>
          Любой, у кого есть ссылка, может просматривать маршрут без прав на
          редактирование.
        </p>
        <div>
          <code>{publicUrl}</code>
          <button
            type="button"
            onClick={() => void copyPublicLink()}
            disabled={!publicLinkEnabled}
          >
            {copyLabel}
          </button>
        </div>
        <div className="public-catalog">
          <span>
            <b>Опубликовать в каталоге</b>
            <small>Другие смогут найти и скопировать ваш маршрут</small>
          </span>
          <button
            type="button"
            onClick={() =>
              setPublished((value) => {
                onUpdateTrip({ ...trip, published: !value });
                return !value;
              })
            }
          >
            {published ? "Опубликовано" : "Опубликовать"}
          </button>
        </div>
      </article>
    </div>
  );
}

function TripCardEditor({
  trip,
  onUpdateTrip,
  onClose,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
  onClose: () => void;
}) {
  const photoInput = useRef<HTMLInputElement>(null);
  const [title, setTitle] = useState(trip.title);
  const [startDate, setStartDate] = useState(trip.startDate || "");
  const [endDate, setEndDate] = useState(trip.endDate || "");
  const [coverImage, setCoverImage] = useState(trip.coverImage || "");
  const [coverChanged, setCoverChanged] = useState(false);
  const selectCover = (file?: File) => {
    if (!file?.type.startsWith("image/")) return;
    void compressCoverPhoto(file)
      .then((image) => {
        setCoverImage(image);
        setCoverChanged(true);
      })
      .catch(() => window.alert("Не удалось обработать фотографию."));
  };
  const save = () => {
    if ((startDate && !endDate) || (!startDate && endDate)) {
      window.alert("Укажите дату начала и дату окончания путешествия.");
      return;
    }
    if (startDate && endDate && startDate > endDate) {
      window.alert("Дата окончания не может быть раньше даты начала.");
      return;
    }
    onUpdateTrip({
      ...trip,
      title: title.trim() || "Без названия",
      startDate,
      endDate,
      dates: startDate && endDate ? formatTripDates(startDate, endDate) : trip.dates,
      coverImage,
      coverPhotos: coverChanged
        ? coverImage
          ? [{ id: crypto.randomUUID(), image: coverImage }]
          : []
        : trip.coverPhotos,
    });
    onClose();
  };
  return (
    <div className="overview-editor-backdrop" onClick={onClose}>
      <section
        className="trip-card-editor"
        role="dialog"
        aria-modal="true"
        aria-labelledby="trip-card-editor-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <h2 id="trip-card-editor-title">Редактирование путешествия</h2>
          <button type="button" onClick={onClose} aria-label="Закрыть">
            ×
          </button>
        </header>
        <div>
          <label>
            Название путешествия
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Название путешествия"
            />
          </label>
          <section>
            <DatePicker
              label="Дата начала"
              value={startDate}
              onChange={setStartDate}
            />
            <DatePicker
              label="Дата окончания"
              value={endDate}
              onChange={setEndDate}
            />
          </section>
          {!startDate && !endDate && trip.dates && (
            <small>Текущие даты: {trip.dates}</small>
          )}
          <input
            ref={photoInput}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={(event) => {
              selectCover(event.target.files?.[0]);
              event.target.value = "";
            }}
          />
          <button
            type="button"
            className={`trip-cover-picker ${coverImage ? "has-image" : ""}`}
            style={
              coverImage ? { backgroundImage: `url(${coverImage})` } : undefined
            }
            onClick={() => photoInput.current?.click()}
          >
            <span>{coverImage ? "Изменить обложку" : "Добавить обложку"}</span>
          </button>
        </div>
        <footer>
          <button type="button" onClick={onClose}>
            Отмена
          </button>
          <button className="accent" type="button" onClick={save}>
            Сохранить
          </button>
        </footer>
      </section>
    </div>
  );
}

function OverviewEditor({
  trip,
  onUpdateTrip,
  onClose,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
  onClose: () => void;
}) {
  const input = useRef<HTMLInputElement>(null);
  const routeCities = Array.from(
    new Set(
      [
        ...(trip.cities || "").split(/[·,]/).map((city) => city.trim()),
        ...(trip.days || []).flatMap((day) =>
          day.roadLeg ? [day.roadLeg.from, day.roadLeg.to] : [],
        ),
      ].filter(Boolean),
    ),
  );
  const [cityIndex, setCityIndex] = useState(0);
  const [slideCity, setSlideCity] = useState(routeCities[0] || "");
  const [caption, setCaption] = useState("");
  const [selectedPhotos, setSelectedPhotos] = useState<string[]>([]);
  const selectedPhoto = selectedPhotos[0] || "";
  const [savedPhotos, setSavedPhotos] = useState<CoverPhoto[]>(() =>
    trip.coverPhotos?.length
      ? trip.coverPhotos
      : trip.coverImage
        ? [{ id: "legacy-cover", image: trip.coverImage }]
        : [],
  );
  const [activePhotoId, setActivePhotoId] = useState<string | null>(
    () =>
      trip.coverPhotos?.[0]?.id || (trip.coverImage ? "legacy-cover" : null),
  );
  const [draggedPhoto, setDraggedPhoto] = useState<number | null>(null);
  const [draggedNewPhoto, setDraggedNewPhoto] = useState<number | null>(null);
  const [textColor, setTextColor] = useState(trip.coverTextColor || "#ffffff");
  const [title, setTitle] = useState(trip.title);
  const [startDate, setStartDate] = useState(trip.startDate || "");
  const [endDate, setEndDate] = useState(trip.endDate || "");
  const [weatherCities, setWeatherCities] = useState(routeCities);
  const [mapPoints, setMapPoints] = useState(
    trip.overviewMapPoints || routeCities,
  );
  const city = routeCities[cityIndex] || "Город";
  const addMapPoint = () => {
    const point = window.prompt("Точка маршрута")?.trim();
    if (point) setMapPoints((points) => [...points, point]);
  };
  const addWeatherCity = () => {
    const nextCity = window.prompt("Город для погоды")?.trim();
    if (nextCity) setWeatherCities((cities) => [...cities, nextCity]);
  };
  const selectPhotos = (files?: FileList | File[] | null) => {
    const images = Array.from(files || []).filter((file) =>
      file.type.startsWith("image/"),
    );
    if (!images.length) return;
    void Promise.all(images.map(compressCoverPhoto))
      .then((images) => {
        const photos = images.map((image) => ({
          id: crypto.randomUUID(),
          image,
          city: routeCities[0] || "",
          description: "",
          textColor,
        }));
        setSavedPhotos((current) => [...current, ...photos]);
        setActivePhotoId(photos[0]?.id || null);
      })
      .catch(() => undefined);
  };
  const selectPhoto = (file?: File) => selectPhotos(file ? [file] : []);
  const save = () => {
    if ((startDate && !endDate) || (!startDate && endDate)) {
      window.alert("Укажите дату начала и дату окончания путешествия.");
      return;
    }
    if (startDate && endDate && startDate > endDate) {
      window.alert("Дата окончания не может быть раньше даты начала.");
      return;
    }
    onUpdateTrip({
      ...trip,
      title: title.trim() || "Без названия",
      startDate,
      endDate,
      dates: startDate && endDate ? formatTripDates(startDate, endDate) : trip.dates,
      cities: weatherCities.join(", "),
      coverImage: savedPhotos[0]?.image,
      coverPhotos: savedPhotos,
      coverTextColor: textColor,
      overviewMapPoints: mapPoints,
    });
    onClose();
  };
  const colors = ["#ffffff", "#f9d79c", "#121317", "#c8c6ff"];
  const activePhoto = savedPhotos.find((photo) => photo.id === activePhotoId);
  const updateActivePhoto = (changes: Partial<CoverPhoto>) =>
    setSavedPhotos((photos) =>
      photos.map((photo) =>
        photo.id === activePhotoId ? { ...photo, ...changes } : photo,
      ),
    );
  return (
    <div className="overview-editor-backdrop">
      <section
        className="overview-editor"
        role="dialog"
        aria-modal="true"
        aria-labelledby="overview-editor-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <h2 id="overview-editor-title">Редактирование главной</h2>
          <button type="button" onClick={onClose} aria-label="Закрыть">
            ×
          </button>
        </header>
        <div className="overview-editor-content">
          <section className="trip-profile-fields">
            <label>
              Название путешествия
              <input
                className="editor-field"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="Название путешествия"
              />
            </label>
            <div>
              <DatePicker
                label="Дата начала"
                value={startDate}
                onChange={setStartDate}
              />
              <DatePicker
                label="Дата окончания"
                value={endDate}
                onChange={setEndDate}
              />
            </div>
            {!startDate && !endDate && trip.dates && (
              <small>Текущие даты: {trip.dates}</small>
            )}
          </section>
          <div
            className={`editor-photo-drop ${activePhoto ? "has-photo" : ""}`}
            style={
              activePhoto
                ? {
                    backgroundImage: `linear-gradient(#11182733, #11182733), url(${activePhoto.image})`,
                  }
                : undefined
            }
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => {
              event.preventDefault();
              selectPhotos(event.dataTransfer.files);
            }}
          >
            <input
              ref={input}
              type="file"
              accept="image/*"
              multiple
              onChange={(event) => {
                selectPhotos(event.target.files);
                event.target.value = "";
              }}
            />
            {activePhoto ? (
              <span>Выбранный слайд</span>
            ) : (
              <>
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="m4 18 5-5 3 3 3-3 5 5" />
                </svg>
                <span>Перетащите фото города</span>
                <small>
                  or{" "}
                  <button type="button" onClick={() => input.current?.click()}>
                    browse files
                  </button>
                </small>
              </>
            )}
          </div>
          <p className="editor-photo-hint">
            Выберите миниатюру, чтобы указать город и факт о ней.
          </p>
          <div className="editor-photo-list">
            {savedPhotos.map((photo, index) => (
              <div
                className={`${draggedPhoto === index ? "dragging " : ""}${activePhotoId === photo.id ? "active" : ""}`}
                style={{ backgroundImage: `url(${photo.image})` }}
                title="Нажмите, чтобы редактировать; перетащите, чтобы изменить порядок"
                draggable
                onClick={() => setActivePhotoId(photo.id)}
                onDragStart={() => setDraggedPhoto(index)}
                onDragEnd={() => setDraggedPhoto(null)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={() => {
                  if (draggedPhoto === null || draggedPhoto === index) return;
                  setSavedPhotos((photos) => {
                    const next = [...photos];
                    const [moved] = next.splice(draggedPhoto, 1);
                    next.splice(index, 0, moved);
                    return next;
                  });
                  setDraggedPhoto(null);
                }}
                key={photo.id}
              >
                <button
                  type="button"
                  aria-label="Удалить фото"
                  onClick={(event) => {
                    event.stopPropagation();
                    setSavedPhotos((photos) =>
                      photos.filter((item) => item.id !== photo.id),
                    );
                    if (activePhotoId === photo.id) setActivePhotoId(null);
                  }}
                >
                  ×
                </button>
              </div>
            ))}
            <button
              type="button"
              className="editor-add-photos"
              onClick={() => input.current?.click()}
            >
              ＋ Добавить несколько фото
            </button>
          </div>
          <input
            className="editor-field"
            value={activePhoto?.city || ""}
            onChange={(event) =>
              updateActivePhoto({ city: event.target.value })
            }
            placeholder="Город"
            disabled={!activePhoto}
          />
          <textarea
            className="editor-caption"
            value={activePhoto?.description || ""}
            onChange={(event) =>
              updateActivePhoto({ description: event.target.value })
            }
            placeholder="Факт о городе"
            disabled={!activePhoto}
          />
          <div className="editor-colors">
            <b>Цвет текста на фото</b>
            <span>
              {colors.map((color) => (
                <button
                  type="button"
                  className={
                    (activePhoto?.textColor || textColor) === color
                      ? "active"
                      : ""
                  }
                  style={{ background: color }}
                  onClick={() => updateActivePhoto({ textColor: color })}
                  aria-label={`Выбрать цвет ${color}`}
                  key={color}
                  disabled={!activePhoto}
                />
              ))}
            </span>
          </div>
          <section className="editor-section">
            <header>
              <b>Маршрут на карте</b>
              <small>{mapPoints.length} точек</small>
            </header>
            <div className="editor-chips">
              {mapPoints.map((point, index) => (
                <button type="button" key={`${point}-${index}`}>
                  Точка {index + 1}
                  <i
                    onClick={(event) => {
                      event.stopPropagation();
                      setMapPoints((points) =>
                        points.filter((_, pointIndex) => pointIndex !== index),
                      );
                    }}
                  >
                    ×
                  </i>
                </button>
              ))}
            </div>
            <button type="button" className="editor-add" onClick={addMapPoint}>
              ＋ Добавить точку
            </button>
          </section>
          <section className="editor-section">
            <header>
              <b>Города для погоды</b>
            </header>
            <div className="editor-weather-cities">
              {weatherCities.map((weatherCity, index) => (
                <label key={`${weatherCity}-${index}`}>
                  <input
                    value={weatherCity}
                    onChange={(event) =>
                      setWeatherCities((cities) =>
                        cities.map((item, cityIndex) =>
                          cityIndex === index ? event.target.value : item,
                        ),
                      )
                    }
                  />
                  <button
                    type="button"
                    onClick={() =>
                      setWeatherCities((cities) =>
                        cities.filter((_, cityIndex) => cityIndex !== index),
                      )
                    }
                  >
                    ×
                  </button>
                </label>
              ))}
            </div>
            <button
              type="button"
              className="editor-add"
              onClick={addWeatherCity}
            >
              ＋ Добавить город
            </button>
          </section>
        </div>
        <footer>
          <button type="button" onClick={onClose}>
            Отмена
          </button>
          <button className="accent" type="button" onClick={save}>
            Готово
          </button>
        </footer>
      </section>
    </div>
  );
  return (
    <div className="overview-editor-backdrop" onClick={onClose}>
      <section
        className="overview-editor"
        role="dialog"
        aria-modal="true"
        aria-labelledby="overview-editor-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <h2 id="overview-editor-title">Редактирование главной</h2>
          <button type="button" onClick={onClose} aria-label="Закрыть">
            ×
          </button>
        </header>
        <div className="overview-editor-content">
          <div className="editor-city-head">
            <b>Слайд города</b>
            <span>
              <button
                type="button"
                onClick={() =>
                  setCityIndex(
                    (index) =>
                      (index - 1 + routeCities.length) % routeCities.length,
                  )
                }
                disabled={routeCities.length < 2}
              >
                ‹
              </button>
              <strong>{city}</strong>
              <button
                type="button"
                onClick={() =>
                  setCityIndex((index) => (index + 1) % routeCities.length)
                }
                disabled={routeCities.length < 2}
              >
                ›
              </button>
            </span>
          </div>
          <div
            className={`editor-photo-drop ${selectedPhoto ? "has-photo" : ""}`}
            style={
              selectedPhoto
                ? {
                    backgroundImage: `linear-gradient(#11182733, #11182733), url(${selectedPhoto})`,
                  }
                : undefined
            }
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => {
              event.preventDefault();
              selectPhoto(event.dataTransfer.files[0]);
            }}
          >
            <input
              ref={input}
              type="file"
              accept="image/*"
              onChange={(event) => {
                selectPhoto(event.target.files?.[0]);
                event.target.value = "";
              }}
            />
            {selectedPhoto ? (
              <span>Фото выбрано</span>
            ) : (
              <>
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="m4 18 5-5 3 3 3-3 5 5" />
                </svg>
                <span>Перетащите фото города</span>
                <small>
                  or{" "}
                  <button type="button" onClick={() => input.current?.click()}>
                    browse files
                  </button>
                </small>
              </>
            )}
          </div>
          <input className="editor-field" value={city} readOnly />
          <textarea
            className="editor-caption"
            value={caption}
            onChange={(event) => setCaption(event.target.value)}
            placeholder="Описание города на главной"
          />
          <div className="editor-colors">
            <b>Цвет текста на фото</b>
            <span>
              <i className="active" />
              <i />
              <i />
              <i />
            </span>
          </div>
          <section className="editor-section">
            <header>
              <b>Маршрут на карте</b>
              <small>{mapPoints.length} точек</small>
            </header>
            <div className="editor-chips">
              {mapPoints.map((point, index) => (
                <button type="button" key={`${point}-${index}`}>
                  Точка {index + 1}
                  <i
                    onClick={(event) => {
                      event.stopPropagation();
                      setMapPoints((points) =>
                        points.filter((_, pointIndex) => pointIndex !== index),
                      );
                    }}
                  >
                    ×
                  </i>
                </button>
              ))}
            </div>
            <button type="button" className="editor-add" onClick={addMapPoint}>
              ＋ Добавить точку
            </button>
          </section>
          <section className="editor-section">
            <header>
              <b>Города для погоды</b>
            </header>
            <div className="editor-weather-cities">
              {weatherCities.map((weatherCity, index) => (
                <label key={`${weatherCity}-${index}`}>
                  <input
                    value={weatherCity}
                    onChange={(event) =>
                      setWeatherCities((cities) =>
                        cities.map((item, cityIndex) =>
                          cityIndex === index ? event.target.value : item,
                        ),
                      )
                    }
                  />
                  <button
                    type="button"
                    onClick={() =>
                      setWeatherCities((cities) =>
                        cities.filter((_, cityIndex) => cityIndex !== index),
                      )
                    }
                  >
                    ×
                  </button>
                </label>
              ))}
            </div>
            <button
              type="button"
              className="editor-add"
              onClick={addWeatherCity}
            >
              ＋ Добавить город
            </button>
          </section>
        </div>
        <footer>
          <button type="button" onClick={onClose}>
            Отмена
          </button>
          <button className="accent" type="button" onClick={save}>
            Готово
          </button>
        </footer>
      </section>
    </div>
  );
}

const weatherDescription = (code: number) => {
  if (code === 0) return "Ясно";
  if (code <= 3) return "Облачно";
  if (code <= 48) return "Туман";
  if (code <= 67) return "Дождь";
  if (code <= 77) return "Снег";
  return "Ливень";
};

function WeatherOverview({
  cities,
  tripDates,
  coverPhotos = weatherCoverPhotos,
}: {
  cities: string[];
  tripDates: string;
  coverPhotos?: CoverPhoto[];
}) {
  const [mode, setMode] = useState<"now" | "trip">("now");
  const [weather, setWeather] = useState<
    Record<string, { temperature: number; code: number }>
  >({});
  const [failed, setFailed] = useState(false);
  const weatherCities = cities.reduce<
    { name: string; latitude: number; longitude: number }[]
  >((result, name) => {
    const match = Object.entries(mapLocations).find(([city]) =>
      name.includes(city),
    );
    if (!match || result.some((city) => city.name === match[0])) return result;
    result.push({
      name: match[0],
      latitude: match[1][1],
      longitude: match[1][0],
    });
    return result;
  }, []);
  const weatherKey = weatherCities
    .map((city) => `${city.name}:${city.latitude},${city.longitude}`)
    .join("|");

  useEffect(() => {
    if (!weatherCities.length) return;
    let cancelled = false;
    const latitude = weatherCities.map((city) => city.latitude).join(",");
    const longitude = weatherCities.map((city) => city.longitude).join(",");
    void fetch(
      `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,weather_code&temperature_unit=celsius`,
    )
      .then(async (response) => {
        if (!response.ok) throw new Error("Weather request failed");
        return response.json() as Promise<
          { current: { temperature_2m: number; weather_code: number } }[]
        >;
      })
      .then((data) => {
        const entries = data.map(
          (item, index) =>
            [
              weatherCities[index].name,
              {
                temperature: item.current.temperature_2m,
                code: item.current.weather_code,
              },
            ] as const,
        );
        if (!cancelled) setWeather(Object.fromEntries(entries));
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
    };
  }, [weatherKey]);

  return (
    <section className="weather-overview">
      <header className="overview-section-head weather-heading">
        <div>
          <h2>Погода по маршруту</h2>
          <p>
            {mode === "now"
              ? "Текущая погода в городах поездки"
              : tripDates}
          </p>
        </div>
        <div className="weather-switch" role="group" aria-label="Период погоды">
          <button
            className={mode === "now" ? "active" : ""}
            onClick={() => setMode("now")}
          >
            Сейчас
          </button>
          <button
            className={mode === "trip" ? "active" : ""}
            onClick={() => setMode("trip")}
          >
            На даты поездки
          </button>
        </div>
      </header>
      {mode === "trip" && (
        <p className="weather-notice">
          Точный прогноз появится примерно за 16 дней до начала поездки.
        </p>
      )}
      <div className="weather-grid">
        {weatherCities.map((city) => {
          const current = weather[city.name];
          const photo = coverPhotos.find(
            (item) =>
              item.city?.trim().toLocaleLowerCase("ru") ===
              city.name.toLocaleLowerCase("ru"),
          );
          return (
            <article
              className={photo ? "weather-card has-photo" : "weather-card"}
              style={
                photo
                  ? {
                      backgroundImage: `linear-gradient(rgba(18, 18, 26, 0.42), rgba(18, 18, 26, 0.72)), url(${photo.image})`,
                    }
                  : undefined
              }
              key={city.name}
            >
              <h3>{city.name}</h3>
              {mode === "now" ? (
                failed ? (
                  <p>Не удалось обновить погоду</p>
                ) : current ? (
                  <>
                    <b>{Math.round(current.temperature)}°C</b>
                    <span>{weatherDescription(current.code)}</span>
                  </>
                ) : (
                  <p>Обновляем...</p>
                )
              ) : (
                <>
                  <b>19 дек - 3 янв</b>
                  <span>Прогноз появится позже</span>
                </>
              )}
            </article>
          );
        })}
      </div>
    </section>
  );
}

function TripOverview({
  trip,
  onUpdateTrip,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
}) {
  const photoInputRef = useRef<HTMLInputElement>(null);
  const [activePhoto, setActivePhoto] = useState(0);
  const [draggedPhoto, setDraggedPhoto] = useState<number | null>(null);
  const [routeTotals, setRouteTotals] = useState<{
    distance: number;
    duration: number;
  } | null>(null);
  const routeDays = (trip.days || []).filter((day) => day.roadLeg);
  const routeKey = routeDays
    .map((day) => `${day.roadLeg?.from}:${day.roadLeg?.to}`)
    .join("|");
  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    const legs = routeDays
      .map((day) => [mapLocation(day.roadLeg!.from), mapLocation(day.roadLeg!.to)])
      .filter((leg): leg is [[number, number], [number, number]] =>
        Boolean(leg[0] && leg[1]),
      );
    if (!token || !legs.length) {
      setRouteTotals(null);
      return;
    }
    let cancelled = false;
    void Promise.all(
      legs.map(async ([from, to]) => {
        const response = await fetch(
          `https://api.mapbox.com/directions/v5/mapbox/driving/${from.join(",")};${to.join(",")}?overview=false&access_token=${token}`,
        );
        const data = (await response.json()) as {
          routes?: { distance: number; duration: number }[];
        };
        return data.routes?.[0];
      }),
    )
      .then((routes) => {
        const validRoutes = routes.filter(
          (route): route is { distance: number; duration: number } => Boolean(route),
        );
        if (cancelled || validRoutes.length !== legs.length) return;
        setRouteTotals(
          validRoutes.reduce(
            (total, route) => ({
              distance: total.distance + route.distance,
              duration: total.duration + route.duration,
            }),
            { distance: 0, duration: 0 },
          ),
        );
      })
      .catch(() => {
        if (!cancelled) setRouteTotals(null);
      });
    return () => {
      cancelled = true;
    };
  }, [routeKey]);
  const routeSummary = `${routeDays.length} дней${
    routeTotals
      ? ` · ${Math.round(routeTotals.distance / 1000).toLocaleString("ru-RU")} км · ${Math.round(routeTotals.duration / 3600)} ч`
      : ""
  }`;
  const overviewCities =
    trip.overviewMapPoints?.length
      ? trip.overviewMapPoints
      : Array.from(
          new Set(
            (trip.days || []).flatMap((day) =>
              day.roadLeg ? [day.roadLeg.from, day.roadLeg.to] : [],
            ).filter(Boolean),
          ),
        );
  const coverPhotos = (
    trip.coverPhotos?.length
      ? trip.coverPhotos
      : trip.coverImage
        ? [
            {
              id: "legacy-cover",
              image: trip.coverImage,
              city: trip.coverCity,
              description: trip.coverDescription,
            },
          ]
        : []
  ).filter((photo) => photo.id !== "verona-cover");
  weatherCoverPhotos = coverPhotos;
  const activeCover =
    coverPhotos[Math.min(activePhoto, Math.max(0, coverPhotos.length - 1))];
  const uploadCoverPhoto = async (file: Blob, extension: string) => {
    const {
      data: { session },
    } = await supabase.auth.getSession();
    if (!session) throw new Error("No active session");
    const path = `${session.user.id}/${trip.id}/${crypto.randomUUID()}.${extension}`;
    const { error } = await supabase.storage
      .from("trip-photos")
      .upload(path, file, {
        cacheControl: "31536000",
        upsert: false,
        contentType: file.type || "image/jpeg",
      });
    if (error) throw error;
    return supabase.storage.from("trip-photos").getPublicUrl(path).data
      .publicUrl;
  };
  const addCoverPhotos = async (files: FileList | null) => {
    if (!files?.length) return;
    try {
      const uploadedPhotos = await Promise.all(
        Array.from(files).map(async (file) => ({
          id: crypto.randomUUID(),
          image: await uploadCoverPhoto(
            file,
            file.name.split(".").pop()?.toLowerCase() || "jpg",
          ),
        })),
      );
      const nextPhotos = [...coverPhotos, ...uploadedPhotos];
      onUpdateTrip({
        ...trip,
        coverImage: nextPhotos[0]?.image,
        coverPhotos: nextPhotos,
      });
      setActivePhoto(nextPhotos.length - uploadedPhotos.length);
    } catch {
      window.alert(
        "Не удалось загрузить фотографию. Попробуйте файл JPG, PNG или WebP до 10 МБ.",
      );
    }
  };
  useEffect(() => {
    const localPhotos = coverPhotos.filter((photo) =>
      photo.image.startsWith("data:image/"),
    );
    if (!localPhotos.length) return;
    let cancelled = false;
    void Promise.all(
      coverPhotos.map(async (photo) => {
        if (!photo.image.startsWith("data:image/")) return photo;
        const file = await fetch(photo.image).then((response) =>
          response.blob(),
        );
        const extension = file.type.split("/")[1] || "jpg";
        return { ...photo, image: await uploadCoverPhoto(file, extension) };
      }),
    )
      .then((migratedPhotos) => {
        if (!cancelled)
          onUpdateTrip({
            ...trip,
            coverImage: migratedPhotos[0]?.image,
            coverPhotos: migratedPhotos,
          });
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [trip.id, trip.coverPhotos]);
  const reorderCoverPhotos = (_from: number, _to: number) => undefined;
  if (trip.isDraft && !activeCover)
    return (
      <div className="trip-overview">
        <div className="overview-draft">
          <div className="cover-photo-stack">
            <section
              className="cover-empty-gallery"
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => {
                event.preventDefault();
                void addCoverPhotos(event.dataTransfer.files);
              }}
            >
              <input
                ref={photoInputRef}
                className="cover-file-input"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                onChange={(event) => void addCoverPhotos(event.target.files)}
              />
              <button
                type="button"
                className="cover-arrow previous"
                aria-label="Предыдущее фото"
              >
                ‹
              </button>
              <button
                type="button"
                className="cover-arrow next"
                aria-label="Следующее фото"
              >
                ›
              </button>
              <div className="empty-gallery-upload">
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="m4 18 5-5 3 3 3-3 5 5" />
                </svg>
                <span>Перетащите фото города</span>
                <small>
                  or{" "}
                  <button
                    type="button"
                    onClick={() => photoInputRef.current?.click()}
                  >
                    browse files
                  </button>
                </small>
              </div>
              <div className="empty-gallery-cities">
                <i />
                <i />
              </div>
            </section>
          </div>
          <aside className="map-card">
            <TripMap routeDays={trip.days} />
            <footer>
              <span>Общий маршрут</span>
              <b>{routeSummary}</b>
            </footer>
          </aside>
        </div>
        <WeatherOverview
          cities={overviewCities}
          tripDates={trip.dates}
          coverPhotos={coverPhotos}
        />
      </div>
    );
  if (trip.isDraft)
    return (
      <div className="trip-overview">
        <div className="overview-draft">
          <div className="cover-photo-stack">
            <section
              className={activeCover ? "has-draft-cover" : ""}
              style={
                activeCover
                  ? {
                      backgroundImage: `linear-gradient(rgba(27, 28, 31, 0.3), rgba(27, 28, 31, 0.3)), url(${activeCover.image})`,
                    }
                  : undefined
              }
            >
              <input
                ref={photoInputRef}
                className="cover-file-input"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                onChange={(event) => void addCoverPhotos(event.target.files)}
              />
              {activeCover ? (
                <>
                  <button
                    type="button"
                    className="cover-arrow previous"
                    onClick={() =>
                      setActivePhoto(
                        (activePhoto - 1 + coverPhotos.length) %
                          coverPhotos.length,
                      )
                    }
                    disabled={coverPhotos.length < 2}
                    aria-label="Предыдущее фото"
                  >
                    ‹
                  </button>
                  <button
                    type="button"
                    className="cover-arrow next"
                    onClick={() =>
                      setActivePhoto((activePhoto + 1) % coverPhotos.length)
                    }
                    disabled={coverPhotos.length < 2}
                    aria-label="Следующее фото"
                  >
                    ›
                  </button>
                  <button
                    type="button"
                    className="add-cover-photo"
                    onClick={() => photoInputRef.current?.click()}
                  >
                    ＋ Фото
                  </button>
                  {activeCover.city && (
                    <div className="cover-photo-caption">
                      <b>{activeCover.city}</b>
                      {activeCover.description && (
                        <span>{activeCover.description}</span>
                      )}
                    </div>
                  )}
                </>
              ) : (
                <>
                  <p>ГЛАВНАЯ</p>
                  <h2>Начните планировать путешествие</h2>
                  <span>Добавьте первую фотографию путешествия.</span>
                  <button
                    type="button"
                    className="add-cover-photo"
                    onClick={() => photoInputRef.current?.click()}
                  >
                    ＋ Фото
                  </button>
                </>
              )}
            </section>
            {coverPhotos.length > 1 && (
              <div className="cover-order">
                <p>Перетащите фото в порядке городов маршрута</p>
                <div>
                  {coverPhotos.map((photo, index) => (
                    <button
                      className={`${index === activePhoto ? "active" : ""} ${index === draggedPhoto ? "dragging" : ""}`}
                      style={{ backgroundImage: `url(${photo.image})` }}
                      draggable
                      onDragStart={() => setDraggedPhoto(index)}
                      onDragEnd={() => setDraggedPhoto(null)}
                      onDragOver={(event) => event.preventDefault()}
                      onDrop={() => {
                        if (draggedPhoto !== null)
                          reorderCoverPhotos(draggedPhoto, index);
                        setDraggedPhoto(null);
                      }}
                      onClick={() => setActivePhoto(index)}
                      aria-label={photo.city || `Фото ${index + 1}`}
                      key={photo.id}
                    >
                      <span>{photo.city || index + 1}</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
          <aside className="map-card">
            <TripMap routeDays={trip.days} />
            <footer>
              <span>Общий маршрут</span>
              <b>{routeSummary}</b>
            </footer>
          </aside>
        </div>
        <WeatherOverview cities={overviewCities} tripDates={trip.dates} />
      </div>
    );
  const cities = [
    {
      name: "Рим",
      dates: "12–14 сентября",
      weather: "22°C · ясно",
      image:
        "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=900&q=80",
    },
    {
      name: "Флоренция",
      dates: "15–16 сентября",
      weather: "24°C · солнечно",
      image:
        "https://images.unsplash.com/photo-1544986581-efac024faf62?auto=format&fit=crop&w=900&q=80",
    },
    {
      name: "Венеция",
      dates: "17–19 сентября",
      weather: "20°C · облачно",
      image:
        "https://images.unsplash.com/photo-1514890547357-a9ee288728e0?auto=format&fit=crop&w=900&q=80",
    },
  ];
  return (
    <div className="trip-overview">
      <section className="overview-route">
        <span>ОБЩИЙ МАРШРУТ</span>
        <h2>
          Москва <b>→</b> Рим <b>→</b> Флоренция <b>→</b> Венеция
        </h2>
        <p>12–19 сентября 2026 · 8 дней · 3 города</p>
      </section>
      <section>
        <div className="overview-section-head">
          <div>
            <h2>Города поездки</h2>
            <p>Прогноз предварительный</p>
          </div>
        </div>
        <div className="city-overview-grid">
          {cities.map((city) => (
            <article className="city-overview-card" key={city.name}>
              <img src={city.image} alt={city.name} />
              <div>
                <h3>
                  {cityFlag(city.name)} {city.name}
                </h3>
                <p>{city.dates}</p>
                <b>{city.weather}</b>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

function WalkingMap({
  sights,
  city,
  activeSightId,
  travelMode = "walking",
}: {
  sights: StoredSight[];
  city?: string;
  activeSightId?: string;
  travelMode?: "walking" | "driving";
}) {
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const markerElements = useRef(new globalThis.Map<string, HTMLSpanElement>());
  const [stats, setStats] = useState<{
    distance: number;
    duration: number;
  } | null>(null);
  const [routeCoordinates, setRouteCoordinates] = useState<
    [number, number][] | null
  >(null);
  const coordinates = sights
    .map((sight) => sight.lnglat)
    .filter((coordinate): coordinate is [number, number] =>
      Boolean(coordinate),
    );
  const routeKey = sights
    .map((sight) => `${sight.id}:${sight.lnglat?.join(",") || ""}`)
    .join(";");

  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    if (!token || coordinates.length < 2) return;
    let cancelled = false;
    const path = coordinates.map((coordinate) => coordinate.join(",")).join(";");
    void fetch(
      `https://api.mapbox.com/directions/v5/mapbox/${travelMode}/${path}?geometries=geojson&overview=full&access_token=${token}`,
    )
      .then((response) => response.json())
      .then((data: {
        routes?: {
          distance: number;
          duration: number;
          geometry?: { coordinates: [number, number][] };
        }[];
      }) => {
        const route = data.routes?.[0];
        if (cancelled) return;
        setStats(route || null);
        setRouteCoordinates(route?.geometry?.coordinates || null);
      })
      .catch(() => {
        if (!cancelled) {
          setStats(null);
          setRouteCoordinates(null);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [routeKey, travelMode]);

  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    const fallbackLocation = city ? mapLocation(city) : undefined;
    if (!container.current || !token || (!coordinates.length && !fallbackLocation))
      return;
    let map: Map | undefined;
    let disposed = false;
    void import("mapbox-gl").then(({ default: mapboxgl }) => {
      if (disposed || !container.current) return;
      mapboxgl.accessToken = token;
      map = new mapboxgl.Map({
        container: container.current,
        style: mapStyle(),
        center: coordinates[0] || fallbackLocation!,
        zoom: coordinates.length ? 13 : 11,
        attributionControl: false,
      });
      mapRef.current = map;
      markerElements.current.clear();
      map.addControl(
        new mapboxgl.NavigationControl({ showCompass: false }),
        "top-right",
      );
      sights.forEach((sight, index) => {
        if (!sight.lnglat) return;
        const marker = document.createElement("span");
        marker.className = "sight-map-marker";
        marker.textContent = String(index + 1);
        markerElements.current.set(sight.id, marker);
        new mapboxgl.Marker({ element: marker })
          .setLngLat(sight.lnglat)
          .addTo(map!);
      });
      map.on("load", () => {
        if (coordinates.length > 1) {
          map!.addSource("walking-route", {
            type: "geojson",
            data: {
              type: "Feature",
              properties: {},
              geometry: {
                type: "LineString",
                coordinates: routeCoordinates || coordinates,
              },
            },
          });
          map!.addLayer({
            id: "walking-route",
            type: "line",
            source: "walking-route",
            paint: {
              "line-color": "#ef7b48",
              "line-width": 4,
              "line-opacity": 0.9,
            },
          });
        }
        const bounds = new mapboxgl.LngLatBounds(
          coordinates[0],
          coordinates[0],
        );
        coordinates.slice(1).forEach((coordinate) => bounds.extend(coordinate));
        map!.fitBounds(bounds, { padding: 38, maxZoom: 14 });
      });
    });
    return () => {
      disposed = true;
      map?.remove();
      mapRef.current = null;
      markerElements.current.clear();
    };
  }, [routeKey, city, routeCoordinates]);
  useEffect(() => {
    if (!activeSightId) return;
    const marker = markerElements.current.get(activeSightId);
    const sight = sights.find((item) => item.id === activeSightId);
    if (!marker || !sight?.lnglat) return;
    marker.classList.remove("bounce");
    void marker.offsetWidth;
    marker.classList.add("bounce");
    mapRef.current?.flyTo({
      center: sight.lnglat,
      zoom: 15,
      duration: 700,
      essential: true,
    });
  }, [activeSightId, sights]);
  useEffect(() => {
    const focusSight = (event: Event) => {
      const id = (event as CustomEvent<string>).detail;
      const marker = markerElements.current.get(id);
      const sight = sights.find((item) => item.id === id);
      if (!marker || !sight?.lnglat) return;
      marker.classList.remove("bounce");
      void marker.offsetWidth;
      marker.classList.add("bounce");
      mapRef.current?.flyTo({
        center: sight.lnglat,
        zoom: 15,
        duration: 700,
        essential: true,
      });
    };
    window.addEventListener("ramingo-focus-sight", focusSight);
    return () => window.removeEventListener("ramingo-focus-sight", focusSight);
  }, [sights]);
  const hours = stats ? Math.floor(stats.duration / 3600) : 0;
  const minutes = stats ? Math.round((stats.duration % 3600) / 60) : 0;
  return (
    <div className="walking-map-wrap">
      <div className="walking-map" ref={container} />
      <footer>
        <span>Маршрут дня · {travelMode === "driving" ? "на машине" : "пешком"}</span>
        <b>
          {stats
            ? `${(stats.distance / 1000).toLocaleString("ru-RU", { maximumFractionDigits: 1 })} км · ${hours ? `${hours} ч ` : ""}${minutes} мин`
            : `${sights.length} ${sights.length === 1 ? "точка" : sights.length < 5 ? "точки" : "точек"}`}
        </b>
      </footer>
    </div>
  );
}

function Sights({
  sights,
  days,
  defaultCity,
  onToggle,
  onAddDay,
  onCreateDay,
  onRenameDay,
}: {
  sights: StoredSight[];
  days: { id: string; title: string; photo?: string; photoPosition?: number }[];
  defaultCity?: string;
  onToggle: (id: string) => void;
  onAddDay: (title: string) => void;
  onCreateDay: (
    dayIndex: number,
    city: string,
    featured: { name: string; description?: string; photo?: string },
    places: DayPlaceDraft[],
    photo?: string,
    photoPosition?: number,
  ) => void;
  onRenameDay: (id: string, title: string) => void;
}) {
  const [addingDay, setAddingDay] = useState(false);
  const [dayEditorOpen, setDayEditorOpen] = useState(false);
  const [routeCopied, setRouteCopied] = useState(false);
  const [selectedDay, setSelectedDay] = useState(0);
  const [activeSightId, setActiveSightId] = useState<string | null>(null);
  const [expandedSightId, setExpandedSightId] = useState<string | null>(null);
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "auto" });
  }, []);
  useEffect(() => {
    if (selectedDay >= days.length) {
      setSelectedDay(0);
      return;
    }
    const dayId = days[selectedDay]?.id;
    if (dayId)
      window.dispatchEvent(
        new CustomEvent("ramingo-select-sight-day", { detail: dayId }),
      );
  }, [selectedDay, days]);
  const cities = Array.from(new Set(sights.map((sight) => sight.city))).sort();
  if (!cities.length && defaultCity) cities.push(defaultCity);
  const [city, setCity] = useState(cities[0] || "");
  useEffect(() => {
    const dayCity = days[selectedDay]?.title;
    if (dayCity) setCity(dayCity);
  }, [selectedDay, days]);
  const routeSights = sights
    .filter((sight) => (sight.walkDay || 1) === selectedDay + 1)
    .sort((a, b) => (a.walkOrder || 0) - (b.walkOrder || 0));
  const copyRoute = async () => {
    if (routeSights.length < 2) return;
    const points = routeSights.map((sight) =>
      sight.lnglat
        ? `${sight.lnglat[1]},${sight.lnglat[0]}`
        : `${sight.name}, ${sight.city}`,
    );
    const travelMode = routeSights.some((sight) => sight.id.startsWith("stelvio_"))
      ? "driving"
      : "walking";
    const url = new URL("https://www.google.com/maps/dir/");
    url.searchParams.set("api", "1");
    url.searchParams.set("origin", points[0]);
    url.searchParams.set("destination", points.at(-1) || "");
    if (points.length > 2) url.searchParams.set("waypoints", points.slice(1, -1).join("|"));
    url.searchParams.set("travelmode", travelMode);
    await navigator.clipboard.writeText(url.toString()).catch(() => undefined);
    setRouteCopied(true);
    window.setTimeout(() => setRouteCopied(false), 1800);
  };
  const [categoryFilter, setCategoryFilter] = useState("Все");
  const [statusFilter, setStatusFilter] = useState("Все");
  const categories = ["Все", ...Array.from(new Set(routeSights.map((sight) => sight.subcategory || sight.group || "Достопримечательность")))];
  const visibleSights = routeSights.filter((sight) => (categoryFilter === "Все" || (sight.subcategory || sight.group || "Достопримечательность") === categoryFilter) && (statusFilter === "Все" || (statusFilter === "Посещено" ? sight.done : !sight.done)));
  const categoryFor = (sight: StoredSight) =>
    sight.subcategory || sight.group || "Достопримечательность";
  const markerToneFor = (sight: StoredSight) => {
    const category = categoryFor(sight).toLowerCase();
    if (category.includes("еда") || category.includes("ресторан")) return "food";
    if (category.includes("переезд") || category.includes("прогул")) return "walk";
    return "sight";
  };
  const focusSight = (sight: StoredSight) => {
    setActiveSightId(sight.id);
    window.dispatchEvent(new CustomEvent("ramingo-focus-sight", { detail: sight.id }));
  };
  const activeDayTitle = days[selectedDay]?.title || city || "Маршрут";
  return (
    <>
      <section className="sights-page sights-redesigned">
        <div className="sights-redesigned-toolbar">
          <span aria-hidden="true" />
          <div>
            <button
              type="button"
              className="sights-add-trigger"
              onClick={() => setDayEditorOpen(true)}
            >
              ＋ Добавить
            </button>
          </div>
        </div>
        <div className="sights-redesigned-grid">
          <aside className="sights-days-rail" aria-label="Дни маршрута">
            <div className="sights-day-list">
              {days.map((day, index) => {
                const count = sights.filter(
                  (sight) => (sight.walkDay || 1) === index + 1,
                ).length;
                return (
                  <div
                    className={
                      selectedDay === index
                        ? "sights-day-card active"
                        : "sights-day-card"
                    }
                    key={day.id}
                  >
                    <button
                      type="button"
                      onClick={() => {
                        setSelectedDay(index);
                        setCity(day.title);
                        setActiveSightId(null);
                        setExpandedSightId(null);
                      }}
                    >
                      <small>День {index + 1}</small>
                      <b>{day.title}</b>
                      <span>{count} мест</span>
                    </button>
                    <button
                      type="button"
                      className="sights-day-rename"
                      onClick={() => {
                        const title = window
                          .prompt("Название дня", day.title)
                          ?.trim();
                        if (title) onRenameDay(day.id, title);
                      }}
                      aria-label={`Переименовать день ${index + 1}`}
                    >
                      ✎
                    </button>
                  </div>
                );
              })}
            </div>
            {addingDay ? (
              <form
                className="sights-add-day-form"
                onSubmit={(event) => {
                  event.preventDefault();
                  const title = String(
                    new FormData(event.currentTarget).get("title") || "",
                  ).trim();
                  if (!title) return;
                  onAddDay(title);
                  setAddingDay(false);
                }}
              >
                <input name="title" placeholder="Например, Рим" autoFocus />
                <button type="submit" className="accent">Добавить</button>
              </form>
            ) : (
              <button
                type="button"
                className="sights-add-day"
                onClick={() => setAddingDay(true)}
              >
                ＋ Добавить день
              </button>
            )}
          </aside>
          <main className="sights-timeline-column">
            <header className="sights-timeline-heading">
              <h1>День {selectedDay + 1} · {activeDayTitle}</h1>
              <p>{routeSights.length} мест</p>
            </header>
            {visibleSights.length ? (
              <div className="sights-timeline">
                {visibleSights.map((sight) => {
                  const category = categoryFor(sight);
                  const tone = markerToneFor(sight);
                  const sightNumber = routeSights.findIndex((item) => item.id === sight.id) + 1;
                  return (
                    <article
                      className={
                        sight.done
                          ? "sights-timeline-event done"
                          : "sights-timeline-event"
                      }
                      key={sight.id}
                    >
                      <span className={`sights-timeline-marker ${tone}`} aria-hidden="true">
                        {sightNumber}
                      </span>
                      <div className="sights-timeline-card">
                        <div className="sights-event-top">
                          <button
                            type="button"
                            className="sights-event-name"
                            onClick={() => {
                              focusSight(sight);
                              onToggle(sight.id);
                            }}
                          >
                            {sight.name}
                          </button>
                          <button
                            type="button"
                            className="sights-event-more"
                            onClick={() => {
                              focusSight(sight);
                              setExpandedSightId((current) =>
                                current === sight.id ? null : sight.id,
                              );
                            }}
                            aria-label={`${expandedSightId === sight.id ? "Скрыть" : "Показать"} детали ${sight.name}`}
                          >
                            ···
                          </button>
                        </div>
                        <div className="sights-event-meta">
                          <span className={`sights-event-tag ${tone}`}>{category}</span>
                          {sight.duration && <span>· {sight.duration}</span>}
                          {sight.city !== city && <span>· {sight.city}</span>}
                          {sight.description && expandedSightId === sight.id && (
                            <span
                              className="sights-event-description"
                            >
                              · {sight.description}
                            </span>
                          )}
                          {sight.photo && expandedSightId === sight.id && (
                            <img
                              className="sights-event-photo"
                              src={sight.photo}
                              alt=""
                            />
                          )}
                          <label className="sights-event-check">
                            <input
                              type="checkbox"
                              checked={Boolean(sight.done)}
                              onChange={() => onToggle(sight.id)}
                            />
                            {sight.done ? "Посещено" : "Отметить"}
                          </label>
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            ) : (
              <div className="sights-redesigned-empty">
                <b>День пока свободен</b>
                <p>Добавьте места, которые хотите посетить.</p>
                <button
                  type="button"
                  className="accent"
                  onClick={() => setDayEditorOpen(true)}
                >
                  ＋ Добавить место
                </button>
              </div>
            )}
            <div className="sights-timeline-filters">
              <div className="sights-filter-group" aria-label="Категория">
                {categories.map((category) => (
                  <button
                    type="button"
                    className={categoryFilter === category ? "active" : ""}
                    onClick={() => setCategoryFilter(category)}
                    key={category}
                  >
                    {category}
                  </button>
                ))}
              </div>
              <div className="sights-filter-group sights-status-filters" aria-label="Статус посещения">
                {(["Все", "Не посещено", "Посещено"] as const).map((status) => (
                  <button
                    type="button"
                    className={statusFilter === status ? "active" : ""}
                    onClick={() => setStatusFilter(status)}
                    key={status}
                  >
                    {status}
                  </button>
                ))}
              </div>
            </div>
          </main>
          <aside className="sights-route-panel">
            <div className="sights-route-panel-head">
              <span>{city || "Маршрут"} · маршрут дня</span>
              <button
                type="button"
                onClick={() => void copyRoute()}
                disabled={routeSights.length < 2}
              >
                {routeCopied ? "Скопировано" : "Копировать"}
              </button>
            </div>
            <WalkingMap
              sights={routeSights}
              city={city}
              activeSightId={activeSightId || undefined}
              travelMode={
                routeSights.some((sight) => sight.id.startsWith("stelvio_"))
                  ? "driving"
                  : "walking"
              }
            />
          </aside>
        </div>
      </section>
      {dayEditorOpen && (
        <DayEditor
          dayNumber={selectedDay + 1}
          defaultCity={days[selectedDay]?.title || city}
          onClose={() => setDayEditorOpen(false)}
          onSave={(nextCity, featuredPlace, places, photo, photoPosition) => {
            onCreateDay(selectedDay, nextCity, featuredPlace, places, photo, photoPosition);
            setDayEditorOpen(false);
          }}
        />
      )}
    </>
  );
}

function DayEditor({
  dayNumber,
  defaultCity,
  onClose,
  onSave,
}: {
  dayNumber: number;
  defaultCity: string;
  onClose: () => void;
  onSave: (city: string, featured: DayPlaceDraft, places: DayPlaceDraft[], photo?: string, photoPosition?: number) => void;
}) {
  const [places, setPlaces] = useState<DayPlaceDraft[]>([]);
  const [place, setPlace] = useState("");
  const [placeCategory, setPlaceCategory] = useState("Достопримечательность");
  const [placeDescription, setPlaceDescription] = useState("");
  const [placePhoto, setPlacePhoto] = useState<string>();
  const [placePhotoFile, setPlacePhotoFile] = useState<File | null>(null);
  const [placePhotoPosition, setPlacePhotoPosition] = useState(50);
  const [draggingPlacePhoto, setDraggingPlacePhoto] = useState(false);
  const placePhotoDrag = useRef<{ y: number; position: number } | null>(null);
  const [featuredPhoto, setFeaturedPhoto] = useState<string>();
  const [uploadingFeaturedPhoto, setUploadingFeaturedPhoto] = useState(false);
  const [photoPosition, setPhotoPosition] = useState(50);
  const addPlace = async () => {
    const value = place.trim();
    if (!value) return;
    const uploadedPhoto = placePhotoFile
      ? await uploadFeaturedPhoto(placePhotoFile)
      : placePhoto;
    if (placePhotoFile && !uploadedPhoto) return;
    setPlaces((current) => [...current, { name: value, subcategory: placeCategory, description: placeDescription.trim() || undefined, photo: uploadedPhoto || undefined, photoPosition: placePhotoPosition }]);
    setPlace("");
    setPlaceDescription("");
    setPlacePhoto(undefined);
    setPlacePhotoFile(null);
    setPlacePhotoPosition(50);
  };
  const uploadFeaturedPhoto = async (file: File) => {
    if (!file.type.match(/^image\/(jpeg|png|webp)$/) || file.size > 10 * 1024 * 1024) {
      window.alert("Выберите JPG, PNG или WebP до 10 МБ.");
      return;
    }
    setUploadingFeaturedPhoto(true);
    try {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session) throw new Error("No active session");
      const extension = file.name.split(".").pop()?.toLowerCase() || "jpg";
      const path = `${session.user.id}/sight-days/${crypto.randomUUID()}.${extension}`;
      const { error } = await supabase.storage.from("trip-photos").upload(path, file, {
        cacheControl: "31536000",
        contentType: file.type,
        upsert: false,
      });
      if (error) throw error;
      return supabase.storage.from("trip-photos").getPublicUrl(path).data.publicUrl;
    } catch {
      window.alert("Не удалось загрузить фото. Попробуйте ещё раз.");
      return null;
    } finally {
      setUploadingFeaturedPhoto(false);
    }
  };
  return (
    <div className="day-editor-backdrop" onClick={onClose}>
      <form
        className="day-editor"
        onClick={(event) => event.stopPropagation()}
        onSubmit={async (event) => {
          event.preventDefault();
          const data = new FormData(event.currentTarget);
          const city = String(data.get("city") || "").trim();
          if (!city) return;
          const selectedPhoto = data.get("featuredPhoto");
          const featuredPhotoFile =
            selectedPhoto instanceof File && selectedPhoto.size > 0
              ? selectedPhoto
              : null;
          // The place-photo preview is also the day cover when no separate
          // feature photo was picked above.
          const dayPhotoFile = featuredPhotoFile || placePhotoFile;
          const uploadedPhoto = dayPhotoFile
            ? await uploadFeaturedPhoto(dayPhotoFile)
            : featuredPhoto;
          if (dayPhotoFile && !uploadedPhoto) return;
          const featured = { name: String(data.get("featured") || "").trim(), description: String(data.get("featuredDescription") || "").trim() || undefined, photo: uploadedPhoto || undefined };
          onSave(city, featured, places, uploadedPhoto || undefined, photoPosition);
        }}
      >
        <header>
          <div>
            <small>ДЕНЬ {dayNumber}</small>
            <h2>День маршрута</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Закрыть">
            ×
          </button>
        </header>
        <label>
          Город
          <input
            name="city"
            defaultValue={defaultCity}
            placeholder="Напр. Болонья"
            autoFocus
          />
        </label>
        <label>
          Главная достопримечательность
          <input name="featured" placeholder="Напр. Две башни" />
        </label>
        <div className="featured-place-details"><textarea name="featuredDescription" placeholder="Описание объекта: что важно увидеть, время посещения, заметки..." /><label className="featured-photo-upload">{uploadingFeaturedPhoto ? "Загружаем фото..." : "Фото объекта"}<input name="featuredPhoto" type="file" accept="image/jpeg,image/png,image/webp" disabled={uploadingFeaturedPhoto} onChange={(event) => { const file = event.target.files?.[0]; setFeaturedPhoto(file ? URL.createObjectURL(file) : undefined); }} />{featuredPhoto && <img src={featuredPhoto} alt="Фото объекта" />}</label></div>
        <section>
          <div>
            <b>Список мест</b>
            <small>{places.length} мест</small>
          </div>
          <div className="day-place-input">
            <input
              value={place}
              onChange={(event) => setPlace(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  void addPlace();
                }
              }}
              placeholder="Добавить место..."
            />
            <button type="button" onClick={() => void addPlace()}>
              +
            </button>
          </div>
          <div className="day-place-details">
            <select value={placeCategory} onChange={(event) => setPlaceCategory(event.target.value)}><option>Достопримечательность</option></select>
            <input value={placeDescription} onChange={(event) => setPlaceDescription(event.target.value)} placeholder="Короткое описание" />
            <label className="day-place-photo-upload">
              <input type="file" accept="image/jpeg,image/png,image/webp" aria-label="Фото места" onChange={(event) => { const file = event.target.files?.[0] || null; setPlacePhotoFile(file); setPlacePhoto(file ? URL.createObjectURL(file) : undefined); }} />
              {placePhoto ? <img className={draggingPlacePhoto ? "dragging" : ""} src={placePhoto} alt="Фото места" style={{ objectPosition: `center ${placePhotoPosition}%` }} onPointerDown={(event) => { event.currentTarget.setPointerCapture(event.pointerId); placePhotoDrag.current = { y: event.clientY, position: placePhotoPosition }; setDraggingPlacePhoto(true); }} onPointerMove={(event) => { const start = placePhotoDrag.current; if (!start) return; const height = event.currentTarget.getBoundingClientRect().height; const next = start.position - ((event.clientY - start.y) / height) * 100; setPlacePhotoPosition(Math.max(0, Math.min(100, next))); }} onPointerUp={() => { placePhotoDrag.current = null; setDraggingPlacePhoto(false); }} onPointerCancel={() => { placePhotoDrag.current = null; setDraggingPlacePhoto(false); }} /> : <span>＋ Добавить фото</span>}
              {placePhoto && <small>Перетащите фото, чтобы выбрать кадр</small>}
            </label>
          </div>
          {places.length > 0 && (
            <ol>
              {places.map((item, index) => (
                <li key={`${item.name}-${index}`}>
                  <span>{item.photo && <img src={item.photo} alt="" />}{item.name}<small>{item.subcategory}</small></span>
                  <button
                    type="button"
                    onClick={() =>
                      setPlaces((current) =>
                        current.filter((_, itemIndex) => itemIndex !== index),
                      )
                    }
                  >
                    ×
                  </button>
                </li>
              ))}
            </ol>
          )}
        </section>
        <footer>
          <button type="button" onClick={onClose}>
            Отмена
          </button>
          <button className="accent" disabled={uploadingFeaturedPhoto}>Сохранить день</button>
        </footer>
      </form>
    </div>
  );
}

function SightNotes({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <section className="sight-notes">
      <div>
        <span>✎</span>
        <div>
          <h3>Заметки</h3>
          <p>Адреса, билеты, идеи и всё, что пригодится в прогулке.</p>
        </div>
      </div>
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder="Например: купить билеты заранее, прийти к открытию..."
      />
    </section>
  );
}

function Workspace({
  go,
  trip,
  onUpdateTrip,
  tab,
  onTabChange,
}: {
  go: (view: View) => void;
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
  tab: Tab;
  onTabChange: (tab: Tab) => void;
}) {
  const [editingRoadDay, setEditingRoadDay] = useState<number | null>(null);
  const [overviewEditorOpen, setOverviewEditorOpen] = useState(false);
  const [selectedSightDayId, setSelectedSightDayId] = useState("sights-day-1");
  const [statusMenuOpen, setStatusMenuOpen] = useState(false);
  const draftDays = trip.days?.length
    ? trip.days
    : [{ id: "day-1", places: trip.places || [] }];
  const firstDraftDay = draftDays[0];
  const useDemoSightContent = trip.title === "Рождественская Италия";
  const savedSightDays =
    trip.sightDaysVersion === 1 && trip.sightDays?.length
      ? trip.sightDays
      : [
          {
            id: "sights-day-1",
            title:
              firstDraftDay.roadLeg?.to ||
              firstDraftDay.roadLeg?.from ||
              "Первый день",
          },
        ];
  const sightDays =
    useDemoSightContent &&
    savedSightDays.length === 3 &&
    savedSightDays[2].title === "Рим"
      ? [
          ...savedSightDays,
          { id: "sights-day-4", title: "Рим" },
          { id: "sights-day-5", title: "Рим" },
          { id: "sights-day-6", title: "Сан-Марино" },
        ]
      : useDemoSightContent &&
          savedSightDays.length === 11 &&
          savedSightDays[10].title === "Равенсбург"
        ? [
            ...savedSightDays,
            { id: "sights-day-12", title: "Прага" },
            { id: "sights-day-13", title: "Прага" },
          ]
        : useDemoSightContent &&
            savedSightDays.length === 12 &&
            savedSightDays[11].title === "Прага"
          ? [...savedSightDays, { id: "sights-day-13", title: "Прага" }]
          : useDemoSightContent &&
              savedSightDays.length === 10 &&
              savedSightDays[9].title === "Милан"
            ? [...savedSightDays, { id: "sights-day-11", title: "Равенсбург" }]
            : useDemoSightContent &&
                savedSightDays.length === 9 &&
                savedSightDays[8].title === "Венеция"
              ? [
                  ...savedSightDays,
                  { id: "sights-day-10", title: "Милан" },
                  { id: "sights-day-11", title: "Равенсбург" },
                ]
              : useDemoSightContent &&
                  savedSightDays.length === 8 &&
                  savedSightDays[7].title === "Кьоджа"
                ? [
                    ...savedSightDays,
                    { id: "sights-day-9", title: "Венеция" },
                    { id: "sights-day-10", title: "Милан" },
                    { id: "sights-day-11", title: "Равенсбург" },
                  ]
                : useDemoSightContent &&
                    savedSightDays.length === 7 &&
                    savedSightDays[6].title === "Сан-Марино"
                  ? [
                      ...savedSightDays,
                      { id: "sights-day-8", title: "Кьоджа" },
                      { id: "sights-day-9", title: "Венеция" },
                      { id: "sights-day-10", title: "Милан" },
                      { id: "sights-day-11", title: "Равенсбург" },
                      { id: "sights-day-12", title: "Прага" },
                      { id: "sights-day-13", title: "Прага" },
                      { id: "sights-day-14", title: "Прага" },
                    ]
                  : useDemoSightContent &&
                      savedSightDays.length === 5 &&
                      savedSightDays[4].title === "Рим"
                    ? [
                        ...savedSightDays,
                        { id: "sights-day-6", title: "Сан-Марино" },
                      ]
                    : useDemoSightContent &&
                        savedSightDays.length === 4 &&
                        savedSightDays[3].title === "Рим"
                      ? [
                          ...savedSightDays,
                          { id: "sights-day-5", title: "Рим" },
                          { id: "sights-day-6", title: "Сан-Марино" },
                        ]
                      : useDemoSightContent &&
                          savedSightDays.length === 1 &&
                          savedSightDays[0].id === "sights-day-1"
                        ? [
                            ...savedSightDays,
                            { id: "sights-day-2", title: "Верона" },
                          ]
                        : savedSightDays;
  useEffect(() => {
    const index = 0;
    setSelectedSightDayId(sightDays[index]?.id || sightDays[0].id);
  }, [sightDays]);
  useEffect(() => {
    const selectDay = (event: Event) =>
      setSelectedSightDayId((event as CustomEvent<string>).detail);
    window.addEventListener("ramingo-select-sight-day", selectDay);
    return () =>
      window.removeEventListener("ramingo-select-sight-day", selectDay);
  }, []);
  useEffect(() => {
    if (!useDemoSightContent || trip.sightNotes?.["sights-day-2"] !== legacyVeronaDayTwoNotes) return;
    onUpdateTrip({
      ...trip,
      sightNotes: { ...trip.sightNotes, "sights-day-2": veronaDayTwoNotes },
    });
  }, [trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (!useDemoSightContent || selectedDay?.title !== "Верона" || trip.sightNotes?.[selectedDay.id])
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: { ...trip.sightNotes, [selectedDay.id]: veronaDayTwoNotes },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (!useDemoSightContent || selectedDay?.title !== "Рим" || trip.sightNotes?.[selectedDay.id])
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: { ...trip.sightNotes, [selectedDay.id]: romeDayThreeNotes },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (!useDemoSightContent || selectedDay?.title !== "Пиза" || trip.sightNotes?.[selectedDay.id])
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: { ...trip.sightNotes, [selectedDay.id]: pisaDaySixNotes },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (
      !useDemoSightContent ||
      selectedDay?.title !== "Сан-Марино" ||
      trip.sightNotes?.[selectedDay.id] !== pisaDaySixNotes
    )
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: { ...trip.sightNotes, [selectedDay.id]: "" },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (
      !useDemoSightContent ||
      selectedDay?.title !== "Сан-Марино" ||
      trip.sightNotes?.[selectedDay.id]
    )
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: {
        ...trip.sightNotes,
        [selectedDay.id]: sanMarinoDaySevenNotes,
      },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (!useDemoSightContent || selectedDay?.title !== "Кьоджа" || trip.sightNotes?.[selectedDay.id])
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: {
        ...trip.sightNotes,
        [selectedDay.id]: chioggiaDayEightNotes,
      },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (!useDemoSightContent || selectedDay?.title !== "Венеция" || trip.sightNotes?.[selectedDay.id])
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: { ...trip.sightNotes, [selectedDay.id]: veniceDayNineNotes },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (!useDemoSightContent || selectedDay?.title !== "Милан" || trip.sightNotes?.[selectedDay.id])
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: { ...trip.sightNotes, [selectedDay.id]: milanDayTenNotes },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (!useDemoSightContent || selectedDay?.title !== "Прага" || trip.sightNotes?.[selectedDay.id])
      return;
    onUpdateTrip({
      ...trip,
      sightNotes: { ...trip.sightNotes, [selectedDay.id]: pragueNotes },
    });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  const defaultChristmasSights = [
    ...munichDayOneSights,
    ...veronaDayTwoSights,
    ...romeDayThreeSights,
    ...romeDayFourSights,
    ...romeDayFiveSights,
    ...pisaDaySixSights,
    ...sanMarinoDaySixSights,
    ...chioggiaDayEightSights,
    ...veniceDayNineSights,
    ...milanDayTenSights,
    ...ravensburgDayElevenSights,
    ...pragueDayTwelveSights,
    ...pragueDayThirteenSights,
    ...pragueDayFourteenSights,
  ];
  const tripSights = useDemoSightContent
    ? [
        ...defaultChristmasSights.map((sight) => ({
          ...sight,
          ...trip.sights?.find((saved) => saved.id === sight.id),
        })),
        ...(trip.sights || []).filter(
          (sight) =>
            !defaultChristmasSights.some(
              (defaultSight) => defaultSight.id === sight.id,
            ) && !(sight.walkDay === 6 && sight.city === "Пиза"),
        ),
      ]
    : trip.sights || [];
  const labels: [Tab, string][] = trip.isDraft
    ? [
        ["overview", "Главная"],
        ["route", "Маршрут"],
        ["sights", "Достопримечательности"],
        ["restaurants", "Рестораны"],
        ["accommodation", "Жильё"],
        ["budget", "Бюджет"],
        ["members", "Участники"],
        ["photos", "Фото"],
      ]
    : [
        ["overview", "Главная"],
        ["route", "Маршрут"],
        ["accommodation", "Жильё"],
        ["bookings", "Транспорт и билеты"],
        ["budget", "Бюджет"],
        ["members", "Участники"],
        ["photos", "Фото"],
      ];
  return (
    <div>
      <header className="trip-header">
        <button
          className="back back-icon"
          onClick={() => go("trips")}
          aria-label="На главную"
          title="На главную"
        >
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path d="M19 12H5m0 0 6-6m-6 6 6 6" />
          </svg>
        </button>
        <div className="trip-heading">
          <div className="trip-title-block">
            <h1>
              {trip.title}{" "}
              <button
                className="status-picker"
                onClick={() => setStatusMenuOpen((open) => !open)}
                aria-expanded={statusMenuOpen}
              >
                ● {trip.status}
              </button>
            </h1>
            <p>
              {trip.isDraft
                ? trip.cities || "Даты, города и маршрут пока не заполнены"
                : trip.dates}
            </p>
            {statusMenuOpen && (
              <div
                className="status-menu"
                role="dialog"
                aria-label="Статус путешествия"
              >
                <b>Статус путешествия</b>
                {["Активное", "Предстоящее", "Черновик", "Завершённое"].map(
                  (status) => (
                    <button
                      className={trip.status === status ? "selected" : ""}
                      onClick={() => {
                        onUpdateTrip({ ...trip, status });
                        setStatusMenuOpen(false);
                      }}
                      key={status}
                    >
                      ● {status}
                    </button>
                  ),
                )}
              </div>
            )}
          </div>
          {tab === "overview" && (
            <button
              className="edit-trip"
              onClick={() => setOverviewEditorOpen(true)}
            >
              ✎ Редактировать
            </button>
          )}
          {!trip.isDraft && (
            <div className="share">
              <div>
                <Avatar>АС</Avatar>
                <Avatar tone="green">МК</Avatar>
                <Avatar tone="blue">ДВ</Avatar>
              </div>
              <button onClick={() => go("public")}>↗ Публичная ссылка</button>
            </div>
          )}
        </div>
        <nav className="tabs">
          {labels.map(([value, label]) => (
            <button
              className={tab === value ? "active" : ""}
              onClick={() => onTabChange(value)}
              key={value}
            >
              {label}
            </button>
          ))}
        </nav>
      </header>
      <main className="workspace">
        {tab === "overview" && (
          <TripOverview trip={trip} onUpdateTrip={onUpdateTrip} />
        )}
        {tab === "route" && (
          <RouteTab
            isDraft={trip.isDraft}
            draftDays={draftDays}
            editingRoadDay={editingRoadDay}
            onEditingRoadDayChange={setEditingRoadDay}
            onAddDraftDay={() =>
              onUpdateTrip({
                ...trip,
                places: undefined,
                days: [...draftDays, { id: crypto.randomUUID(), places: [] }],
              })
            }
            onUpdateDraftDay={(day, changes) =>
              onUpdateTrip({
                ...trip,
                places: undefined,
                days: draftDays.map((item, index) =>
                  index === day ? { ...item, ...changes } : item,
                ),
              })
            }
          />
        )}
        {tab === "sights" && (
          <>
            <Sights
              sights={tripSights}
              days={sightDays}
              defaultCity={trip.cities.split(",")[0]?.trim()}
              onToggle={(id) => {
                const sight = tripSights.find((item) => item.id === id);
                const isCheckbox =
                  document.activeElement instanceof HTMLInputElement &&
                  document.activeElement.type === "checkbox";
                if (isCheckbox) {
                  onUpdateTrip({
                    ...trip,
                    sights: tripSights.map((item) =>
                      item.id === id ? { ...item, done: !item.done } : item,
                    ),
                  });
                  return;
                }
                if (!sight) return;
                window.dispatchEvent(
                  new CustomEvent("ramingo-focus-sight", { detail: id }),
                );
                const query = `${sight.name}, ${sight.city}`;
                window.open(
                  `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`,
                  "_blank",
                  "noopener,noreferrer",
                );
              }}
              onAddDay={(title) =>
                onUpdateTrip({
                  ...trip,
                  sightDaysVersion: 1,
                  sightDays: [...sightDays, { id: crypto.randomUUID(), title }],
                })
              }
              onCreateDay={(dayIndex, city, featured, places, photo, photoPosition) => {
                const dayNumber = dayIndex + 1;
                const currentDaySights = tripSights.filter(
                  (sight) => (sight.walkDay || 1) === dayNumber,
                );
                const currentFeatured =
                  currentDaySights.find(
                    (sight) =>
                      sight.id === "munich-christkindlmarkt" ||
                      sight.id === "verona-signori",
                  ) || currentDaySights[0];
                const newSights = [{ ...featured, subcategory: "Главная достопримечательность" }, ...places]
                  .filter((place) => place.name)
                  .map((place, index) => ({
                    id: crypto.randomUUID(),
                    ...place,
                    city,
                    walkDay: dayNumber,
                    walkOrder: index,
                  }));
                const updatedSights =
                  photo && currentFeatured
                    ? [
                        ...(trip.sights || []).filter(
                          (sight) => sight.id !== currentFeatured.id,
                        ),
                        { ...currentFeatured, photo },
                        ...newSights,
                      ]
                    : [...trip.sights || [], ...newSights];
                onUpdateTrip({
                  ...trip,
                  sightDaysVersion: 1,
                  sightDays: sightDays.map((day, index) =>
                    index === dayIndex ? { ...day, title: city, ...(photo ? { photo, photoPosition } : {}) } : day,
                  ),
                  sights: updatedSights,
                });
              }}
              onRenameDay={(id, title) =>
                onUpdateTrip({
                  ...trip,
                  sightDaysVersion: 1,
                  sightDays: sightDays.map((day) =>
                    day.id === id ? { ...day, title } : day,
                  ),
                })
              }
            />
            <SightNotes
              value={
                trip.sightNotes?.[selectedSightDayId] ||
                (selectedSightDayId === "sights-day-1" &&
                useDemoSightContent
                  ? munichDayOneNotes
                  : useDemoSightContent && selectedSightDayId === "sights-day-2"
                    ? veronaDayTwoNotes
                    : "")
              }
              onChange={(value) =>
                onUpdateTrip({
                  ...trip,
                  sightNotes: {
                    ...trip.sightNotes,
                    [selectedSightDayId]: value,
                  },
                })
              }
            />
          </>
        )}
        {tab === "restaurants" && (
          <Restaurants trip={trip} onUpdateTrip={onUpdateTrip} />
        )}
        {tab === "accommodation" && (
          <Accommodation trip={trip} onUpdateTrip={onUpdateTrip} />
        )}
        {tab === "bookings" && <Bookings />}
        {tab === "budget" && (
          <Budget trip={trip} onUpdateTrip={onUpdateTrip} />
        )}
        {tab === "photos" && <Photos />}
        {tab === "members" && (
          <Members trip={trip} onUpdateTrip={onUpdateTrip} />
        )}
      </main>
      {overviewEditorOpen && (
        <OverviewEditor
          trip={trip}
          onUpdateTrip={onUpdateTrip}
          onClose={() => setOverviewEditorOpen(false)}
        />
      )}
    </div>
  );
}

function Catalog({ go }: { go: (view: View) => void }) {
  const [filter, setFilter] = useState("Все");
  const [query, setQuery] = useState("");
  const filters = ["Все", "Европа", "Азия", "Города", "Природа", "7–10 дней"];
  const matchesFilter = (item: (typeof catalog)[number]) => {
    if (filter === "Все") return true;
    if (filter === "Европа" || filter === "Города")
      return item[0].includes("Италия");
    if (filter === "7–10 дней") return item[2] === "8 дней";
    return false;
  };
  const filteredCatalog = catalog.filter(
    (item) =>
      `${item[0]} ${item[1]}`.toLowerCase().includes(query.toLowerCase()) &&
      matchesFilter(item),
  );
  return (
    <div className="page wide">
      <p className="eyebrow">Сообщество путешественников</p>
      <h1>Каталог маршрутов</h1>
      <div className="search">
        ⌕
        <input
          placeholder="Куда хотите поехать?"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </div>
      <div className="chips">
        {filters.map((value) => (
          <button
            className={filter === value ? "selected" : ""}
            onClick={() => setFilter(value)}
            key={value}
          >
            {value}
          </button>
        ))}
      </div>
      <div className="catalog-grid">
        {filteredCatalog.map((item, index) => (
          <article className="catalog-card" key={item[0]}>
            <div className={`catalog-cover ${item[5]}`}>
              {index === 0 && <span>★ Рекомендуем</span>}
              <b>{item[2]}</b>
            </div>
            <div>
              <h2>{item[0]}</h2>
              <p>{item[1]}</p>
              <footer>
                <span>
                  <Avatar>{item[3].slice(0, 2)}</Avatar>
                  <small>
                    <b>{item[3]}</b>♡ {item[4]}
                  </small>
                </span>
                <button onClick={() => go("public")}>Открыть</button>
              </footer>
            </div>
          </article>
        ))}
        {filteredCatalog.length === 0 && (
          <div className="empty-state">
            По этому запросу маршрутов не найдено.
          </div>
        )}
      </div>
    </div>
  );
}

function PublicRoute({ go }: { go: (view: View) => void }) {
  return (
    <div className="public">
      <header>
        <span>◇ Публичный маршрут · только просмотр</span>
        <button onClick={() => go("catalog")}>← В каталог</button>
      </header>
      <section className="public-hero">
        <div>
          <p>Италия · Рим · Флоренция · Венеция</p>
          <h1>Классическая Италия за 8 дней</h1>
        </div>
      </section>
      <main>
        <div className="author">
          <span>
            <Avatar>АС</Avatar>
            <b>
              Анна Соколова<small>8 дней · 27 мест · ♡ 342</small>
            </b>
          </span>
          <button onClick={() => go("trips")}>Скопировать себе</button>
        </div>
        {days.slice(0, 4).map((day, index) => (
          <section className="public-day" key={day.date}>
            <header>
              <i>{index + 1}</i>
              <h2>
                День {index + 1} · {day.city}
              </h2>
              <span>{day.date}</span>
            </header>
            <div>
              {day.places.slice(0, 3).map((place, placeIndex) => (
                <PlaceRow place={place} index={placeIndex} key={place} />
              ))}
            </div>
          </section>
        ))}
      </main>
    </div>
  );
}

function Auth({
  go,
  onAuthorized,
}: {
  go: (view: View) => void;
  onAuthorized: (name: string) => void;
}) {
  const [mode, setMode] = useState<"register" | "login">("register");
  const [message, setMessage] = useState("");
  const [rememberMe, setRememberMe] = useState(true);
  const isRegister = mode === "register";
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const name = String(formData.get("name") ?? "").trim();
    const email = String(formData.get("email") ?? "")
      .trim()
      .toLowerCase();
    const password = String(formData.get("password") ?? "");
    const accepted = formData.get("terms") === "on";

    if (
      !email ||
      !password ||
      (isRegister && (!name || password.length < 8 || !accepted))
    ) {
      setMessage(
        isRegister
          ? "Заполните все поля, пароль должен содержать не менее 8 символов."
          : "Введите e-mail и пароль.",
      );
      return;
    }

    if (isRegister) {
      setAuthSessionPersistence(true);
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: { data: { full_name: name } },
      });
      if (error) {
        setMessage(error.message);
        return;
      }
      // Supabase returns an obfuscated user instead of an error for an existing
      // address when e-mail confirmation is enabled.
      if (!data.session && data.user?.identities?.length === 0) {
        setMessage("Этот e-mail уже зарегистрирован. Войдите в аккаунт.");
        setMode("login");
        return;
      }
      if (!data.session) {
        setMessage("Аккаунт создан. Подтвердите e-mail, затем войдите.");
        setMode("login");
        return;
      }
      onAuthorized(name);
      setMessage("Аккаунт создан. Открываем ваши путешествия...");
      window.setTimeout(() => go("trips"), 500);
      return;
    }

    setAuthSessionPersistence(rememberMe);
    const { data, error } = await supabase.auth.signInWithPassword({
      email,
      password,
    });
    if (error || !data.user) {
      setMessage(
        error?.message ?? "Не удалось войти. Проверьте e-mail и пароль.",
      );
      return;
    }
    onAuthorized(
      data.user.user_metadata.full_name || data.user.email || "Путешественник",
    );
    setMessage("Вход выполнен. Открываем ваши путешествия...");
    window.setTimeout(() => go("trips"), 500);
  };
  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-form">
          <div className="auth-brand">
            <span>R</span>
            <b>Ramingo</b>
          </div>
          <div className="auth-switch">
            <button
              className={isRegister ? "active" : ""}
              onClick={() => {
                setMode("register");
                setMessage("");
              }}
            >
              Регистрация
            </button>
            <button
              className={!isRegister ? "active" : ""}
              onClick={() => {
                setMode("login");
                setMessage("");
              }}
            >
              Вход
            </button>
          </div>
          <h1>{isRegister ? "Создайте аккаунт" : "С возвращением"}</h1>
          <p>
            {isRegister
              ? "Начните планировать первое путешествие за пару минут."
              : "Войдите, чтобы продолжить планирование путешествий."}
          </p>
          <div className="auth-providers">
            <button>
              <b className="google-mark">G</b> Google
            </button>
            <button>
              <b className="apple-mark">●</b> Apple
            </button>
          </div>
          <div className="auth-divider">
            <span>или через e-mail</span>
          </div>
          <form autoComplete={isRegister ? "off" : "on"} onSubmit={handleSubmit}>
            <label className={isRegister ? "" : "hidden"}>
              Имя
              <input
                name="name"
                placeholder="Введите имя"
                autoComplete={isRegister ? "off" : "name"}
              />
            </label>
            <label>
              E-mail
              <input
                name="email"
                type="email"
                placeholder="you@example.com"
                autoComplete={isRegister ? "off" : "email"}
              />
            </label>
            <label>
              Пароль
              <input
                name="password"
                type="password"
                placeholder={
                  isRegister ? "Минимум 8 символов" : "Введите пароль"
                }
                autoComplete={isRegister ? "new-password" : "current-password"}
              />
            </label>
            {isRegister && (
              <label className="terms">
                <input name="terms" type="checkbox" defaultChecked />{" "}
                <span>
                  Я принимаю <a href="#/terms">условия использования</a> и{" "}
                  <a href="#/privacy">политику конфиденциальности</a>
                </span>
              </label>
            )}
            {!isRegister && (
              <label className="remember">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(event) => setRememberMe(event.target.checked)}
                />
                <span>Запомнить меня</span>
              </label>
            )}
            <button className="auth-submit">
              {isRegister ? "Создать аккаунт" : "Войти"}
            </button>
          </form>
          {message && (
            <p className="auth-message" role="status">
              {message}
            </p>
          )}
          <div className="auth-footer">
            {isRegister ? "Уже есть аккаунт?" : "Впервые в Ramingo?"}{" "}
            <button
              onClick={() => {
                setMode(isRegister ? "login" : "register");
                setMessage("");
              }}
            >
              {isRegister ? "Войти" : "Зарегистрироваться"}
            </button>
          </div>
        </div>
        <aside className="auth-promo">
          <div>
            <p>ПЛАНИРУЙТЕ ВМЕСТЕ</p>
            <h2>
              Маршруты, жильё,
              <br />
              бюджет и<br />
              участники — в<br />
              одном месте
            </h2>
          </div>
        </aside>
      </section>
    </main>
  );
}

type LegalPageKind = "privacy" | "terms";

const legalEntityName =
  import.meta.env.VITE_LEGAL_ENTITY_NAME || "Ramingo";
const legalContactEmail = import.meta.env.VITE_LEGAL_CONTACT_EMAIL || "";
const legalEffectiveDate =
  import.meta.env.VITE_LEGAL_EFFECTIVE_DATE || "7 августа 2026 года";

function LegalPage({ kind }: { kind: LegalPageKind }) {
  const navigate = useNavigate();
  const isPrivacy = kind === "privacy";

  return (
    <main className="legal-page">
      <article className="legal-card">
        <header className="legal-header">
          <a className="legal-brand" href="/#/auth" aria-label="Ramingo">
            <span>R</span>
            <b>Ramingo</b>
          </a>
          <button className="legal-back" type="button" onClick={() => navigate(-1)}>
            Вернуться назад
          </button>
        </header>
        <div className="legal-content">
          <p className="legal-eyebrow">Ramingo · Travel Planner</p>
          <h1>{isPrivacy ? "Политика конфиденциальности" : "Условия использования"}</h1>
          <p className="legal-updated">Последнее обновление: {legalEffectiveDate}</p>

          {isPrivacy ? (
            <>
              <p>
                Эта политика объясняет, какие данные обрабатывает сервис «Ramingo»,
                зачем они нужны и как запросить их удаление. Оператор сервиса —{" "}
                <strong>{legalEntityName}</strong>.
              </p>
              <h2>Какие данные мы обрабатываем</h2>
              <ul>
                <li>
                  данные аккаунта: адрес электронной почты, имя и идентификатор
                  аккаунта провайдера авторизации;
                </li>
                <li>
                  содержимое путешествий: маршруты, даты, города, места, рестораны,
                  жильё, бронирования, бюджет, фотографии и заметки;
                </li>
                <li>
                  данные совместной работы: приглашённые участники, их роли и
                  адреса электронной почты;
                </li>
                <li>
                  технические данные, необходимые для авторизации, защиты аккаунта и
                  работы приложения.
                </li>
              </ul>
              <h2>Зачем это нужно</h2>
              <p>
                Мы используем эти данные для входа в аккаунт, синхронизации
                путешествий между устройствами, совместного планирования, загрузки
                фотографий, отправки приглашений и отображения карт, маршрутов и
                прогноза погоды.
              </p>
              <h2>Сторонние сервисы</h2>
              <p>
                Для работы функций сервис обращается к Supabase (авторизация, база
                данных, функции и хранилище), Mapbox (карты и маршруты), Open-Meteo
                (погода), а также к сервисам изображений и электронной почты,
                подключённым в production-конфигурации. Эти сервисы получают только
                данные, необходимые для соответствующего запроса.
              </p>
              <h2>Публичные ссылки и совместный доступ</h2>
              <p>
                Если вы включаете публичную ссылку или приглашаете участника, часть
                содержимого путешествия становится доступной людям, которым вы
                передали ссылку или приглашение. Не публикуйте в путешествиях данные,
                которыми не хотите делиться.
              </p>
              <h2>Хранение и удаление</h2>
              <p>
                Данные хранятся, пока аккаунт и путешествия используются. Удалить
                аккаунт можно в приложении или на странице{" "}
                <a href="/#/delete-account">удаления аккаунта</a>. При удалении мы
                удаляем аккаунт, связанные путешествия, участников, пользовательские
                данные и фотографии из доступного хранилища.
              </p>
              <h2>Ваши права и контакт</h2>
              <p>
                Вы можете запросить доступ, исправление или удаление своих данных.
                Для этого напишите на{" "}
                {legalContactEmail ? (
                  <a href={`mailto:${legalContactEmail}`}>{legalContactEmail}</a>
                ) : (
                  <strong>контактный адрес издателя не настроен</strong>
                )}
                .
              </p>
            </>
          ) : (
            <>
              <p>
                Используя «Ramingo», вы соглашаетесь с настоящими условиями. Сервис
                предназначен для личного планирования путешествий и совместной работы
                над маршрутами.
              </p>
              <h2>Аккаунт</h2>
              <p>
                Вы отвечаете за актуальность данных аккаунта и сохранность доступа к
                нему. Не передавайте пароль или коды входа другим людям.
              </p>
              <h2>Пользовательский контент</h2>
              <p>
                Вы сохраняете права на добавленные маршруты, фотографии и заметки и
                подтверждаете, что имеете право их использовать. Не добавляйте
                незаконный, вредоносный или чужой контент без разрешения.
              </p>
              <h2>Совместная работа</h2>
              <p>
                Владелец путешествия управляет приглашениями и ролями участников.
                Перед отправкой приглашения убедитесь, что у вас есть основание
                использовать адрес получателя.
              </p>
              <h2>Сторонние сервисы</h2>
              <p>
                Карты, погода, изображения, ссылки на бронирования и другие внешние
                сервисы могут иметь собственные условия и быть временно недоступны.
                «Ramingo» не подтверждает содержание или доступность сторонних сайтов.
              </p>
              <h2>Удаление аккаунта и прекращение доступа</h2>
              <p>
                Вы можете удалить аккаунт в приложении или через{" "}
                <a href="/#/delete-account">страницу удаления аккаунта</a>. Мы можем
                ограничить доступ при нарушении этих условий или угрозе безопасности
                сервиса.
              </p>
              <h2>Контакт</h2>
              <p>
                По вопросам сервиса обращайтесь на{" "}
                {legalContactEmail ? (
                  <a href={`mailto:${legalContactEmail}`}>{legalContactEmail}</a>
                ) : (
                  <strong>контактный адрес издателя не настроен</strong>
                )}
                .
              </p>
            </>
          )}
        </div>
      </article>
    </main>
  );
}

function AccountDeletionPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(true);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!email.trim() || !password) {
      setMessage("Введите e-mail и пароль, чтобы подтвердить удаление аккаунта.");
      return;
    }
    setBusy(true);
    setMessage("");
    setAuthSessionPersistence(!rememberMe);
    const { data, error } = await supabase.auth.signInWithPassword({
      email: email.trim().toLowerCase(),
      password,
    });
    if (error || !data.user) {
      setMessage(error?.message ?? "Не удалось подтвердить аккаунт.");
      setBusy(false);
      return;
    }
    const { error: deleteError } = await supabase.functions.invoke("delete-account", {
      body: {},
    });
    if (deleteError) {
      setMessage(deleteError.message || "Не удалось удалить аккаунт. Попробуйте ещё раз.");
      setBusy(false);
      return;
    }
    await supabase.auth.signOut();
    navigate("/auth", { replace: true });
  };

  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-form">
          <div className="auth-brand">
            <span>R</span>
            <b>Ramingo</b>
          </div>
          <h1>Удаление аккаунта</h1>
          <p>
            Войдите, чтобы подтвердить удаление аккаунта и связанных с ним данных.
            Будут удалены поездки, участники, профиль и загруженные фотографии.
          </p>
          <form onSubmit={handleSubmit}>
            <label>
              E-mail
              <input
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
              />
            </label>
            <label>
              Пароль
              <input
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                type="password"
                autoComplete="current-password"
                placeholder="Введите пароль"
              />
            </label>
            <label className="remember">
              <input
                checked={rememberMe}
                onChange={(event) => setRememberMe(event.target.checked)}
                type="checkbox"
              />
              <span>Не сохранять вход на этом устройстве</span>
            </label>
            {message && <p className="auth-message">{message}</p>}
            <button className="primary" type="submit" disabled={busy}>
              {busy ? "Удаляем аккаунт…" : "Удалить аккаунт"}
            </button>
          </form>
          <button className="auth-link" type="button" onClick={() => navigate("/auth")}>
            Вернуться ко входу
          </button>
        </div>
      </section>
    </main>
  );
}

export function App() {
  const location = useLocation();
  const navigate = useNavigate();
  const tripMatch = matchPath("/trips/:tripId/:tab?", location.pathname);
  const routeTripId = tripMatch?.params.tripId;
  const routeTab = ([
    "overview",
    "route",
    "sights",
    "restaurants",
    "accommodation",
    "bookings",
    "budget",
    "photos",
    "members",
  ] as Tab[]).includes(tripMatch?.params.tab as Tab)
    ? (tripMatch?.params.tab as Tab)
    : "overview";
  const view: View = tripMatch
    ? "trip"
    : location.pathname === "/create"
      ? "create"
      : location.pathname === "/catalog"
        ? "catalog"
        : location.pathname === "/public"
          ? "public"
          : location.pathname === "/delete-account"
            ? "delete-account"
            : location.pathname === "/privacy"
              ? "privacy"
              : location.pathname === "/terms"
                ? "terms"
          : location.pathname === "/trips"
            ? "trips"
            : "auth";
  const [menu, setMenu] = useState(false);
  const [storedPayload, setStoredPayload] = useState<StoredTripPayload | null>(
    null,
  );
  const [drafts, setDrafts] = useState<TripSummary[]>([]);
  const [activeTrip, setActiveTrip] = useState<TripSummary>(trips[0]);
  const [profileName, setProfileName] = useState("Путешественник");
  const [authReady, setAuthReady] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const go = (next: View, tripId = activeTrip.id) => {
    const paths: Record<Exclude<View, "trip">, string> = {
      auth: "/auth",
      trips: "/trips",
      create: "/create",
      catalog: "/catalog",
      public: "/public",
      "delete-account": "/delete-account",
      privacy: "/privacy",
      terms: "/terms",
    };
    navigate(next === "trip" ? `/trips/${tripId}/overview` : paths[next]);
    setMenu(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  useEffect(() => {
    const setAuthenticatedUser = (
      user: { email?: string; user_metadata: { full_name?: string } },
      shouldNavigate = false,
    ) => {
      setProfileName(
        user.user_metadata.full_name || user.email || "Путешественник",
      );
      if (!shouldNavigate) return;
      const nextPath = new URLSearchParams(window.location.search).get("next");
      const inviteTrip = nextPath && matchPath("/trips/:tripId/:tab?", nextPath);
      if (location.pathname === "/auth" || location.pathname === "/") {
        navigate(inviteTrip ? nextPath : "/trips", { replace: true });
      }
    };
    const loadSavedTrip = async () => {
      const { data, error } = await supabase
        .from("trip_state")
        .select("payload")
        .eq("id", "main")
        .maybeSingle();
      if (error) {
        console.error("Could not load the saved trip.", error);
        return;
      }
      const payload = data?.payload as StoredTripPayload | undefined;
      const trip = payload && savedTrip(payload);
      if (!payload || !trip) return;
      setStoredPayload(payload);
      setDrafts((items) =>
        items.some((item) => item.id === trip.id) ? items : [...items, trip],
      );
    };
    const loadUserData = async (userId: string) => {
      const { data, error } = await supabase
        .from("trips")
        .select("id,payload");
      if (error) {
        console.error("Could not load trips.", error);
        return;
      }
      const remoteDrafts = ((data || []) as TripRow[])
        .map(tripFromRow)
        .filter((trip): trip is TripSummary => trip !== null);
      setDrafts((current) => [
        ...remoteDrafts,
        ...current.filter(
          (trip) =>
            trip.id === "supabase-main" &&
            !remoteDrafts.some((remote) => remote.id === trip.id),
        ),
      ]);
      setActiveTrip((current) =>
        remoteDrafts.find((trip) => trip.id === current.id) ||
        remoteDrafts[0] ||
        current,
      );
    };
    void supabase.auth.getSession().then(async ({ data }) => {
      setAuthReady(true);
      if (!data.session?.user) {
        setIsAuthenticated(false);
        if (location.pathname !== "/auth") {
          const next = `${location.pathname}${location.search}`;
          navigate(`/auth?next=${encodeURIComponent(next)}`, { replace: true });
        }
        return;
      }
      setIsAuthenticated(true);
      setAuthenticatedUser(data.session.user, true);
      void loadUserData(data.session.user.id);
      void loadSavedTrip();
    });
    const { data: listener } = supabase.auth.onAuthStateChange(
      (event, session) => {
        if (session?.user) {
          setAuthReady(true);
          setIsAuthenticated(true);
          setAuthenticatedUser(session.user, event === "SIGNED_IN");
          if (event === "SIGNED_IN") {
            void loadUserData(session.user.id);
            void loadSavedTrip();
          }
        } else if (event === "SIGNED_OUT") {
          setIsAuthenticated(false);
          navigate("/auth", { replace: true });
        }
      },
    );
    return () => listener.subscription.unsubscribe();
  }, []);
  useEffect(() => {
    const trip = [...drafts, ...trips].find((item) => item.id === routeTripId);
    if (trip) setActiveTrip(trip);
  }, [drafts, routeTripId]);
  const updateTrip = (trip: TripSummary) => {
    setActiveTrip(trip);
    setDrafts((items) =>
      items.map((item) => (item.id === trip.id ? trip : item)),
    );
    saveTripToSupabase(trip);
    if (trip.id !== "supabase-main" || !storedPayload?.data) return;
    const currentDays = storedPayload.data.days || [];
    const updatedDays: StoredDay[] = (trip.days || []).map((day, index) => {
      const existing = currentDays[index] || {};
      const leg = day.roadLeg;
      return {
        ...existing,
        id: existing.id || day.id,
        city: leg ? `${leg.from} → ${leg.to}` : existing.city,
        dayMapUrl:
          leg?.mapsUrl ||
          (leg ? mapsUrl(leg.from, leg.to) : existing.dayMapUrl),
        checkInFrom: leg?.checkInFrom || undefined,
        checkInTo: leg?.checkInTo || undefined,
        checkOutFrom: leg?.checkOutFrom || undefined,
        checkOutTo: leg?.checkOutTo || undefined,
        completed: leg?.completed || [],
        items: day.places.map((title, itemIndex) => ({
          ...existing.items?.[itemIndex],
          id: existing.items?.[itemIndex]?.id || crypto.randomUUID(),
          title,
        })),
      };
    });
    const nextPayload: StoredTripPayload = {
      ...storedPayload,
      data: {
        ...storedPayload.data,
        days: updatedDays,
        sights: trip.sights,
        trip: {
          ...storedPayload.data.trip,
          title: trip.title,
          start: trip.startDate,
          end: trip.endDate,
          isDraft: true,
          status: trip.status,
          coverImage: trip.coverImage,
          coverPhotos: trip.coverPhotos,
          coverTextColor: trip.coverTextColor,
          overviewMapPoints: trip.overviewMapPoints,
          sightDays: trip.sightDays,
          sightDaysVersion: trip.sightDaysVersion,
          sightNotes: trip.sightNotes,
          members: trip.members,
          publicLinkEnabled: trip.publicLinkEnabled,
          published: trip.published,
        },
      },
    };
    setStoredPayload(nextPayload);
    void supabase
      .from("trip_state")
      .update({ payload: nextPayload })
      .eq("id", "main")
      .then(({ error }) => {
        if (error) console.error("Could not save the trip.", error);
      });
  };
  const toggleSight = (id: string) => {
    if (!storedPayload?.data?.sights) return;
    const nextPayload: StoredTripPayload = {
      ...storedPayload,
      data: {
        ...storedPayload.data,
        sights: storedPayload.data.sights.map((sight) =>
          sight.id === id ? { ...sight, done: !sight.done } : sight,
        ),
      },
    };
    setStoredPayload(nextPayload);
    void supabase
      .from("trip_state")
      .update({ payload: nextPayload })
      .eq("id", "main")
      .then(({ error }) => {
        if (error) console.error("Could not save the sight.", error);
      });
  };
  if (
    !authReady ||
    (!isAuthenticated &&
      view !== "auth" &&
      view !== "delete-account" &&
      view !== "privacy" &&
      view !== "terms") ||
    (isAuthenticated && view === "auth")
  ) {
    return null;
  }
  if (view === "delete-account") return <AccountDeletionPage />;
  if (view === "privacy") return <LegalPage kind="privacy" />;
  if (view === "terms") return <LegalPage kind="terms" />;
  if (view === "auth")
    return (
      <Auth
        go={go}
        onAuthorized={(name) => {
          setProfileName(name);
          setIsAuthenticated(true);
        }}
      />
    );
  return (
    <div className="app">
      <Sidebar
        view={view}
        go={go}
        open={menu}
        close={() => setMenu(false)}
        profileName={profileName}
      />
      <div className="main">
        <button className="menu-button" onClick={() => setMenu(true)}>
          ☰
        </button>
        {view === "trips" && (
          <Trips
            go={go}
            profileName={profileName}
            drafts={drafts}
            onUpdateTrip={updateTrip}
            onOpenTrip={(trip) => {
              setActiveTrip(trip);
              go("trip", trip.id);
            }}
          />
        )}
        {view === "create" && (
          <CreateTrip
            go={go}
            onCreate={(trip) => {
              setDrafts((items) => [...items, trip]);
              setActiveTrip(trip);
              saveTripToSupabase(trip);
              go("trip", trip.id);
            }}
          />
        )}
        {view === "trip" && (() => {
          const routeTrip = [...drafts, ...trips].find(
            (trip) => trip.id === routeTripId,
          );
          if (!routeTrip) return <main className="workspace">Загрузка путешествия...</main>;
          return (
            <Workspace
              go={go}
              trip={routeTrip}
              onUpdateTrip={updateTrip}
              tab={routeTab}
              onTabChange={(tab) => navigate(`/trips/${routeTrip.id}/${tab}`)}
            />
          );
        })()}
        {view === "catalog" && <Catalog go={go} />}
        {view === "public" && <PublicRoute go={go} />}
      </div>
    </div>
  );
}
