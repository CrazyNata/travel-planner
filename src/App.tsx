import {
  useEffect,
  useEffectEvent,
  useRef,
  useState,
  type FormEvent,
  type InputHTMLAttributes,
  type ReactNode,
} from "react";
import type { Map } from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import { matchPath, useLocation, useNavigate } from "react-router-dom";
import { setAuthSessionPersistence, supabase } from "./supabase";
import { AccommodationPrototype } from "./AccommodationPrototype";

type View =
  | "auth"
  | "trips"
  | "create"
  | "trip"
  | "catalog"
  | "public"
  | "delete-account"
  | "privacy"
  | "terms"
  | "housing-preview";
type Tab =
  | "overview"
  | "route"
  | "sights"
  | "restaurants"
  | "accommodation"
  | "bookings"
  | "budget"
  | "pets"
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
  date?: string;
  description?: string;
  textColor?: string;
};
let weatherCoverPhotos: CoverPhoto[] = [];
const tripPhotoUrlLifetimeSeconds = 24 * 60 * 60;

function tripPhotoPath(url: string) {
  const storageUrl = url.match(/^storage:\/\/([^/]+)\/(.+)$/);
  if (storageUrl?.[1] === "trip-photos") {
    try {
      return decodeURIComponent(storageUrl[2]);
    } catch {
      return storageUrl[2];
    }
  }
  try {
    const pathname = new URL(url).pathname;
    const match = pathname.match(
      /\/storage\/v1\/object\/(?:public|sign)\/trip-photos\/(.+)$/,
    );
    return match ? decodeURIComponent(match[1]) : null;
  } catch {
    return null;
  }
}

function mapTripPhotoUrls<T>(
  value: T,
  mapUrl: (url: string, path: string) => string,
): T {
  if (typeof value === "string") {
    const path = tripPhotoPath(value);
    return (path ? mapUrl(value, path) : value) as T;
  }
  if (Array.isArray(value)) {
    return value.map((item) => mapTripPhotoUrls(item, mapUrl)) as T;
  }
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [
        key,
        mapTripPhotoUrls(item, mapUrl),
      ]),
    ) as T;
  }
  return value;
}

function collectTripPhotoPaths(value: unknown, paths = new Set<string>()) {
  if (typeof value === "string") {
    const path = tripPhotoPath(value);
    if (path) paths.add(path);
  } else if (Array.isArray(value)) {
    value.forEach((item) => collectTripPhotoPaths(item, paths));
  } else if (value && typeof value === "object") {
    Object.values(value).forEach((item) => collectTripPhotoPaths(item, paths));
  }
  return paths;
}

async function signTripPhotoUrls<T>(value: T): Promise<T> {
  const paths = [...collectTripPhotoPaths(value)];
  if (!paths.length) return value;
  const { data, error } = await supabase.storage
    .from("trip-photos")
    .createSignedUrls(paths, tripPhotoUrlLifetimeSeconds);
  if (error) {
    console.error("Could not load private trip photos.", error);
    return value;
  }
  const signedUrls = new globalThis.Map(
    (data || []).flatMap((item) =>
      item.signedUrl ? [[item.path, item.signedUrl] as const] : [],
    ),
  );
  return mapTripPhotoUrls(value, (url, path) => signedUrls.get(path) || url);
}

function canonicalTripPhotoUrls<T>(value: T): T {
  return mapTripPhotoUrls(value, (_url, path) =>
    supabase.storage.from("trip-photos").getPublicUrl(path).data.publicUrl,
  );
}

async function signedTripPhotoUrl(path: string) {
  const { data, error } = await supabase.storage
    .from("trip-photos")
    .createSignedUrl(path, tripPhotoUrlLifetimeSeconds);
  if (error) throw error;
  return data.signedUrl;
}

const defaultSightPhotos = [
  "/sight-photos/munich-square.png",
  "/sight-photos/munich-street.png",
  "/sight-photos/munich-gate.png",
];
type TripMember = {
  id: string;
  initials: string;
  name: string;
  email: string;
  role: "Владелец" | "Редактор" | "Читатель";
  tone: "sand" | "green" | "blue";
};
type PetPlace = {
  id: string;
  name: string;
  city: string;
  type: "shop" | "vet";
  address: string;
  rating?: number;
  reviewCount?: number;
  photoUrl?: string;
  photoName?: string;
  googlePlaceId?: string;
  mapsUrl?: string;
  latitude?: number;
  longitude?: number;
  note?: string;
  phone?: string;
  distanceKm?: number;
  openNow?: boolean;
  is24h?: boolean;
};
type RememberedAccount = {
  email: string;
  name: string;
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
  photos?: CoverPhoto[];
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
  petPlaces?: PetPlace[];
  members?: TripMember[];
  publicLinkEnabled?: boolean;
  published?: boolean;
};
type TripRow = {
  id: string;
  payload: TripSummary;
  owner_id?: string;
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
  googleRating?: number;
  googleReviews?: number;
  photoTransforms?: {
    offsetX?: number;
    offsetY?: number;
    scaleX?: number;
    scaleY?: number;
  }[];
};
type ImportedAccommodation = {
  id: string;
  name: string;
  city: string;
  address?: string;
  description?: string;
  link?: string;
  googleRating?: number;
  googleReviews?: number;
  photos?: string[];
  photoNames?: string[];
  placeType?: string;
};
type ImportedRestaurant = {
  id: string;
  name: string;
  city: string;
  lnglat?: [number, number];
  status: string;
  cuisine?: string;
  note?: string;
  link?: string;
  price?: string;
  googleRating?: number;
  googleReviews?: number;
  photos?: string[];
  photoNames?: string[];
  placeType?: string;
  categories?: string[];
  priority?: boolean;
  dogFriendly?: boolean;
};
const restaurantCuisineOptions = [
  "Итальянская",
  "Тоскана",
  "Тосканская",
  "Римская",
  "Средиземноморская",
  "Европейская",
  "Французская",
  "Испанская",
  "Чешская",
  "Австрийская",
  "Немецкая",
  "Баварская",
  "Японская",
  "Китайская",
  "Тайская",
  "Индийская",
  "Мексиканская",
  "Американская",
  "Грузинская",
  "Турецкая",
  "Морепродукты",
  "Пицца",
  "Стейкхаус",
  "Вегетарианская",
  "Кофейня и выпечка",
];
type BudgetScope = "общий" | "семья" | "личный";
type BudgetCurrency = "EUR" | "RUB" | "CZK";
type BudgetExpense = {
  id: string;
  name: string;
  amount: number;
  /** Stored as the EUR-normalized amount for backwards-compatible totals. */
  currency?: BudgetCurrency;
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

const budgetCategories = [
  "Жильё",
  "Транспорт",
  "Еда и рестораны",
  "Активности и билеты",
  "Прочее",
];

function inferBudgetCategory(name: string) {
  const value = name.trim().toLocaleLowerCase("ru-RU");
  if (!value) return undefined;
  if (/(жиль|отел|гостини|апартамент|хостел|airbnb|booking)/.test(value)) {
    return "Жильё";
  }
  if (/(аренд.*(машин|авто)|бензин|такси|транспорт|поезд|перелет|самолет|дорог.*билет)/.test(value)) {
    return "Транспорт";
  }
  if (/(еда|ресторан|обед|ужин|завтрак|кафе|пицц|продукт|бар)/.test(value)) {
    return "Еда и рестораны";
  }
  if (/(дневн.*трат|непредвид|прочее)/.test(value)) {
    return "Прочее";
  }
  if (/(экскурс|музей|достопримеч|актив|развлеч|парк)/.test(value)) {
    return "Активности и билеты";
  }
  return undefined;
}

function normalizeBudgetExpense(expense: BudgetExpense) {
  const inferred = inferBudgetCategory(expense.name);
  return inferred && expense.category === "Еда и рестораны" && inferred !== expense.category
    ? { ...expense, category: inferred }
    : expense;
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
  googleRating?: number;
  googleReviews?: number;
  link?: string;
  photoNames?: string[];
};
type DayPlaceDraft = Pick<
  StoredSight,
  | "name"
  | "subcategory"
  | "description"
  | "photo"
  | "photoPosition"
  | "lnglat"
  | "googleRating"
  | "googleReviews"
>;
type SightRating = { score: number; reviews: number };

const sightRatingPresets: { match: RegExp; score: number; reviews: number }[] = [
  { match: /marienplatz/i, score: 4.7, reviews: 37200 },
  { match: /neues rathaus|новая ратуш/i, score: 4.7, reviews: 29400 },
  { match: /frauenkirche/i, score: 4.7, reviews: 16800 },
  { match: /piazza bra|пьяцца бра/i, score: 4.7, reviews: 31200 },
  { match: /piazza delle erbe|пьяцца делле эрбе/i, score: 4.6, reviews: 18900 },
  { match: /casa di giulietta|дом джульетты|дворик джульетты/i, score: 4.4, reviews: 16400 },
  { match: /ponte pietra|понте пьетра/i, score: 4.7, reviews: 8700 },
  { match: /colosse|колиз|colosseo/i, score: 4.8, reviews: 336000 },
  { match: /trevi|треви/i, score: 4.8, reviews: 112000 },
  { match: /pantheon|пантеон/i, score: 4.8, reviews: 82000 },
  { match: /venice|венеци|san marco|сан марко/i, score: 4.7, reviews: 90000 },
  { match: /pražský hrad|пражск.*град|charles bridge|карлов.*мост/i, score: 4.8, reviews: 78000 },
];

function sightRatingFor(sight: StoredSight): SightRating {
  if (typeof sight.googleRating === "number" && Number.isFinite(sight.googleRating)) {
    return {
      score: sight.googleRating,
      reviews: typeof sight.googleReviews === "number" && Number.isFinite(sight.googleReviews)
        ? Math.trunc(sight.googleReviews)
        : 0,
    };
  }
  const preset = sightRatingPresets.find(({ match }) => match.test(sight.name));
  if (preset) return { score: preset.score, reviews: preset.reviews };
  let hash = 0;
  for (const character of `${sight.city}:${sight.name}`) {
    hash = (hash * 31 + character.charCodeAt(0)) >>> 0;
  }
  return {
    score: 4.5 + (hash % 5) / 10,
    reviews: 1200 + (hash % 23800),
  };
}

function formatSightReviews(reviews: number) {
  return reviews.toLocaleString("ru-RU");
}

const sightDescriptionPresets: { match: RegExp; text: string }[] = [
  { match: /piazza navona/i, text: "Барочная площадь Рима с фонтаном Четырёх рек, дворцами и уличными кафе." },
  { match: /fontana dei quattro fiumi|фонтан четыр[ёе]х рек/i, text: "Знаменитый барочный фонтан Бернини в центре площади Навона." },
  { match: /sant['’]?agnese|chiesa.*agnese/i, text: "Барочная церковь на площади Навона с выразительным фасадом и богатым интерьером." },
  { match: /piazza venezia/i, text: "Монументальная площадь в центре Рима у Витториано и главных исторических улиц." },
  { match: /arena di verona|арена вероны/i, text: "Римский амфитеатр, где проходят оперные спектакли и большие городские события." },
  { match: /charles bridge|карлов.*мост|praz(?:sky|ský) hrad|пражск.*мост/i, text: "Средневековый мост через Влтаву со статуями и видами на исторический центр." },
  { match: /marienplatz/i, text: "Главная площадь Мюнхена с Новой ратушей, часами Глокеншпиля и рождественской ярмаркой." },
];

function sightDescriptionFor(sight: StoredSight) {
  if (sight.description?.trim()) return sight.description.trim();
  const label = `${sight.name} ${sight.city}`.toLowerCase();
  const preset = sightDescriptionPresets.find(({ match }) => match.test(label));
  if (preset) return preset.text;
  if (/площад|piazza|square/.test(label)) {
    return "Историческая площадь с красивой архитектурой, городской жизнью и местами для прогулки.";
  }
  if (/собор|церк|базилик|kirche|duomo|basilica|church|chiesa/.test(label)) {
    return "Исторический храм с выразительным фасадом, интересным интерьером и атмосферой старого города.";
  }
  if (/ярмарк|рынок|market|christkindlmarkt|елк|подсветк|lights/.test(label)) {
    return "Праздничная локация с огнями, ярмарочными домиками, местными угощениями и сувенирами.";
  }
  if (/улиц|via |corso|straß|strasse|street|квартал/.test(label)) {
    return "Прогулочная улица с историческими фасадами, магазинами, кафе и атмосферой центра города.";
  }
  if (/мост|ponte|bridge/.test(label)) {
    return "Живописная точка маршрута с исторической архитектурой и видами на воду и старый город.";
  }
  if (/замок|дворец|palazzo|residenz|castle|palace/.test(label)) {
    return "Исторический комплекс с парадными залами, красивыми дворами и архитектурными деталями.";
  }
  if (/фонтан|fontan/.test(label)) {
    return "Знаковая городская достопримечательность с выразительной скульптурой и историей.";
  }
  if (/парк|сад|garden|parco|hofgarten/.test(label)) {
    return "Спокойное место для прогулки среди зелени, архитектуры и городских видов.";
  }
  if (/набереж|реки|канал|canal|tiber|arno|lagoon|берег/.test(label)) {
    return "Живописная прогулка вдоль воды с видами на город, мосты и исторические здания.";
  }
  if (/башн|tower|torre/.test(label)) {
    return "Историческая башня с характерным силуэтом и красивым видом на город.";
  }
  if (/смотров|панорам|вид|view|panorama/.test(label)) {
    return "Смотровая точка с панорамой города и отличными возможностями для фотографий.";
  }
  if (/музе|museum/.test(label)) {
    return "Место для знакомства с историей, искусством и культурой города.";
  }
  if (/театр|теат|scala/.test(label)) {
    return "Знаковое культурное место с богатой историей, красивым фасадом и особой атмосферой.";
  }
  if (/монумент|стату|statue|monument/.test(label)) {
    return "Памятник, который помогает лучше почувствовать историю и характер города.";
  }
  return `Историческая достопримечательность «${sight.name}» в городе ${sight.city}: архитектура, история и место для прогулки.`;
}
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
      photos?: CoverPhoto[];
      coverTextColor?: string;
      overviewMapPoints?: string[];
      sightDays?: { id: string; title: string; photo?: string; photoPosition?: number }[];
      sightDaysVersion?: number;
      sightNotes?: Record<string, string>;
      petPlaces?: PetPlace[];
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

function parseTripDateRange(value: string) {
  const months: Record<string, number> = {
    январь: 1, января: 1, февраль: 2, февраля: 2, март: 3, марта: 3,
    апрель: 4, апреля: 4, май: 5, мая: 5, июнь: 6, июня: 6,
    июль: 7, июля: 7, август: 8, августа: 8, сентябрь: 9, сентября: 9,
    октябрь: 10, октября: 10, ноябрь: 11, ноября: 11, декабрь: 12, декабря: 12,
  };
  const match = value.toLowerCase().match(
    /(\d{1,2})\s+([а-яё]+)\s+(\d{4})\s*[–—-]\s*(\d{1,2})\s+([а-яё]+)\s+(\d{4})/,
  );
  if (!match) return null;
  const startMonth = months[match[2]];
  const endMonth = months[match[5]];
  if (!startMonth || !endMonth) return null;
  const iso = (year: string, month: number, day: string) =>
    `${year}-${String(month).padStart(2, "0")}-${day.padStart(2, "0")}`;
  return [iso(match[3], startMonth, match[1]), iso(match[6], endMonth, match[4])] as const;
}

function normalizeTripDates(dates: string) {
  const match = dates.match(
    /^(\d{4}-\d{2}-\d{2})\s*[–-]\s*(\d{4}-\d{2}-\d{2})$/,
  );
  return match ? formatTripDates(match[1], match[2]) : dates;
}

function cityFlag(city: string) {
  const country = city.includes("Прага")
    ? "cz"
    : city.includes("Зальцбург")
      ? "at"
      : city.includes("Мюнхен") || city.includes("Равенсбург") ||
        city.includes("Инцелль") || city.includes("Инцель") ||
        city.includes("Германия")
        ? "de"
        : city.includes("Сан-Марино")
          ? "sm"
          : (
    [
      "Верона",
      "Рим",
      "Пиза",
      "Фильине",
      "Кьоджа",
      "Милан",
      "Вальдидентро",
      "Флоренция",
      "Венеция",
    ].some((name) => city.includes(name))
            ? "it"
            : null);
  if (!country) return null;
  const labels = {
    cz: "Чехия",
    at: "Австрия",
    de: "Германия",
    sm: "Сан-Марино",
    it: "Италия",
  };
  return (
    <span className="city-flag" role="img" aria-label={labels[country]}>
      <svg viewBox="0 0 24 16" aria-hidden="true">
        {country === "cz" && (
          <>
            <path fill="#fff" d="M0 0h24v8H0z" />
            <path fill="#d7141a" d="M0 8h24v8H0z" />
            <path fill="#11457e" d="m0 0 10 8-10 8z" />
          </>
        )}
        {country === "at" && (
          <>
            <path fill="#ed2939" d="M0 0h24v16H0z" />
            <path fill="#fff" d="M0 5.33h24v5.34H0z" />
          </>
        )}
        {country === "de" && (
          <>
            <path fill="#151515" d="M0 0h24v5.34H0z" />
            <path fill="#d00" d="M0 5.33h24v5.34H0z" />
            <path fill="#ffce00" d="M0 10.66h24V16H0z" />
          </>
        )}
        {country === "it" && (
          <>
            <path fill="#009246" d="M0 0h8v16H0z" />
            <path fill="#fff" d="M8 0h8v16H8z" />
            <path fill="#ce2b37" d="M16 0h8v16h-8z" />
          </>
        )}
        {country === "sm" && (
          <>
            <path fill="#fff" d="M0 0h24v8H0z" />
            <path fill="#5eb6e4" d="M0 8h24v8H0z" />
            <circle cx="12" cy="8" r="2.3" fill="#f5c242" stroke="#55764f" strokeWidth=".7" />
          </>
        )}
      </svg>
    </span>
  );
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
    photos: payload.data?.trip?.photos,
    coverTextColor: payload.data?.trip?.coverTextColor,
    overviewMapPoints: payload.data?.trip?.overviewMapPoints,
    sights: payload.data?.sights,
    sightDays: payload.data?.trip?.sightDays,
    sightDaysVersion: payload.data?.trip?.sightDaysVersion,
    sightNotes: payload.data?.trip?.sightNotes,
    petPlaces: payload.data?.trip?.petPlaces,
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

function markTripOwner(trip: TripSummary, ownerId?: string) {
  if (!trip.members?.length) return trip;
  if (trip.members.some((member) => member.role === "Владелец")) return trip;

  // Older trips did not persist the owner role in their payload. The
  // owner_id column is the source of truth; member ids may differ between
  // legacy and current invitations, so fall back to the first (owner) row.
  const owner = trip.members.find((member) => member.id === ownerId) || trip.members[0];

  return {
    ...trip,
    members: trip.members.map((member) =>
      member.id === owner.id ? { ...member, role: "Владелец" as const } : member,
    ),
  };
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
      const payload = canonicalTripPhotoUrls(trip);
      const { data: updated, error: updateError } = await supabase
        .from("trips")
        .update({ payload })
        .eq("id", trip.id)
        .select("id");
      if (updateError) throw updateError;
      if (updated?.length) return;

      const { data: membership, error: membershipError } = await supabase
        .from("trip_collaborators")
        .select("role")
        .eq("trip_id", trip.id)
        .eq("user_id", session.user.id)
        .maybeSingle();
      if (membershipError) throw membershipError;
      if (membership) {
        throw new Error(
          membership.role === "Читатель"
            ? "Trip is read-only for this user."
            : "Could not update the shared trip.",
        );
      }

      const { error: insertError } = await supabase.from("trips").insert({
        id: trip.id,
        owner_id: session.user.id,
        payload,
      });
      if (insertError) throw insertError;
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
  Инцелль: [12.75, 47.76],
  Инцель: [12.75, 47.76],
  Мюнхен: [11.582, 48.1351],
  Равенсбург: [9.611, 47.781],
  Верона: [10.9916, 45.4384],
  Рим: [12.4964, 41.9028],
  Пиза: [10.4017, 43.7228],
  "Фильине-Вальдарно": [11.469, 43.62],
  "Сан-Марино": [12.4578, 43.9424],
  Кьоджа: [12.278, 45.219],
  Chioggia: [12.278, 45.219],
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
  "Аликанте, Испания",
  "Афины, Греция",
  "Братислава, Словакия",
  "Брюссель, Бельгия",
  "Варшава, Польша",
  "Глазго, Великобритания",
  "Грац, Австрия",
  "Дрезден, Германия",
  "Дубровник, Хорватия",
  "Женева, Швейцария",
  "Загреб, Хорватия",
  "Инсбрук, Австрия",
  "Копенгаген, Дания",
  "Краков, Польша",
  "Лион, Франция",
  "Любляна, Словения",
  "Марсель, Франция",
  "Ницца, Франция",
  "Осло, Норвегия",
  "Порту, Португалия",
  "Пиза, Италия",
  "Рига, Латвия",
  "Севилья, Испания",
  "София, Болгария",
  "Стокгольм, Швеция",
  "Таллин, Эстония",
  "Тбилиси, Грузия",
  "Токио, Япония",
  "Хельсинки, Финляндия",
  "Валенсия, Испания",
  "Вильнюс, Литва",
  "Чикаго, США",
  "Эдинбург, Великобритания",
  "Кейптаун, ЮАР",
  "Киото, Япония",
  "Майами, США",
  "Мельбурн, Австралия",
  "Москва, Россия",
  "Монреаль, Канада",
  "Нью-Йорк, США",
  "Сингапур, Сингапур",
  "Сидней, Австралия",
  "Санкт-Петербург, Россия",
  "Казань, Россия",
  "Сочи, Россия",
  "Екатеринбург, Россия",
  "Новосибирск, Россия",
  "Торонто, Канада",
  "Ванкувер, Канада",
  "Вашингтон, США",
  "Кастель-Гандольфо, Италия",
  "Озеро Комо, Италия",
];

function externalUrl(value: string) {
  if (!value) return "#";
  return /^https?:\/\//i.test(value) ? value : `https://${value}`;
}

const accommodationMonthNumbers: Record<string, number> = {
  янв: 1,
  января: 1,
  январь: 1,
  feb: 2,
  фев: 2,
  февраля: 2,
  февраль: 2,
  mar: 3,
  мар: 3,
  марта: 3,
  март: 3,
  apr: 4,
  апр: 4,
  апреля: 4,
  апрель: 4,
  may: 5,
  май: 5,
  мая: 5,
  июн: 6,
  июня: 6,
  июнь: 6,
  jun: 6,
  июл: 7,
  июля: 7,
  июль: 7,
  jul: 7,
  авг: 8,
  августа: 8,
  август: 8,
  aug: 8,
  сен: 9,
  сент: 9,
  сентября: 9,
  сентябрь: 9,
  sep: 9,
  окт: 10,
  октября: 10,
  октябрь: 10,
  oct: 10,
  ноя: 11,
  ноября: 11,
  ноябрь: 11,
  nov: 11,
  дек: 12,
  декабря: 12,
  декабрь: 12,
  dec: 12,
};

function accommodationDateParts(value?: string) {
  const raw = value?.trim() || "";
  if (!raw) return { checkIn: "", checkOut: "" };

  // Keep the canonical values already stored by the date picker unchanged.
  const isoDates = raw.match(/\b\d{4}-\d{2}-\d{2}\b/g) || [];
  if (isoDates.length >= 2) {
    return { checkIn: isoDates[0] || "", checkOut: isoDates[1] || "" };
  }
  if (isoDates.length === 1) return { checkIn: isoDates[0] || "", checkOut: "" };

  // Imported/catalogue stays can use a display range such as
  // "25–26 сен · 1 ночь" or "30 сен–1 окт · 1 ночь". Convert it to the
  // value expected by <input type="date"> instead of passing "25" to Date.
  const range = raw.match(
    /^\s*(\d{1,2})(?:\s+([A-Za-zА-Яа-яЁё]+))?\s*[–-]\s*(\d{1,2})(?:\s+([A-Za-zА-Яа-яЁё]+))?/,
  );
  if (!range) return { checkIn: "", checkOut: "" };

  const firstMonth = range[2]
    ? accommodationMonthNumbers[range[2].toLowerCase().replace(/\.$/, "")]
    : undefined;
  const secondMonth = range[4]
    ? accommodationMonthNumbers[range[4].toLowerCase().replace(/\.$/, "")]
    : undefined;
  const monthForFirst = firstMonth || secondMonth;
  const monthForSecond = secondMonth || firstMonth;
  if (!monthForFirst || !monthForSecond) return { checkIn: "", checkOut: "" };

  const year = Number(raw.match(/\b(20\d{2})\b/)?.[1] || new Date().getFullYear());
  const secondYear = secondMonth && firstMonth && secondMonth < firstMonth ? year + 1 : year;
  const toIso = (day: string, month: number, dateYear: number) => {
    const date = new Date(Date.UTC(dateYear, month - 1, Number(day)));
    return date.getUTCDate() === Number(day) && date.getUTCMonth() === month - 1
      ? date.toISOString().slice(0, 10)
      : "";
  };

  return {
    checkIn: toIso(range[1], monthForFirst, year),
    checkOut: toIso(range[3], monthForSecond, secondYear),
  };
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

function accommodationStartTime(stay: SavedAccommodation) {
  const start = accommodationDateParts(stay.dates).checkIn;
  const timestamp = Date.parse(`${start}T00:00:00Z`);
  return Number.isFinite(timestamp) ? timestamp : Number.POSITIVE_INFINITY;
}

type AccommodationCurrency =
  | "EUR"
  | "USD"
  | "GBP"
  | "RUB"
  | "CZK"
  | "PLN"
  | "CHF"
  | "HUF"
  | "TRY"
  | "JPY";

const accommodationCurrencies: {
  value: AccommodationCurrency;
  label: string;
  symbol: string;
}[] = [
  { value: "EUR", label: "EUR", symbol: "€" },
  { value: "USD", label: "USD", symbol: "$" },
  { value: "GBP", label: "GBP", symbol: "£" },
  { value: "RUB", label: "RUB", symbol: "₽" },
  { value: "CZK", label: "CZK", symbol: "Kč" },
  { value: "PLN", label: "PLN", symbol: "zł" },
  { value: "CHF", label: "CHF", symbol: "CHF" },
  { value: "HUF", label: "HUF", symbol: "Ft" },
  { value: "TRY", label: "TRY", symbol: "₺" },
  { value: "JPY", label: "JPY", symbol: "¥" },
];

function parseAccommodationPrice(value?: string) {
  const input = value?.trim() || "";
  if (!input) return { amount: "", currency: "EUR" as AccommodationCurrency };
  const escapeRegExp = (text: string) => text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const currency = accommodationCurrencies.find(({ symbol, value: code }) => {
    const escapedSymbol = escapeRegExp(symbol);
    return new RegExp(`^(?:${escapedSymbol}|${code})\\s*`, "i").test(input) ||
      new RegExp(`\\s*(?:${escapedSymbol}|${code})$`, "i").test(input);
  });
  if (!currency) return { amount: input, currency: "EUR" as AccommodationCurrency };
  const currencyPattern = `(?:${escapeRegExp(currency.symbol)}|${currency.value})`;
  const withoutPrefix = input.replace(
    new RegExp(`^${currencyPattern}\\s*`, "i"),
    "",
  );
  return {
    amount: withoutPrefix.replace(
      new RegExp(`\\s*${currencyPattern}$`, "i"),
      "",
    ).trim(),
    currency: currency.value,
  };
}

function formatAccommodationPriceValue(
  amount: string,
  currency: AccommodationCurrency,
) {
  const normalizedAmount = amount.trim();
  if (!normalizedAmount) return "";
  return `${accommodationCurrencies.find((item) => item.value === currency)?.symbol || "€"}${normalizedAmount}`;
}

function formatAccommodationPrice(value: string) {
  const parsed = parseAccommodationPrice(value);
  return formatAccommodationPriceValue(parsed.amount, parsed.currency);
}

function mapStyle() {
  return {
    version: 8 as const,
    sources: {
      openstreetmap: {
        type: "raster" as const,
        tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
        tileSize: 256,
        attribution:
          '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      },
    },
    layers: [
      {
        id: "openstreetmap",
        type: "raster" as const,
        source: "openstreetmap",
      },
    ],
  };
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
  const normalizedCity = city.trim().toLocaleLowerCase();
  return Object.entries(mapLocations).find(([name]) =>
    normalizedCity.includes(name.toLocaleLowerCase()),
  )?.[1];
}

type BrowserLocationState = {
  status: "idle" | "loading" | "ready" | "error";
  coordinates?: [number, number];
  accuracy?: number;
  message?: string;
};

function useBrowserLocation() {
  const [state, setState] = useState<BrowserLocationState>({ status: "idle" });

  const request = () => {
    if (typeof navigator === "undefined" || !navigator.geolocation) {
      setState({
        status: "error",
        message: "Этот браузер не поддерживает геолокацию.",
      });
      return;
    }
    setState({ status: "loading" });
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setState({
          status: "ready",
          coordinates: [position.coords.longitude, position.coords.latitude],
          accuracy: position.coords.accuracy,
        });
      },
      (error) => {
        const message =
          error.code === error.PERMISSION_DENIED
            ? "Разрешите доступ к геолокации в настройках браузера."
            : error.code === error.TIMEOUT
              ? "Не удалось определить местоположение вовремя."
              : "Не удалось определить местоположение.";
        setState({ status: "error", message });
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
    );
  };

  return { state, request };
}

function BrowserLocationButton({
  state,
  onRequest,
}: {
  state: BrowserLocationState;
  onRequest: () => void;
}) {
  const label =
    state.status === "ready"
      ? "Моё местоположение найдено"
      : state.message || "Найти меня на карте";
  return (
    <button
      type="button"
      className={`map-location-button${state.status === "error" ? " error" : ""}${state.status === "ready" ? " ready" : ""}`}
      aria-label={label}
      title={label}
      aria-busy={state.status === "loading"}
      disabled={state.status === "loading"}
      onClick={onRequest}
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="12" cy="12" r="7" />
        <circle cx="12" cy="12" r="2.5" />
      </svg>
    </button>
  );
}

function routeSegmentsFor(days: DraftDay[]) {
  return days.flatMap((day, dayIndex) => {
    const leg = day.roadLeg;
    if (!leg) return [];
    const coordinates = [leg.from, leg.to]
      .map(mapLocation)
      .filter((coordinate): coordinate is [number, number] =>
        Boolean(coordinate),
      );
    return coordinates.length ? [{ dayIndex, coordinates }] : [];
  });
}

function routeCoordinatesFor(days: DraftDay[]) {
  const coordinates: [number, number][] = [];
  const sameCoordinate = (first: [number, number], second: [number, number]) =>
    first[0] === second[0] && first[1] === second[1];
  routeSegmentsFor(days).forEach((segment) => {
    segment.coordinates.forEach((coordinate) => {
      if (!coordinates.at(-1) || !sameCoordinate(coordinates.at(-1)!, coordinate))
        coordinates.push(coordinate);
    });
  });
  return coordinates;
}

function routePointIndexFor(days: DraftDay[], dayIndex?: number) {
  if (dayIndex === undefined) return undefined;
  let pointIndex = -1;
  let previous: [number, number] | undefined;
  const sameCoordinate = (first: [number, number], second: [number, number]) =>
    first[0] === second[0] && first[1] === second[1];
  for (const segment of routeSegmentsFor(days)) {
    for (const coordinate of segment.coordinates) {
      if (!previous || !sameCoordinate(previous, coordinate)) pointIndex += 1;
      if (segment.dayIndex === dayIndex && coordinate === segment.coordinates[0])
        return pointIndex;
      previous = coordinate;
    }
  }
  return undefined;
}

function routeSegment(coordinates: [number, number][], day: number) {
  return coordinates.slice(day, day + 2);
}

const STATIC_MAP_WIDTH = 338;
const STATIC_MAP_HEIGHT = 420;

function staticMapWorldPoint(
  coordinate: [number, number],
  zoom: number,
): [number, number] {
  const scale = 256 * 2 ** zoom;
  const latitude = Math.max(-85.05112878, Math.min(85.05112878, coordinate[1]));
  const latitudeRadians = (latitude * Math.PI) / 180;
  return [
    ((coordinate[0] + 180) / 360) * scale,
    ((1 - Math.log(Math.tan(latitudeRadians) + 1 / Math.cos(latitudeRadians)) / Math.PI) / 2) * scale,
  ];
}

function staticMapZoom(coordinates: [number, number][]) {
  if (coordinates.length < 2) return 12;
  const zooms = Array.from({ length: 15 }, (_, index) => index + 2).reverse();
  return (
    zooms.find((zoom) => {
      const points = coordinates.map((coordinate) => staticMapWorldPoint(coordinate, zoom));
      const xSpan = Math.max(...points.map(([x]) => x)) - Math.min(...points.map(([x]) => x));
      const ySpan = Math.max(...points.map(([, y]) => y)) - Math.min(...points.map(([, y]) => y));
      return xSpan <= STATIC_MAP_WIDTH - 54 && ySpan <= STATIC_MAP_HEIGHT - 54;
    }) || 2
  );
}

function StaticTripMap({
  coordinates,
  routeCoordinates,
  activeDay,
  focusIndex,
  mapClassName = "map",
  markerClassName = "map-marker",
  connectWaypoints = true,
  onMarkerClick,
  userLocation,
}: {
  coordinates: [number, number][];
  routeCoordinates?: [number, number][];
  activeDay?: number;
  focusIndex?: number;
  mapClassName?: string;
  markerClassName?: string;
  connectWaypoints?: boolean;
  onMarkerClick?: (index: number) => void;
  userLocation?: [number, number];
}) {
  if (!coordinates.length && !userLocation) {
    return <div className={`${mapClassName} map-unavailable`}>Добавьте города или места, чтобы увидеть их на карте.</div>;
  }
  const focusCoordinates =
    focusIndex !== undefined && coordinates.length > 1
      ? coordinates.slice(focusIndex, focusIndex + 2)
      : coordinates;
  const mapFocusCoordinates = userLocation
    ? [...focusCoordinates, userLocation]
    : focusCoordinates;
  const lineCoordinates = routeCoordinates && routeCoordinates.length > 1
    ? routeCoordinates
    : coordinates;
  const zoom = staticMapZoom(mapFocusCoordinates);
  const points = coordinates.map((coordinate) => staticMapWorldPoint(coordinate, zoom));
  const linePoints = lineCoordinates.map((coordinate) => staticMapWorldPoint(coordinate, zoom));
  const focusPoints = mapFocusCoordinates.map((coordinate) => staticMapWorldPoint(coordinate, zoom));
  const minX = Math.min(...focusPoints.map(([x]) => x));
  const maxX = Math.max(...focusPoints.map(([x]) => x));
  const minY = Math.min(...focusPoints.map(([, y]) => y));
  const maxY = Math.max(...focusPoints.map(([, y]) => y));
  const centerX = (minX + maxX) / 2;
  const centerY = (minY + maxY) / 2;
  const positionedPoints = points.map(([x, y]) => [
    STATIC_MAP_WIDTH / 2 + x - centerX,
    STATIC_MAP_HEIGHT / 2 + y - centerY,
  ]);
  const positionedLinePoints = linePoints.map(([x, y]) => [
    STATIC_MAP_WIDTH / 2 + x - centerX,
    STATIC_MAP_HEIGHT / 2 + y - centerY,
  ]);
  const positionedUserPoint = userLocation
    ? (() => {
        const [x, y] = staticMapWorldPoint(userLocation, zoom);
        return [
          STATIC_MAP_WIDTH / 2 + x - centerX,
          STATIC_MAP_HEIGHT / 2 + y - centerY,
        ];
      })()
    : undefined;
  const tileCenterX = Math.floor(centerX / 256);
  const tileCenterY = Math.floor(centerY / 256);
  const tileCount = 5;
  const tileLimit = 2 ** zoom;
  const tiles = Array.from({ length: tileCount * tileCount }, (_, index) => {
    const offsetX = (index % tileCount) - 2;
    const offsetY = Math.floor(index / tileCount) - 2;
    const x = tileCenterX + offsetX;
    const y = tileCenterY + offsetY;
    if (y < 0 || y >= tileLimit) return null;
    const wrappedX = ((x % tileLimit) + tileLimit) % tileLimit;
    return {
      key: `${zoom}-${wrappedX}-${y}`,
      src: `https://tile.openstreetmap.org/${zoom}/${wrappedX}/${y}.png`,
      left: x * 256 - centerX,
      top: y * 256 - centerY,
    };
  }).filter((tile): tile is { key: string; src: string; left: number; top: number } => Boolean(tile));

  return (
    <div className={mapClassName}>
      <div className="static-map" aria-label="Карта путешествия">
        <div className="static-map-tiles" aria-hidden="true">
          {tiles.map((tile) => (
            <img
              key={tile.key}
              src={tile.src}
              alt=""
              draggable={false}
              style={{ left: `calc(50% + ${tile.left}px)`, top: `calc(50% + ${tile.top}px)` }}
            />
          ))}
        </div>
        <svg className="static-map-route" viewBox={`0 0 ${STATIC_MAP_WIDTH} ${STATIC_MAP_HEIGHT}`} preserveAspectRatio="none" aria-hidden="true">
          {connectWaypoints && positionedPoints.length > 1 && (
            <>
              <polyline className="halo" points={positionedLinePoints.map(([x, y]) => `${x},${y}`).join(" ")} />
              <polyline className="overview" points={positionedLinePoints.map(([x, y]) => `${x},${y}`).join(" ")} />
              <polyline className="waypoint-halo" points={positionedPoints.map(([x, y]) => `${x},${y}`).join(" ")} />
              <polyline className="waypoints" points={positionedPoints.map(([x, y]) => `${x},${y}`).join(" ")} />
            </>
          )}
          {connectWaypoints && focusIndex !== undefined && positionedPoints.slice(focusIndex, focusIndex + 2).length > 1 && (
            <>
              <polyline className="active-halo" points={positionedPoints.slice(focusIndex, focusIndex + 2).map(([x, y]) => `${x},${y}`).join(" ")} />
              <polyline className="active" points={positionedPoints.slice(focusIndex, focusIndex + 2).map(([x, y]) => `${x},${y}`).join(" ")} />
            </>
          )}
        </svg>
        {positionedPoints.map(([x, y], index) => (
          <button
            type="button"
            className={`${markerClassName} static-map-marker${index === activeDay ? " active" : ""}`}
            key={`${x}-${y}-${index}`}
            style={{ left: `${(x / STATIC_MAP_WIDTH) * 100}%`, top: `${(y / STATIC_MAP_HEIGHT) * 100}%` }}
            onClick={() => onMarkerClick?.(index)}
            aria-label={`Показать место ${index + 1} на карте`}
          >
            {index + 1}
          </button>
        ))}
        {positionedUserPoint && (
          <span
            className="map-user-location-static"
            style={{
              left: `${(positionedUserPoint[0] / STATIC_MAP_WIDTH) * 100}%`,
              top: `${(positionedUserPoint[1] / STATIC_MAP_HEIGHT) * 100}%`,
            }}
            title="Ваше местоположение"
            aria-label="Ваше местоположение"
          />
        )}
        <small className="static-map-attribution">© OpenStreetMap contributors</small>
      </div>
    </div>
  );
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

const attractionCatalog: StoredSight[] = Array.from(
  new globalThis.Map(
    [
      ...munichDayOneSights,
      ...veronaDayTwoSights,
      ...romeDayThreeSights,
      ...romeDayFourSights,
      ...romeDayFiveSights,
      ...sanMarinoDaySixSights,
      ...pisaDaySixSights,
      ...chioggiaDayEightSights,
      ...veniceDayNineSights,
      ...milanDayTenSights,
      ...ravensburgDayElevenSights,
      ...pragueDayTwelveSights,
      ...pragueDayThirteenSights,
      ...pragueDayFourteenSights,
    ].map((sight) => [sight.id, sight] as const),
  ).values(),
);

type WikipediaAttractionPage = {
  pageid?: number;
  title?: string;
  extract?: string;
  fullurl?: string;
  thumbnail?: { source?: string };
  coordinates?: { lat?: number; lon?: number }[];
};

async function fetchWikipediaAttractions(city: string, signal: AbortSignal) {
  const sources = [
    {
      endpoint: "https://ru.wikipedia.org/w/api.php",
      search: `${city} достопримечательности`,
    },
    {
      endpoint: "https://en.wikipedia.org/w/api.php",
      search: `${city} attractions`,
    },
  ];
  const results = await Promise.all(
    sources.map(async ({ endpoint, search }) => {
      const params = new URLSearchParams({
        action: "query",
        format: "json",
        formatversion: "2",
        origin: "*",
        generator: "search",
        gsrsearch: search,
        gsrnamespace: "0",
        gsrlimit: "12",
        prop: "pageimages|extracts|coordinates|info",
        piprop: "thumbnail|original",
        pithumbsize: "480",
        pilimit: "12",
        exintro: "1",
        explaintext: "1",
        exsentences: "2",
        inprop: "url",
      });
      const response = await fetch(`${endpoint}?${params}`, { signal });
      if (!response.ok) throw new Error("Wikipedia catalog request failed");
      const data = await response.json() as {
        query?: { pages?: WikipediaAttractionPage[] };
      };
      return data.query?.pages || [];
    }),
  );
  const normalizedCity = city.toLocaleLowerCase();
  const cityTokens = normalizedCity
    .split(/[\s,·-]+/)
    .filter((token) => token.length >= 3)
    .map((token) => token.slice(0, Math.max(3, token.length - 2)));
  const requireCityMention = /[А-Яа-яЁё]/.test(city);
  const unique = new globalThis.Map<string, StoredSight>();
  results.flat().forEach((page) => {
    const name = page.title?.trim();
    const photo = page.thumbnail?.source?.trim();
    if (!name || !photo || unique.has(name.toLocaleLowerCase())) return;
    if (
      requireCityMention &&
      !cityTokens.some((token) =>
        `${name} ${page.extract || ""}`.toLocaleLowerCase().includes(token),
      )
    ) return;
    const coordinate = page.coordinates?.[0];
    unique.set(name.toLocaleLowerCase(), {
      id: `wikipedia-${page.pageid || crypto.randomUUID()}`,
      name,
      city,
      group: "Достопримечательность",
      subcategory: "Каталог · Wikipedia",
      description: page.extract?.trim() || undefined,
      photo,
      lnglat:
        typeof coordinate?.lat === "number" && typeof coordinate.lon === "number"
          ? [coordinate.lon, coordinate.lat]
          : undefined,
    });
  });
  return [...unique.values()];
}

type GoogleSightCatalogPlace = {
  place_id?: unknown;
  name?: unknown;
  address?: unknown;
  category?: unknown;
  type?: unknown;
  rating?: unknown;
  rating_count?: unknown;
  description?: unknown;
  photo_url?: unknown;
  photo_names?: unknown;
  google_maps_url?: unknown;
  website?: unknown;
  latitude?: unknown;
  longitude?: unknown;
};

async function fetchGoogleSightCatalog(city: string, signal: AbortSignal) {
  const publishableKey = String(import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY || "");
  if (!publishableKey || !city.trim()) throw new Error("Google Places is not configured");
  const response = await fetch(googleFunctionUrl("restaurant-enrichment"), {
    method: "POST",
    signal,
    headers: {
      apikey: publishableKey,
      Authorization: `Bearer ${publishableKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      category: "sight",
      city: restaurantCitySearchName(city),
      limit: 60,
      languageCode: "ru",
    }),
  });
  if (!response.ok) throw new Error("Google sight catalog request failed");
  const data = await response.json().catch(() => null) as {
    sights?: GoogleSightCatalogPlace[];
  } | null;
  const places = Array.isArray(data?.sights) ? data.sights : [];
  return places.flatMap((place, index) => {
    const name = String(place.name || "").trim();
    if (!name) return [];
    const latitude = typeof place.latitude === "number" ? place.latitude : Number(place.latitude);
    const longitude = typeof place.longitude === "number" ? place.longitude : Number(place.longitude);
    const coordinate = Number.isFinite(latitude) && Number.isFinite(longitude)
      ? [longitude, latitude] as [number, number]
      : undefined;
    const rating = typeof place.rating === "number" ? place.rating : Number(place.rating);
    const reviews = typeof place.rating_count === "number"
      ? place.rating_count
      : place.rating_count === null || place.rating_count === undefined
        ? NaN
        : Number(place.rating_count);
    const photoNames = Array.isArray(place.photo_names)
      ? place.photo_names.filter((item): item is string => typeof item === "string")
      : [];
    const photo = String(place.photo_url || "").trim();
    return [{
      id: `google-sight-${String(place.place_id || index)}`,
      name,
      city,
      group: "Достопримечательность",
      subcategory: String(place.type || place.category || "Достопримечательность").trim() || "Достопримечательность",
      description: String(place.description || place.address || "").trim() || undefined,
      photo: photo || undefined,
      photoNames,
      lnglat: coordinate,
      googleRating: Number.isFinite(rating) ? rating : undefined,
      googleReviews: Number.isFinite(reviews) ? Math.trunc(reviews) : undefined,
      link: String(place.website || place.google_maps_url || "").trim() || undefined,
    } satisfies StoredSight];
  });
}

async function fetchSightCatalog(city: string, signal: AbortSignal) {
  try {
    const googleItems = await fetchGoogleSightCatalog(city, signal);
    if (googleItems.length) return googleItems;
  } catch (error) {
    if (signal.aborted) throw error;
  }
  return fetchWikipediaAttractions(city, signal);
}

type GoogleAccommodationCatalogPlace = {
  place_id?: unknown;
  name?: unknown;
  address?: unknown;
  category?: unknown;
  type?: unknown;
  rating?: unknown;
  rating_count?: unknown;
  description?: unknown;
  photo_url?: unknown;
  photo_names?: unknown;
  google_maps_url?: unknown;
};

async function fetchGoogleAccommodationCatalog(
  city: string,
  query: string,
  signal: AbortSignal,
) {
  const publishableKey = String(import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY || "");
  if (!publishableKey || !city.trim()) throw new Error("Google Places is not configured");
  const response = await fetch(googleFunctionUrl("restaurant-enrichment"), {
    method: "POST",
    signal,
    headers: {
      apikey: publishableKey,
      Authorization: `Bearer ${publishableKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      category: "accommodation",
      city: restaurantCitySearchName(city),
      query: query.trim(),
      limit: 60,
      languageCode: "ru",
    }),
  });
  if (!response.ok) throw new Error("Google accommodation catalog request failed");
  const data = await response.json().catch(() => null) as {
    accommodations?: GoogleAccommodationCatalogPlace[];
  } | null;
  const places = Array.isArray(data?.accommodations) ? data.accommodations : [];
  return places.flatMap((place, index) => {
    const name = String(place.name || "").trim();
    if (!name) return [];
    const rating = typeof place.rating === "number" ? place.rating : Number(place.rating);
    const reviews = typeof place.rating_count === "number" ? place.rating_count : Number(place.rating_count);
    const photoNames = Array.isArray(place.photo_names)
      ? place.photo_names.filter((item): item is string => typeof item === "string")
      : [];
    const photo = String(place.photo_url || "").trim();
    return [{
      id: `google-accommodation-${String(place.place_id || index)}`,
      name,
      city,
      address: String(place.address || "").trim() || undefined,
      description: String(place.description || place.address || "").trim() || undefined,
      link: String(place.google_maps_url || "").trim() || undefined,
      googleRating: Number.isFinite(rating) ? rating : undefined,
      googleReviews: Number.isFinite(reviews) ? Math.trunc(reviews) : undefined,
      photos: photo ? [photo] : [],
      photoNames,
      placeType: String(place.type || place.category || "Жильё").trim() || "Жильё",
    } satisfies ImportedAccommodation];
  });
}

type MapboxRestaurantFeature = {
  id?: string;
  text?: string;
  place_name?: string;
  center?: [number, number];
  geometry?: { coordinates?: [number, number] };
  properties?: { category?: string; cuisine?: string; image?: string; website?: string };
};

type PhotonRestaurantFeature = {
  properties?: {
    osm_type?: string;
    osm_id?: number;
    name?: string;
    city?: string;
    country?: string;
    street?: string;
    housenumber?: string;
    osm_value?: string;
    cuisine?: string;
    image?: string;
    website?: string;
  };
  geometry?: { coordinates?: [number, number] };
};

type RestaurantWikiPage = {
  title?: string;
  extract?: string;
  fullurl?: string;
  thumbnail?: { source?: string };
};

type CommonsRestaurantPage = {
  imageinfo?: { thumburl?: string; url?: string }[];
};

type GoogleRestaurantCatalogPlace = {
  place_id?: unknown;
  name?: unknown;
  address?: unknown;
  cuisine?: unknown;
  category?: unknown;
  type?: unknown;
  rating?: unknown;
  rating_count?: unknown;
  price_level?: unknown;
  description?: unknown;
  photo_url?: unknown;
  photo_name?: unknown;
  photo_names?: unknown;
  google_maps_url?: unknown;
  website?: unknown;
  latitude?: unknown;
  longitude?: unknown;
};

type GooglePetCatalogPlace = {
  place_id?: unknown;
  name?: unknown;
  address?: unknown;
  category?: unknown;
  type?: unknown;
  rating?: unknown;
  rating_count?: unknown;
  description?: unknown;
  photo_url?: unknown;
  photo_name?: unknown;
  photo_names?: unknown;
  google_maps_url?: unknown;
  phone?: unknown;
  website?: unknown;
  latitude?: unknown;
  longitude?: unknown;
  open_now?: unknown;
};

function restaurantPriceFromGoogle(value: unknown) {
  const level = typeof value === "number" && Number.isFinite(value)
    ? Math.trunc(value)
    : 0;
  return level >= 1 && level <= 4 ? "€".repeat(level) : "€€";
}

function restaurantCuisineFromCatalog(value: unknown, name: string, city: string) {
  const text = `${String(value || "")} ${name} ${city}`.toLocaleLowerCase();
  const matches: [RegExp, string][] = [
    [/pizza|pizzeria|italian|итал|рим|rome|trattoria|osteria/, "Итальянская"],
    [/sushi|ramen|japan|япон/, "Японская"],
    [/thai|тайск/, "Тайская"],
    [/indian|индий/, "Индийская"],
    [/mexican|мексик/, "Мексиканская"],
    [/chinese|китай/, "Китайская"],
    [/french|француз/, "Французская"],
    [/spanish|испан/, "Испанская"],
    [/czech|чеш|prague|прага/, "Чешская"],
    [/german|немец|munich|мюнх|salzburg|зальцбург/, "Европейская"],
    [/seafood|fish|морепродукт/, "Морепродукты"],
    [/steak|стейк/, "Стейкхаус"],
    [/coffee|cafe|кафе|кофейн/, "Кофейня и выпечка"],
  ];
  return matches.find(([pattern]) => pattern.test(text))?.[1] || "Европейская";
}

function googleFunctionUrl(functionName: string) {
  const baseUrl = String(import.meta.env.VITE_SUPABASE_URL || "").replace(/\/$/, "");
  return `${baseUrl}/functions/v1/${functionName}`;
}

async function fetchGoogleRestaurantPhotoUrls(
  photoNames: string[],
  signal: AbortSignal,
) {
  if (!photoNames.length) return new globalThis.Map<string, string>();
  const publishableKey = String(import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY || "");
  if (!publishableKey) return new globalThis.Map<string, string>();
  const response = await fetch(googleFunctionUrl("restaurant-enrichment"), {
    method: "POST",
    signal,
    headers: {
      apikey: publishableKey,
      Authorization: `Bearer ${publishableKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ photoNames: photoNames.slice(0, 24) }),
  });
  if (!response.ok) return new globalThis.Map<string, string>();
  const data = await response.json().catch(() => null) as {
    photos?: { photo_name?: unknown; photo_url?: unknown }[];
  } | null;
  return new globalThis.Map<string, string>(
    (data?.photos || [])
      .filter((photo) => typeof photo.photo_name === "string" && typeof photo.photo_url === "string")
      .map((photo) => [String(photo.photo_name), String(photo.photo_url)] as [string, string]),
  );
}

async function fetchGooglePetCatalog(
  city: string,
  type: PetPlace["type"],
  query: string,
  signal: AbortSignal,
): Promise<PetPlace[]> {
  const publishableKey = String(import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY || "");
  if (!publishableKey || !city.trim()) return [];
  const response = await fetch(googleFunctionUrl("restaurant-enrichment"), {
    method: "POST",
    signal,
    headers: {
      apikey: publishableKey,
      Authorization: `Bearer ${publishableKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      category: "pet",
      petType: type,
      city: restaurantCitySearchName(city),
      query: query.trim(),
      limit: 24,
      languageCode: "ru",
    }),
  });
  if (!response.ok) throw new Error("Google pet catalog request failed");
  const data = await response.json().catch(() => null) as { petPlaces?: GooglePetCatalogPlace[] } | null;
  const places = Array.isArray(data?.petPlaces) ? data.petPlaces : [];
  return places.flatMap((place, index) => {
    const name = String(place.name || "").trim();
    if (!name) return [];
    const placeId = String(place.place_id || `${city}-${type}-${index}`).trim();
    const photoNames = Array.isArray(place.photo_names)
      ? place.photo_names.filter((item): item is string => typeof item === "string")
      : [];
    const photoName = String(place.photo_name || photoNames[0] || "").trim();
    const latitude = typeof place.latitude === "number" ? place.latitude : Number(place.latitude);
    const longitude = typeof place.longitude === "number" ? place.longitude : Number(place.longitude);
    const mapsUrl = String(place.google_maps_url || "").trim() ||
      `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${name}, ${city}`)}`;
    const rating = typeof place.rating === "number" ? place.rating : Number(place.rating);
    const reviews = typeof place.rating_count === "number" ? place.rating_count : Number(place.rating_count);
    return [{
      id: `google-pet-${placeId}`,
      googlePlaceId: placeId,
      name,
      city,
      type,
      address: String(place.address || "").trim(),
      rating: Number.isFinite(rating) ? rating : undefined,
      reviewCount: Number.isFinite(reviews) ? Math.trunc(reviews) : undefined,
      photoUrl: String(place.photo_url || "").trim() || undefined,
      photoName: photoName || undefined,
      mapsUrl,
      latitude: Number.isFinite(latitude) ? latitude : undefined,
      longitude: Number.isFinite(longitude) ? longitude : undefined,
      note: String(place.description || "").trim() || undefined,
      phone: String(place.phone || "").trim() || undefined,
      openNow: typeof place.open_now === "boolean" ? place.open_now : undefined,
    } satisfies PetPlace];
  });
}

async function enrichPetCatalogPhotos(places: PetPlace[], signal: AbortSignal) {
  const photoNames = places
    .filter((place) => !place.photoUrl)
    .map((place) => place.photoName || "")
    .filter(Boolean);
  if (!photoNames.length) return places;
  const photoUrls = await fetchGoogleRestaurantPhotoUrls(photoNames, signal);
  return places.map((place) => {
    if (place.photoUrl || !place.photoName) return place;
    const photo = photoUrls.get(place.photoName);
    return photo ? { ...place, photoUrl: photo } : place;
  });
}

async function fetchGoogleRestaurantCatalog(
  city: string,
  query: string,
  signal: AbortSignal,
): Promise<ImportedRestaurant[]> {
  const publishableKey = String(import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY || "");
  if (!publishableKey || !city.trim()) return [];
  const response = await fetch(googleFunctionUrl("restaurant-enrichment"), {
    method: "POST",
    signal,
    headers: {
      apikey: publishableKey,
      Authorization: `Bearer ${publishableKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      category: "restaurant",
      city: restaurantCitySearchName(city),
      query: query.trim(),
      limit: 60,
      languageCode: "ru",
    }),
  });
  if (!response.ok) throw new Error("Google restaurant catalog request failed");
  const data = await response.json().catch(() => null) as {
    restaurants?: GoogleRestaurantCatalogPlace[];
  } | null;
  const places = Array.isArray(data?.restaurants) ? data.restaurants : [];
  return places.map((place, index) => {
    const name = String(place.name || "Ресторан").trim() || "Ресторан";
    const photo = String(place.photo_url || "");
    const latitude = typeof place.latitude === "number" ? place.latitude : Number(place.latitude);
    const longitude = typeof place.longitude === "number" ? place.longitude : Number(place.longitude);
    const coordinate = Number.isFinite(latitude) && Number.isFinite(longitude)
      ? [longitude, latitude] as [number, number]
      : undefined;
    const rating = typeof place.rating === "number" ? place.rating : Number(place.rating);
    const reviews = typeof place.rating_count === "number" ? place.rating_count : Number(place.rating_count);
    const photoNames = Array.isArray(place.photo_names)
      ? place.photo_names.filter((item): item is string => typeof item === "string")
      : [];
    return {
      id: `google-restaurant-${String(place.place_id || index)}`,
      name,
      city,
      lnglat: coordinate,
      status: "хочу",
      cuisine: restaurantCuisineFromCatalog(place.cuisine, name, city),
      note: String(place.description || place.address || "").trim() || undefined,
      link: String(place.google_maps_url || place.website || "").trim() || `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${name}, ${city}`)}`,
      price: restaurantPriceFromGoogle(place.price_level),
      googleRating: Number.isFinite(rating) ? rating : undefined,
      googleReviews: Number.isFinite(reviews) ? Math.trunc(reviews) : undefined,
      photos: photo ? [photo] : [],
      photoNames,
      placeType: String(place.type || "Ресторан").trim() || "Ресторан",
      categories: [String(place.category || "Ресторан").trim() || "Ресторан"],
    } satisfies ImportedRestaurant;
  });
}

async function enrichRestaurantCatalogPhotos(
  restaurants: ImportedRestaurant[],
  signal: AbortSignal,
) {
  const photoNames = restaurants
    .filter((restaurant) => !restaurant.photos?.length)
    .map((restaurant) => restaurant.photoNames?.[0] || "")
    .filter(Boolean);
  if (!photoNames.length) return restaurants;
  // Google Places accepts a limited number of photo names per request. Resolve
  // the whole catalog in small parallel batches so cards beyond the first
  // viewport also get their real photos, while the catalog itself remains
  // usable as soon as the restaurant list arrives.
  const photoUrls = new globalThis.Map<string, string>();
  const batchSize = 24;
  const batches: string[][] = [];
  for (let index = 0; index < photoNames.length; index += batchSize) {
    batches.push(photoNames.slice(index, index + batchSize));
  }
  const resolvedBatches = await Promise.all(
    batches.map((batch) => fetchGoogleRestaurantPhotoUrls(batch, signal)),
  );
  resolvedBatches.forEach((batch) => {
    batch.forEach((url, name) => photoUrls.set(name, url));
  });
  if (!photoUrls.size) return restaurants;
  return restaurants.map((restaurant) => {
    if (restaurant.photos?.length || !restaurant.photoNames?.[0]) return restaurant;
    const photo = photoUrls.get(restaurant.photoNames[0]);
    return photo ? { ...restaurant, photos: [photo] } : restaurant;
  });
}

async function enrichSightCatalogPhotos(
  sights: StoredSight[],
  signal: AbortSignal,
) {
  const photoNames = sights
    .filter((sight) => !sight.photo)
    .map((sight) => sight.photoNames?.[0] || "")
    .filter(Boolean);
  if (!photoNames.length) return sights;
  const photoUrls = new globalThis.Map<string, string>();
  const batchSize = 24;
  const batches: string[][] = [];
  for (let index = 0; index < photoNames.length; index += batchSize) {
    batches.push(photoNames.slice(index, index + batchSize));
  }
  const resolvedBatches = await Promise.all(
    batches.map((batch) => fetchGoogleRestaurantPhotoUrls(batch, signal)),
  );
  resolvedBatches.forEach((batch) => {
    batch.forEach((url, name) => photoUrls.set(name, url));
  });
  if (!photoUrls.size) return sights;
  return sights.map((sight) => {
    if (sight.photo || !sight.photoNames?.[0]) return sight;
    const photo = photoUrls.get(sight.photoNames[0]);
    return photo ? { ...sight, photo } : sight;
  });
}

async function enrichAccommodationCatalogPhotos(
  stays: ImportedAccommodation[],
  signal: AbortSignal,
) {
  const photoNames = stays
    .filter((stay) => !stay.photos?.length)
    .map((stay) => stay.photoNames?.[0] || "")
    .filter(Boolean);
  if (!photoNames.length) return stays;
  const photoUrls = new globalThis.Map<string, string>();
  const batchSize = 24;
  const batches: string[][] = [];
  for (let index = 0; index < photoNames.length; index += batchSize) {
    batches.push(photoNames.slice(index, index + batchSize));
  }
  const resolvedBatches = await Promise.all(
    batches.map((batch) => fetchGoogleRestaurantPhotoUrls(batch, signal)),
  );
  resolvedBatches.forEach((batch) => {
    batch.forEach((url, name) => photoUrls.set(name, url));
  });
  if (!photoUrls.size) return stays;
  return stays.map((stay) => {
    if (stay.photos?.length || !stay.photoNames?.[0]) return stay;
    const photo = photoUrls.get(stay.photoNames[0]);
    return photo ? { ...stay, photos: [photo] } : stay;
  });
}

function restaurantCitySearchName(city: string) {
  const name = city.toLocaleLowerCase();
  const aliases: [RegExp, string][] = [
    [/зальцбург|salzburg/, "Salzburg"],
    [/мюнхен|munich/, "Munich"],
    [/прага|prague/, "Prague"],
    [/рим|rome/, "Rome"],
    [/флоренц|флоренция|florence/, "Florence"],
    [/венеци|venice/, "Venice"],
    [/верона|verona/, "Verona"],
    [/милан|milan/, "Milan"],
    [/пиза|pisa/, "Pisa"],
  ];
  return aliases.find(([pattern]) => pattern.test(name))?.[1] || city.split(",")[0].trim();
}

function cleanRestaurantName(name: string) {
  return name.replace(/^(restaurant|ristorante|restaurace|cafe|café|bar)\s+/i, "").trim();
}

async function fetchCommonsRestaurantPhoto(name: string, city: string, signal: AbortSignal) {
  const params = new URLSearchParams({
    action: "query",
    format: "json",
    formatversion: "2",
    origin: "*",
    generator: "search",
    gsrsearch: `${cleanRestaurantName(name)} ${restaurantCitySearchName(city)}`,
    gsrnamespace: "6",
    gsrlimit: "3",
    prop: "imageinfo",
    iiprop: "url",
    iiurlwidth: "640",
  });
  const response = await fetch(`https://commons.wikimedia.org/w/api.php?${params}`, { signal });
  if (!response.ok) throw new Error("Commons photo request failed");
  const data = await response.json() as { query?: { pages?: CommonsRestaurantPage[] } };
  return data.query?.pages
    ?.map((page) => page.imageinfo?.[0]?.thumburl || page.imageinfo?.[0]?.url)
    .find(Boolean);
}

async function fetchRestaurantWikiInfo(
  name: string,
  city: string,
  signal: AbortSignal,
) {
  const searches = [
    { endpoint: "https://ru.wikipedia.org/w/api.php", search: `${cleanRestaurantName(name)} ${city}` },
    { endpoint: "https://en.wikipedia.org/w/api.php", search: `${cleanRestaurantName(name)} ${restaurantCitySearchName(city)}` },
  ];
  const pageResults = await Promise.all(
    searches.map(async ({ endpoint, search }) => {
      try {
        const params = new URLSearchParams({
          action: "query",
          format: "json",
          formatversion: "2",
          origin: "*",
          generator: "search",
          gsrsearch: search,
          gsrnamespace: "0",
          gsrlimit: "3",
          prop: "pageimages|extracts|info",
          piprop: "thumbnail|original",
          pithumbsize: "640",
          pilimit: "3",
          exintro: "1",
          explaintext: "1",
          exsentences: "2",
          inprop: "url",
        });
        const response = await fetch(`${endpoint}?${params}`, { signal });
        if (!response.ok) return [];
        const data = await response.json() as { query?: { pages?: RestaurantWikiPage[] } };
        return data.query?.pages || [];
      } catch {
        return [];
      }
    }),
  );
  const pages = pageResults.flat();
  const tokens = name
    .toLocaleLowerCase()
    .split(/[^\p{L}\p{N}]+/u)
    .filter((token) => token.length >= 4);
  const candidates = pages.flat().filter((page) => {
    const text = `${page.title || ""} ${page.extract || ""}`.toLocaleLowerCase();
    return tokens.length === 0 || tokens.some((token) => text.includes(token));
  });
  const restaurantWords = /restaurant|ristorante|restaurace|cafe|café|bar|keller|trattoria|pizzeria|gelateria|bistro|brasserie|gasthaus|brewery|pub|hotel|кафе|бар|ресторан|пиццер|траттор/i;
  const relevantPages = candidates.filter((item) =>
    restaurantWords.test(`${item.title || ""} ${item.extract || ""}`),
  );
  const page = relevantPages.find((item) => item.thumbnail?.source) || relevantPages[0];
  let commonsPhoto: string | undefined;
  try {
    commonsPhoto = await fetchCommonsRestaurantPhoto(name, city, signal);
  } catch {
    commonsPhoto = undefined;
  }
  if (!page && !commonsPhoto) return undefined;
  return {
    photo: page?.thumbnail?.source?.trim() || commonsPhoto,
    note: page.extract?.trim(),
    link: page.fullurl,
  };
}

async function fetchRestaurantCatalog(
  city: string,
  query: string,
  signal: AbortSignal,
): Promise<ImportedRestaurant[]> {
  if (!city.trim()) return [];
  try {
    const googleRestaurants = await fetchGoogleRestaurantCatalog(city, query, signal);
    if (googleRestaurants.length) return googleRestaurants;
  } catch {
    // Keep the catalog usable when Google Places is unavailable or rate-limited.
  }
  let features: MapboxRestaurantFeature[] = [];
  if (!features.length) {
    const base = mapLocation(city);
    let center = base;
    if (!center) {
      try {
        const geocodeParams = new URLSearchParams({ q: city, limit: "1" });
        const geocodeResponse = await fetch(`https://photon.komoot.io/api/?${geocodeParams}`, { signal });
        if (geocodeResponse.ok) {
          const geocodeData = await geocodeResponse.json() as { features?: PhotonRestaurantFeature[] };
          center = geocodeData.features?.[0]?.geometry?.coordinates;
        }
      } catch {
        // The catalog will show its empty state below.
      }
    }
    if (center) {
      const [longitude, latitude] = center;
      const bbox = [longitude - 0.22, latitude - 0.16, longitude + 0.22, latitude + 0.16].join(",");
      const photonParams = new URLSearchParams({
        q: query.trim() || "restaurant",
        limit: "24",
        bbox,
      });
      const photonResponse = await fetch(`https://photon.komoot.io/api/?${photonParams}`, { signal });
      if (photonResponse.ok) {
        const photonData = await photonResponse.json() as { features?: PhotonRestaurantFeature[] };
        features = (photonData.features || []).map((feature) => {
          const properties = feature.properties || {};
          const address = [
            properties.street && `${properties.street} ${properties.housenumber || ""}`.trim(),
            properties.city,
            properties.country,
          ].filter(Boolean).join(", ");
          return {
            id: `photon-${properties.osm_type || "place"}-${properties.osm_id || crypto.randomUUID()}`,
            text: properties.name,
            place_name: address || city,
            center: feature.geometry?.coordinates,
            properties: {
              category: properties.osm_value || "restaurant",
              cuisine: properties.cuisine,
              image: properties.image,
              website: properties.website,
            },
          } satisfies MapboxRestaurantFeature;
        });
      }
    }
  }
  const foodWords = /restaurant|cafe|bar|food|pizzeria|pizza|sushi|thai|burger|ramen|steak|indian|mexican|trattoria|osteria|ristorante|пицц|кафе|ресторан|бар|столов/i;
  const unique = new globalThis.Map<string, MapboxRestaurantFeature>();
  features.forEach((feature) => {
    const name = feature.text?.trim() || feature.place_name?.split(",")[0]?.trim();
    if (!name) return;
    const category = `${feature.properties?.category || ""} ${name}`;
    if (!foodWords.test(category) && !query.trim()) return;
    const key = name.toLocaleLowerCase();
    if (!unique.has(key)) unique.set(key, feature);
  });
  const selectedFeatures = [...unique.values()].slice(0, 60);
  const enriched = await Promise.all(
    selectedFeatures.map(async (feature) => {
      const name = feature.text?.trim() || feature.place_name?.split(",")[0]?.trim() || "Ресторан";
      let wiki: Awaited<ReturnType<typeof fetchRestaurantWikiInfo>>;
      try {
        wiki = await fetchRestaurantWikiInfo(name, city, signal);
      } catch {
        wiki = undefined;
      }
      const coordinate = feature.center || feature.geometry?.coordinates;
      const category = feature.properties?.cuisine?.split(",")[0]?.trim() || feature.properties?.category?.split(",")[0]?.trim();
      const placeName = feature.place_name?.trim() || city;
      return {
        id: `mapbox-restaurant-${feature.id || crypto.randomUUID()}`,
        name,
        city,
        lnglat: coordinate,
        status: "хочу",
        cuisine: category || "Европейская",
        note: wiki?.note || placeName,
        link: wiki?.link || feature.properties?.website || `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${name}, ${city}`)}`,
        price: "€€",
        photos: wiki?.photo || feature.properties?.image ? [wiki?.photo || feature.properties?.image || ""] : [],
        placeType: "ресторан",
        categories: category ? [category] : [],
      } satisfies ImportedRestaurant;
    }),
  );
  return enriched;
}

function catalogPhotoFor(sight: StoredSight, index: number) {
  return sight.photo || defaultSightPhotos[index % defaultSightPhotos.length];
}

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
  const browserLocation = useBrowserLocation();
  const location = city ? mapLocation(city) : undefined;
  const displayedRouteDays = routeDays;
  const routeCoordinates = routeCoordinatesFor(displayedRouteDays);
  const activeRoutePoint = routePointIndexFor(displayedRouteDays, activeDay);
  const fallbackCoordinates = routeCoordinates.length > 1
    ? routeCoordinates
    : location && places.length
      ? places.map(
          (_, index) =>
            [
              location[0] + (index - 2) * 0.012,
              location[1] + (index % 2 ? 1 : -1) * (index + 1) * 0.006,
            ] as [number, number],
        )
      : [];
  const previousActiveDay = useRef<number | undefined>(activeDay);
  const shouldFocusStaticMap =
    activeDay !== undefined &&
    previousActiveDay.current !== undefined &&
    activeDay !== previousActiveDay.current;
  useEffect(() => {
    previousActiveDay.current = activeDay;
  }, [activeDay]);
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
  const browserLocationKey = browserLocation.state.coordinates?.join(",") || "";

  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    if (!container.current || !token) return;
    let disposed = false;
    let map: Map | undefined;
    let resizeObserver: ResizeObserver | undefined;
    const userCoordinates = browserLocation.state.coordinates;

    void import("mapbox-gl").then(({ default: mapboxgl }) => {
      if (disposed || !container.current) return;
      if (token) mapboxgl.accessToken = token;
      map = new mapboxgl.Map({
        container: container.current,
        style: mapStyle(),
        center: userCoordinates ?? routeCoordinates[0] ?? location ?? mapLocations["Москва"],
        zoom: userCoordinates ? 13 : routeCoordinates.length ? 5 : location ? 12 : 3,
        attributionControl: true,
      });
      mapRef.current = map;
      if (userCoordinates) {
        const element = document.createElement("span");
        element.className = "map-user-location-marker";
        element.title = "Ваше местоположение";
        new mapboxgl.Marker({ element }).setLngLat(userCoordinates).addTo(map);
      }
      resizeObserver = new ResizeObserver(() => map?.resize());
      resizeObserver.observe(container.current);
      map.addControl(
        new mapboxgl.NavigationControl({ showCompass: false }),
        "top-right",
      );

      if (routeCoordinates.length > 1) {
        markerElements.current = [];
        const markerOccurrences = new globalThis.Map<string, number>();
        routeCoordinates.forEach((coordinate, index) => {
          const element = document.createElement("span");
          element.className =
            index === activeRoutePoint ? "map-marker active" : "map-marker";
          element.textContent = String(index + 1);
          markerElements.current.push(element);
          const coordinateKey = coordinate.join(",");
          const occurrence = markerOccurrences.get(coordinateKey) || 0;
          markerOccurrences.set(coordinateKey, occurrence + 1);
          const offset: [number, number] | undefined = occurrence
            ? [18 * (occurrence % 2 ? 1 : -1), -18 * occurrence]
            : undefined;
          new mapboxgl.Marker({ element, offset })
            .setLngLat(coordinate)
            .addTo(map!);
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
          const activeSegment =
            activeRoutePoint === undefined
              ? []
              : routeSegment(routeCoordinates, activeRoutePoint);
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
          if (userCoordinates) {
            map!.flyTo({ center: userCoordinates, zoom: 14, duration: 700, essential: true });
          }
        });
      } else if (location && places.length) {
        const coordinates = fallbackCoordinates;
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
          if (userCoordinates) {
            map!.flyTo({ center: userCoordinates, zoom: 14, duration: 700, essential: true });
          }
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
  }, [city, locationKey, placesKey, routeKey, browserLocationKey]);

  useEffect(() => {
    const routePoint = routePointIndexFor(displayedRouteDays, activeDay);
    const coordinate =
      routePoint === undefined
        ? undefined
        : routeCoordinatesFor(displayedRouteDays)[routePoint];
    if (!mapRef.current) return;
    markerElements.current.forEach((element, index) =>
      element.classList.toggle("active", index === routePoint),
    );
    const map = mapRef.current;
    const activeSegment =
      routePoint === undefined
        ? []
        : routeSegment(routeCoordinatesFor(displayedRouteDays), routePoint);
    const source = map.getSource("active-route") as
      { setData: (data: object) => void } | undefined;
    if (source && activeSegment.length > 1) {
      source.setData({
        type: "Feature",
        properties: {},
        geometry: { type: "LineString", coordinates: activeSegment },
      });
    } else if (source) {
      if (map.getLayer("active-route")) map.removeLayer("active-route");
      map.removeSource("active-route");
    } else if (activeSegment.length > 1 && map.isStyleLoaded()) {
      map.addSource("active-route", {
        type: "geojson",
        data: {
          type: "Feature",
          properties: {},
          geometry: { type: "LineString", coordinates: activeSegment },
        },
      });
      map.addLayer({
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
    if (!coordinate) return;
    map.flyTo({
      center: coordinate,
      zoom: 8,
      duration: 900,
      essential: true,
    });
  }, [activeDay, routeKey]);

  if (!import.meta.env.VITE_MAPBOX_ACCESS_TOKEN)
    return (
      <div className="map-location-wrap">
        <StaticTripMap
          coordinates={fallbackCoordinates}
          activeDay={activeRoutePoint}
          focusIndex={shouldFocusStaticMap ? activeRoutePoint : undefined}
          userLocation={browserLocation.state.coordinates}
        />
        <BrowserLocationButton state={browserLocation.state} onRequest={browserLocation.request} />
      </div>
    );
  return (
    <div className="map-location-wrap">
      <div
        ref={container}
        className="map"
        aria-label={city ? `Карта ${city}` : "Карта путешествия"}
      />
      <BrowserLocationButton state={browserLocation.state} onRequest={browserLocation.request} />
    </div>
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
  name,
  className,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  name?: string;
  className?: string;
}) {
  const parsedSelected = value ? new Date(`${value}T12:00:00`) : null;
  // Existing trips may contain a display-only date range. Treat an invalid
  // value as empty so opening an editor never crashes the whole page.
  const selected =
    parsedSelected && !Number.isNaN(parsedSelected.getTime())
      ? parsedSelected
      : null;
  const [open, setOpen] = useState(false);
  const pickerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const [popoverStyle, setPopoverStyle] = useState<{
    position: "fixed";
    left: number;
    right: "auto";
    top: number;
    bottom: "auto";
    width: number;
    transform?: string;
  }>();
  const [month, setMonth] = useState(() =>
    selected
      ? new Date(selected.getFullYear(), selected.getMonth(), 1)
      : new Date(),
  );
  useEffect(() => {
    if (!open) return;
    const closeOnOutsideClick = (event: PointerEvent) => {
      if (!pickerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", closeOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeOnOutsideClick);
  }, [open]);
  useEffect(() => {
    if (!open) return;
    const updatePopoverPosition = () => {
      const trigger = triggerRef.current;
      if (!trigger) return;
      const rect = trigger.getBoundingClientRect();
      const width = Math.min(306, window.innerWidth - 24);
      const left = Math.max(12, Math.min(rect.left, window.innerWidth - width - 12));
      const showAbove = rect.top > 438;
      setPopoverStyle({
        position: "fixed",
        left,
        right: "auto",
        top: showAbove ? rect.top - 8 : rect.bottom + 8,
        bottom: "auto",
        width,
        ...(showAbove ? { transform: "translateY(-100%)" } : {}),
      });
    };
    updatePopoverPosition();
    window.addEventListener("resize", updatePopoverPosition);
    window.addEventListener("scroll", updatePopoverPosition, true);
    return () => {
      window.removeEventListener("resize", updatePopoverPosition);
      window.removeEventListener("scroll", updatePopoverPosition, true);
    };
  }, [open]);
  useEffect(() => {
    if (selected) setMonth(new Date(selected.getFullYear(), selected.getMonth(), 1));
  }, [value]);
  const weekdayLabels = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"];
  const monthStart = (month.getDay() + 6) % 7;
  const formatted = selected
    ? new Intl.DateTimeFormat("ru-RU", {
        day: "numeric",
        month: "long",
        year: "numeric",
      }).format(selected)
    : "Выберите дату";
  const today = new Date();
  const isSameDay = (first: Date | null, second: Date) =>
    Boolean(
      first &&
        first.getFullYear() === second.getFullYear() &&
        first.getMonth() === second.getMonth() &&
        first.getDate() === second.getDate(),
    );
  const chooseDate = (date: Date) => {
    const next = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
    onChange(next);
    setOpen(false);
  };
  return (
    <label className={`date-field${className ? ` ${className}` : ""}`}>
      {label}
      {name && <input type="hidden" name={name} value={value} />}
      <div className="date-picker" ref={pickerRef}>
        <button
          type="button"
          className="date-trigger"
          ref={triggerRef}
          onClick={() => setOpen(!open)}
        >
          <span className={value ? "" : "placeholder"}>{formatted}</span>
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <rect x="4" y="5" width="16" height="15" rx="2" />
            <path d="M8 3v4m8-4v4M4 10h16" />
          </svg>
        </button>
        {open && (
          <div className="calendar-popover" style={popoverStyle}>
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
              {Array.from({ length: 42 }, (_, index) => {
                const date = new Date(
                  month.getFullYear(),
                  month.getMonth(),
                  index - monthStart + 1,
                );
                const isCurrentMonth = date.getMonth() === month.getMonth();
                return (
                  <button
                    type="button"
                    className={`${isCurrentMonth ? "" : "outside-month "}${isSameDay(selected, date) ? "selected " : ""}${isSameDay(today, date) ? "today" : ""}`}
                    onClick={() => chooseDate(date)}
                    key={date.toISOString()}
                  >
                    {date.getDate()}
                  </button>
                );
              })}
            </div>
            <div className="calendar-footer">
              <button type="button" onClick={() => { onChange(""); setOpen(false); }}>
                Очистить
              </button>
              <button type="button" onClick={() => chooseDate(today)}>
                Сегодня
              </button>
            </div>
          </div>
        )}
      </div>
    </label>
  );
}

function AccommodationCityPicker({
  value,
  onChange,
  cities = accommodationCities,
  placeholder = "Начните вводить город",
  allOption,
  className = "",
}: {
  value: string;
  onChange: (value: string) => void;
  cities?: string[];
  placeholder?: string;
  allOption?: string;
  className?: string;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState(allOption && value === allOption ? "" : value);
  const [remoteOptions, setRemoteOptions] = useState<string[]>([]);
  const [searching, setSearching] = useState(false);
  const pickerRef = useRef<HTMLDivElement>(null);
  useEffect(
    () => setQuery(allOption && value === allOption ? "" : value),
    [allOption, value],
  );
  useEffect(() => {
    if (!open) return;
    const closeOnOutsideClick = (event: PointerEvent) => {
      if (!pickerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", closeOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeOnOutsideClick);
  }, [open]);
  useEffect(() => {
    const search = query.trim();
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    if (!open || search.length < 2) {
      setRemoteOptions([]);
      setSearching(false);
      return;
    }
    const controller = new AbortController();
    const timeout = window.setTimeout(async () => {
      setSearching(true);
      try {
        let remoteCities: string[] = [];
        if (token) {
          const params = new URLSearchParams({
            access_token: token,
            autocomplete: "true",
            language: "ru,en",
            limit: "8",
            types: "place,locality",
          });
          const response = await fetch(
            `https://api.mapbox.com/geocoding/v5/mapbox.places/${encodeURIComponent(search)}.json?${params}`,
            { signal: controller.signal },
          );
          if (response.ok) {
            const data = await response.json() as {
              features?: { place_name?: string; text?: string }[];
            };
            remoteCities = (data.features || [])
              .map((feature) => feature.place_name || feature.text || "")
              .filter(Boolean);
          }
        }
        if (!remoteCities.length) {
          const photonParams = new URLSearchParams({
            q: search,
            limit: "8",
          });
          const response = await fetch(
            `https://photon.komoot.io/api/?${photonParams}`,
            { signal: controller.signal },
          );
          if (!response.ok) throw new Error("City search failed");
          const data = await response.json() as {
            features?: {
              properties?: {
                name?: string;
                country?: string;
                type?: string;
              };
            }[];
          };
          remoteCities = (data.features || [])
            .filter((feature) =>
              ["city", "town", "village", "municipality"].includes(feature.properties?.type || ""),
            )
            .map((feature) => {
              const name = feature.properties?.name?.trim() || "";
              const country = feature.properties?.country?.trim() || "";
              return name && country && !name.includes(country)
                ? `${name}, ${country}`
                : name;
            })
            .filter(Boolean);
        }
        setRemoteOptions(remoteCities);
      } catch {
        if (!controller.signal.aborted) setRemoteOptions([]);
      } finally {
        if (!controller.signal.aborted) setSearching(false);
      }
    }, 260);
    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [open, query]);
  const normalizedQuery = query.trim().toLocaleLowerCase();
  const localOptions = Array.from(new Set(cities.filter(Boolean)))
    .filter((city) => !normalizedQuery || city.toLocaleLowerCase().includes(normalizedQuery));
  const visibleLocalOptions = normalizedQuery ? localOptions : localOptions.slice(0, 8);
  const filteredRemoteOptions = normalizedQuery
    ? remoteOptions.filter((city) => {
      const cityName = city.split(",")[0]?.trim().toLocaleLowerCase() || city.toLocaleLowerCase();
      return cityName.includes(normalizedQuery) || normalizedQuery.includes(cityName);
    })
    : [];
  const hasExactLocalMatch = localOptions.some(
    (city) => city.trim().toLocaleLowerCase() === normalizedQuery,
  );
  const options = Array.from(new Set([
    ...(allOption ? [allOption] : []),
    ...(hasExactLocalMatch ? [] : filteredRemoteOptions),
    ...visibleLocalOptions,
  ]))
    .sort((first, second) => first.localeCompare(second, "ru"))
    .slice(0, 12);
  const chooseCity = (city: string) => {
    setQuery(allOption && city === allOption ? "" : city);
    onChange(city);
    setOpen(false);
  };
  return (
    <div className={`accommodation-city-picker ${className}`.trim()} ref={pickerRef}>
      <input
        // Chrome treats a generic `city` field as an address form field and
        // may show saved address profiles even when autocomplete is disabled.
        // Use a travel-specific name and the password sentinel to keep this
        // picker limited to the app's own city suggestions.
        name="travel-city"
        value={!open && allOption && value === allOption ? allOption : query}
        onChange={(event) => {
          setQuery(event.target.value);
          onChange(event.target.value);
          setOpen(true);
        }}
        onFocus={() => {
          if (allOption && value === allOption) setQuery("");
          setOpen(true);
        }}
        onClick={() => setOpen(true)}
        onKeyDown={(event) => {
          if (event.key === "Escape") setOpen(false);
          if (event.key === "Enter" && options[0]) {
            event.preventDefault();
            chooseCity(options[0]);
          }
        }}
        placeholder={placeholder}
        autoComplete="new-password"
        role="combobox"
        aria-expanded={open}
        aria-controls="accommodation-city-options"
      />
      <button
        type="button"
        className="accommodation-city-chevron"
        aria-label={open ? "Скрыть список городов" : "Открыть список городов"}
        onMouseDown={(event) => event.preventDefault()}
        onClick={() => setOpen((current) => !current)}
      >
        ⌄
      </button>
      {open && (
        <div
          id="accommodation-city-options"
          className="accommodation-city-suggestions"
          role="listbox"
        >
          {options.length ? options.map((city) => (
            <button
              type="button"
              role="option"
              aria-selected={value === city}
              className={value === city ? "selected" : ""}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => chooseCity(city)}
              key={city}
            >
              {city}
            </button>
          )) : searching ? (
            <p>Ищем города…</p>
          ) : (
            <p>Город будет сохранён как введён</p>
          )}
        </div>
      )}
    </div>
  );
}

function AccountSettingIcon({
  name,
}: {
  name: "language" | "theme" | "password" | "photo" | "delete";
}) {
  const paths = {
    language: (
      <>
        <circle cx="12" cy="12" r="9" />
        <path d="M3 12h18M12 3c3 3.2 3 14.8 0 18M12 3c-3 3.2-3 14.8 0 18" />
      </>
    ),
    theme: <path d="M20 15.4A8 8 0 0 1 8.6 4a8.5 8.5 0 1 0 11.4 11.4Z" />,
    password: (
      <>
        <rect x="5" y="10" width="14" height="10" rx="2" />
        <path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v2" />
      </>
    ),
    photo: (
      <>
        <rect x="3" y="4" width="18" height="16" rx="2" />
        <circle cx="8.5" cy="9" r="1.5" />
        <path d="m4.5 17 4-4 3 3 2.5-2.5 5.5 5.5" />
      </>
    ),
    delete: (
      <>
        <path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5" />
      </>
    ),
  };
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      {paths[name]}
    </svg>
  );
}

function Sidebar({
  view,
  go,
  open,
  close,
  profileName,
  tripCount,
  cityCount,
  darkTheme = false,
  onDarkThemeChange,
}: {
  view: View;
  go: (view: View) => void;
  open: boolean;
  close: () => void;
  profileName: string;
  tripCount: number;
  cityCount: number;
  darkTheme?: boolean;
  onDarkThemeChange?: (value: boolean) => void;
}) {
  const [settings, setSettings] = useState(false);
  const [panel, setPanel] = useState<"language" | "photo" | "password" | null>(null);
  const [interfaceLanguage, setInterfaceLanguage] = useState<"ru" | "en" | "es" | "de">("ru");
  const interfaceLanguages = [
    ["ru", "Русский", "RU"],
    ["en", "English", "EN"],
    ["es", "Español", "ES"],
    ["de", "Deutsch", "DE"],
  ] as const;
  const profileHandle = profileName.includes("@")
    ? profileName.split("@")[0]
    : profileName;
  useEffect(() => {
    let active = true;
    void supabase.auth.getSession().then(async ({ data }) => {
      if (!data.session?.user) return;
      const { data: preference, error } = await supabase
        .from("user_data")
        .select("value")
        .eq("user_id", data.session.user.id)
        .eq("key", "interface-language")
        .maybeSingle();
      if (error) {
        console.error("Could not load interface language.", error);
        return;
      }
      const value = preference?.value;
      if (
        active &&
        (value === "ru" || value === "en" || value === "es" || value === "de")
      ) {
        setInterfaceLanguage(value);
        document.documentElement.lang = value;
      }
    });
    return () => {
      active = false;
    };
  }, []);
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
        </nav>
        <div className="account-wrap">
          {settings && (
            <>
              <button
                className="settings-scrim"
                type="button"
                onClick={closeSettings}
                aria-label="Закрыть личный кабинет"
              />
              <section
                className={`settings-popover ${darkTheme ? "dark" : ""}`}
                aria-label="Личный кабинет"
              >
                <div className="settings-handle" />
                <header className="settings-profile">
                  <div className="settings-avatar">
                    {profileHandle.slice(0, 1).toUpperCase()}
                  </div>
                  <div>
                    <h2>{profileHandle}</h2>
                    <p>Личный кабинет · Ramingo</p>
                  </div>
                  <button
                    className="settings-close"
                    type="button"
                    onClick={closeSettings}
                    aria-label="Закрыть настройки"
                  >
                    ×
                  </button>
                </header>

                <div className="settings-stats">
                  <div>
                    <b>{tripCount}</b>
                    <span>поездки</span>
                  </div>
                  <div>
                    <b>{cityCount}</b>
                    <span>городов</span>
                  </div>
                  <div>
                    <b>—</b>
                    <span>км</span>
                  </div>
                </div>

                <p className="settings-label">Настройки аккаунта</p>
                <div className="settings-list">
                  <button
                    className="settings-row"
                    type="button"
                    onClick={() => setPanel(panel === "language" ? null : "language")}
                    aria-expanded={panel === "language"}
                  >
                    <span className="settings-icon"><AccountSettingIcon name="language" /></span>
                    <b>Языки</b>
                    <small>
                      {interfaceLanguages.find(([value]) => value === interfaceLanguage)?.[1]}
                    </small>
                    <i>›</i>
                  </button>
                  {panel === "language" && (
                    <div className="settings-language-panel">
                      {interfaceLanguages.map(([value, _label, code]) => (
                        <button
                          className={interfaceLanguage === value ? "selected" : ""}
                          type="button"
                          onClick={() => {
                            setInterfaceLanguage(value);
                            document.documentElement.lang = value;
                            void saveUserData("interface-language", value);
                          }}
                          key={value}
                        >
                          {code}
                        </button>
                      ))}
                    </div>
                  )}
                  <button
                    className="settings-row"
                    type="button"
                    onClick={() => onDarkThemeChange?.(!darkTheme)}
                    aria-pressed={darkTheme}
                  >
                    <span className="settings-icon"><AccountSettingIcon name="theme" /></span>
                    <b>Тёмная тема</b>
                    <span className={`settings-toggle ${darkTheme ? "on" : ""}`}>
                      <i />
                    </span>
                  </button>
                  <button
                    className="settings-row"
                    type="button"
                    onClick={() =>
                      setPanel(panel === "password" ? null : "password")
                    }
                  >
                    <span className="settings-icon"><AccountSettingIcon name="password" /></span>
                    <b>Сменить пароль</b>
                    <i>›</i>
                  </button>
                  {panel === "password" && (
                    <div className="settings-panel">
                      <PasswordField placeholder="Текущий пароль" autoComplete="current-password" />
                      <PasswordField placeholder="Новый пароль" autoComplete="new-password" />
                      <button className="accent" type="button">Обновить пароль</button>
                    </div>
                  )}
                  <button
                    className="settings-row"
                    type="button"
                    onClick={() => setPanel(panel === "photo" ? null : "photo")}
                  >
                    <span className="settings-icon"><AccountSettingIcon name="photo" /></span>
                    <b>Сменить фото</b>
                    <i>›</i>
                  </button>
                  {panel === "photo" && (
                    <div className="settings-panel">
                      <div className="mini-upload">
                        ↑<small>Перетащите фото</small>
                      </div>
                      <button className="accent" type="button">Сохранить фото</button>
                    </div>
                  )}
                  <button
                    className="settings-row danger"
                    type="button"
                    onClick={() => {
                      closeSettings();
                      close();
                      go("delete-account");
                    }}
                  >
                    <span className="settings-icon"><AccountSettingIcon name="delete" /></span>
                    <b>Удалить аккаунт</b>
                    <i>›</i>
                  </button>
                </div>

                <p className="settings-version">Версия приложения · 0.2.18-beta</p>
                <button
                  className="settings-logout"
                  type="button"
                  onClick={async () => {
                    await supabase.auth.signOut();
                    setSettings(false);
                    close();
                    go("auth");
                  }}
                >
                  Выйти из аккаунта
                </button>
              </section>
            </>
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
  onDeleteTrip,
  onLeaveTrip,
}: {
  go: (view: View) => void;
  profileName: string;
  drafts: TripSummary[];
  onOpenTrip: (trip: TripSummary) => void;
  onUpdateTrip: (trip: TripSummary) => void;
  onDeleteTrip: (trip: TripSummary) => Promise<void>;
  onLeaveTrip: (trip: TripSummary) => Promise<void>;
}) {
  const [filter, setFilter] = useState("all");
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
                  onClick={() => setEditingTrip(trip)}
                  aria-label={`Настройки: ${trip.title}`}
                  aria-haspopup="dialog"
                >
                  <i />
                  <i />
                  <i />
                </button>
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
          onDeleteTrip={onDeleteTrip}
          onLeaveTrip={onLeaveTrip}
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
  onDelete,
}: {
  roadLeg?: RoadLeg;
  onChange: (roadLeg: RoadLeg) => void;
  onSave: (roadLeg: RoadLeg) => void;
  onCancel: () => void;
  onDelete: () => void;
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
        <button type="button" className="road-leg-delete" onClick={onDelete}>
          Удалить день
        </button>
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
  dragDisabled,
  selected,
  dragging,
  dropTarget,
  onSelect,
  onEdit,
  onChange,
  onSave,
  onCancel,
  onDelete,
  onDragStart,
  onDragOver,
  onDrop,
  onDragEnd,
}: {
  day: DraftDay;
  index: number;
  editing: boolean;
  dragDisabled: boolean;
  selected: boolean;
  dragging: boolean;
  dropTarget: boolean;
  onSelect: () => void;
  onEdit: () => void;
  onChange: (roadLeg: RoadLeg) => void;
  onSave: (roadLeg: RoadLeg) => void;
  onCancel: () => void;
  onDelete: () => void;
  onDragStart: () => void;
  onDragOver: () => void;
  onDrop: () => void;
  onDragEnd: () => void;
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
      className={`draft-route-card${selected ? " selected" : ""}${dragging ? " dragging" : ""}${dropTarget ? " drop-target" : ""}${dragDisabled ? " drag-disabled" : ""}`}
      draggable={!dragDisabled}
      onDragStart={(event) => {
        if (dragDisabled) return;
        event.dataTransfer.effectAllowed = "move";
        onDragStart();
      }}
      onDragOver={(event) => {
        if (dragDisabled) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = "move";
        onDragOver();
      }}
      onDrop={(event) => {
        if (dragDisabled) return;
        event.preventDefault();
        onDrop();
      }}
      onDragEnd={onDragEnd}
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
          onDelete={onDelete}
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
  onDeleteDraftDay,
  onReorderDraftDays,
}: {
  isDraft?: boolean;
  draftDays?: DraftDay[];
  editingRoadDay?: number | null;
  onEditingRoadDayChange?: (day: number | null) => void;
  onAddDraftDay?: () => void;
  onUpdateDraftDay?: (day: number, changes: Partial<DraftDay>) => void;
  onDeleteDraftDay?: (day: number) => void;
  onReorderDraftDays?: (from: number, to: number) => void;
}) {
  const [day, setDay] = useState(0);
  const [selectedRouteDay, setSelectedRouteDay] = useState(0);
  const [draggedDay, setDraggedDay] = useState<number | null>(null);
  const [dropTargetDay, setDropTargetDay] = useState<number | null>(null);
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
              dragDisabled={editingRoadDay !== null}
              selected={selectedRouteDay === index}
              dragging={draggedDay === index}
              dropTarget={dropTargetDay === index && draggedDay !== index}
              onSelect={() => setSelectedRouteDay(index)}
              onEdit={() => {
                setDraggedDay(null);
                setDropTargetDay(null);
                onEditingRoadDayChange?.(index);
              }}
              onChange={(roadLeg) => onUpdateDraftDay?.(index, { roadLeg })}
              onSave={(roadLeg) => {
                onUpdateDraftDay?.(index, { roadLeg });
                onEditingRoadDayChange?.(null);
              }}
              onCancel={() => onEditingRoadDayChange?.(null)}
              onDelete={() => onDeleteDraftDay?.(index)}
              onDragStart={() => {
                setDraggedDay(index);
                setDropTargetDay(index);
              }}
              onDragOver={() => setDropTargetDay(index)}
              onDrop={() => {
                if (draggedDay !== null && draggedDay !== index)
                  onReorderDraftDays?.(draggedDay, index);
                setDraggedDay(null);
                setDropTargetDay(null);
              }}
              onDragEnd={() => {
                setDraggedDay(null);
                setDropTargetDay(null);
              }}
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

function RestaurantMap({
  places,
  activeRestaurantId,
  onSelect,
  onCoordinatesResolved,
}: {
  places: ImportedRestaurant[];
  activeRestaurantId?: string;
  onSelect: (restaurantId: string) => void;
  onCoordinatesResolved?: (updates: Record<string, [number, number]>) => void;
}) {
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const markerElements = useRef(new globalThis.Map<string, HTMLSpanElement>());
  const browserLocation = useBrowserLocation();
  const [resolvedCoordinates, setResolvedCoordinates] = useState<Record<string, [number, number]>>({});
  const isCoordinate = (value: ImportedRestaurant["lnglat"]): value is [number, number] =>
    Array.isArray(value) && value.length >= 2 && Number.isFinite(value[0]) && Number.isFinite(value[1]) &&
    Math.abs(value[0]) <= 180 && Math.abs(value[1]) <= 90;
  const coordinateInputKey = places
    .map((place) => `${place.id}:${isCoordinate(place.lnglat) ? place.lnglat.join(",") : ""}`)
    .join(";");
  useEffect(() => {
    const missing = places.filter((place) => !isCoordinate(place.lnglat) && !resolvedCoordinates[place.id]);
    if (!missing.length) return;
    const controller = new AbortController();
    const resolve = async () => {
      const updates: Record<string, [number, number]> = {};
      let cursor = 0;
      const worker = async () => {
        while (!controller.signal.aborted) {
          const index = cursor++;
          if (index >= missing.length) return;
          const place = missing[index];
          try {
            const base = mapLocation(place.city);
            const params = new URLSearchParams({
              q: `${place.name}, ${place.city}`,
              limit: "5",
            });
            if (base) {
              params.set("bbox", [base[0] - 0.12, base[1] - 0.1, base[0] + 0.12, base[1] + 0.1].join(","));
            }
            const response = await fetch(`https://photon.komoot.io/api/?${params}`, { signal: controller.signal });
            if (!response.ok) continue;
            const data = await response.json().catch(() => null) as {
              features?: { properties?: { name?: unknown; city?: unknown }; geometry?: { coordinates?: unknown } }[];
            } | null;
            const nameTokens = place.name.toLocaleLowerCase().split(/[^\p{L}\p{N}]+/u).filter((token) => token.length >= 4);
            const cityToken = place.city.split(",")[0].trim().toLocaleLowerCase();
            const candidates = (data?.features || []).flatMap((feature) => {
              const coordinates = feature.geometry?.coordinates;
              if (!Array.isArray(coordinates) || coordinates.length < 2) return [];
              const longitude = Number(coordinates[0]);
              const latitude = Number(coordinates[1]);
              if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) return [];
              const featureName = String(feature.properties?.name || "").toLocaleLowerCase();
              const featureCity = String(feature.properties?.city || "").toLocaleLowerCase();
              const nameMatches = nameTokens.filter((token) => featureName.includes(token)).length;
              const cityMatches = cityToken && featureCity.includes(cityToken) ? 1 : 0;
              const distance = base ? Math.hypot(longitude - base[0], latitude - base[1]) : 0;
              return [{ coordinate: [longitude, latitude] as [number, number], score: nameMatches * 100 + cityMatches * 20 - distance }];
            }).sort((left, right) => right.score - left.score);
            if (candidates[0]) updates[place.id] = candidates[0].coordinate;
          } catch {
            if (controller.signal.aborted) return;
          }
        }
      };
      await Promise.all(Array.from({ length: Math.min(4, missing.length) }, () => worker()));
      if (controller.signal.aborted || !Object.keys(updates).length) return;
      setResolvedCoordinates((current) => ({ ...current, ...updates }));
      onCoordinatesResolved?.(updates);
    };
    void resolve();
    return () => controller.abort();
  }, [coordinateInputKey]);
  const points = places.map((place, index) => {
    const preciseCoordinate = (isCoordinate(place.lnglat) ? place.lnglat : undefined) || resolvedCoordinates[place.id];
    const base = preciseCoordinate || mapLocation(place.city) || mapLocations["Рим"];
    if (preciseCoordinate) {
      return { place, coordinate: preciseCoordinate };
    }
    const sameCityIndex = places
      .slice(0, index)
      .filter((item) => item.city === place.city).length;
    // Catalog rows from older sources may not have a point. Keep those
    // markers in a compact land-side grid around the city center instead of
    // spreading them in a circle, which can place them in a lake or lagoon.
    const column = (sameCityIndex % 5) - 2;
    const row = Math.floor(sameCityIndex / 5) - 1;
    return {
      place,
      coordinate: [
        base[0] + column * 0.0012,
        base[1] + row * 0.0012,
      ] as [number, number],
    };
  });
  const routePoints = Array.from(
    new globalThis.Map(
      places.flatMap((place) => {
        const coordinate = (isCoordinate(place.lnglat) ? place.lnglat : undefined) || resolvedCoordinates[place.id] || mapLocation(place.city);
        return coordinate ? [[place.city, coordinate] as const] : [];
      }),
    ).values(),
  );
  const mapKey = points.map(({ place, coordinate }) => `${place.id}:${coordinate.join(",")}`).join(";");
  const browserLocationKey = browserLocation.state.coordinates?.join(",") || "";
  useEffect(() => {
    if (!container.current || !points.length) return;
    let disposed = false;
    let map: Map | undefined;
    const userCoordinates = browserLocation.state.coordinates;
    void import("mapbox-gl").then(({ default: mapboxgl }) => {
      if (disposed || !container.current) return;
      const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
      if (token) mapboxgl.accessToken = token;
      map = new mapboxgl.Map({
        container: container.current,
        style: mapStyle(),
        center: userCoordinates ?? points[0].coordinate,
        zoom: userCoordinates ? 13 : routePoints.length > 1 ? 5 : 12,
        attributionControl: true,
      });
      mapRef.current = map;
      if (userCoordinates) {
        const element = document.createElement("span");
        element.className = "map-user-location-marker";
        element.title = "Ваше местоположение";
        new mapboxgl.Marker({ element }).setLngLat(userCoordinates).addTo(map);
      }
      markerElements.current.clear();
      map.addControl(new mapboxgl.NavigationControl({ showCompass: false }), "top-right");
      points.forEach(({ place, coordinate }, index) => {
        const marker = document.createElement("span");
        marker.className = "sight-map-marker restaurant-map-marker";
        marker.textContent = String(index + 1);
        marker.title = place.name;
        marker.addEventListener("click", () => onSelect(place.id));
        markerElements.current.set(place.id, marker);
        new mapboxgl.Marker({ element: marker }).setLngLat(coordinate).addTo(map!);
      });
      map.on("load", () => {
        if (routePoints.length > 1) {
          map!.addSource("restaurant-route", {
            type: "geojson",
            data: {
              type: "Feature",
              properties: {},
              geometry: { type: "LineString", coordinates: routePoints },
            },
          });
          map!.addLayer({
            id: "restaurant-route",
            type: "line",
            source: "restaurant-route",
            paint: {
              "line-color": "#5e55df",
              "line-width": 3,
              "line-opacity": 0.72,
              "line-dasharray": [1.2, 1.2],
            },
          });
        }
        const bounds = new mapboxgl.LngLatBounds(points[0].coordinate, points[0].coordinate);
        points.slice(1).forEach(({ coordinate }) => bounds.extend(coordinate));
        map!.fitBounds(bounds, { padding: 54, maxZoom: routePoints.length > 1 ? 8 : 14 });
        if (userCoordinates) {
          map!.flyTo({ center: userCoordinates, zoom: 14, duration: 700, essential: true });
        }
      });
    });
    return () => {
      disposed = true;
      map?.remove();
      mapRef.current = null;
      markerElements.current.clear();
    };
  }, [mapKey, browserLocationKey]);
  useEffect(() => {
    if (!activeRestaurantId) return;
    const marker = markerElements.current.get(activeRestaurantId);
    const point = points.find(({ place }) => place.id === activeRestaurantId);
    if (!marker || !point) return;
    marker.classList.remove("bounce");
    void marker.offsetWidth;
    marker.classList.add("bounce");
    mapRef.current?.flyTo({ center: point.coordinate, zoom: routePoints.length > 1 ? 11 : 14, duration: 650, essential: true });
  }, [activeRestaurantId, mapKey]);
  if (!places.length) {
    return <div className="restaurant-map-empty">Добавленные рестораны появятся здесь автоматически.</div>;
  }
  const activeIndex = activeRestaurantId
    ? points.findIndex(({ place }) => place.id === activeRestaurantId)
    : -1;
  if (!import.meta.env.VITE_MAPBOX_ACCESS_TOKEN) {
    return (
      <div className="map-location-wrap">
        <StaticTripMap
          coordinates={points.map(({ coordinate }) => coordinate)}
          activeDay={activeIndex >= 0 ? activeIndex : undefined}
          focusIndex={activeIndex >= 0 ? activeIndex : undefined}
          mapClassName="restaurant-map"
          markerClassName="sight-map-marker restaurant-map-marker"
          connectWaypoints={false}
          userLocation={browserLocation.state.coordinates}
          onMarkerClick={(index) => {
            const point = points[index];
            if (point) onSelect(point.place.id);
          }}
        />
        <BrowserLocationButton state={browserLocation.state} onRequest={browserLocation.request} />
      </div>
    );
  }
  return (
    <div className="map-location-wrap">
      <div className="restaurant-map" ref={container} aria-label="Карта ресторанов" />
      <BrowserLocationButton state={browserLocation.state} onRequest={browserLocation.request} />
    </div>
  );
}

function RestaurantPage({
  places,
  onChange,
  availableCities = [],
}: {
  places: ImportedRestaurant[];
  onChange: (restaurants: ImportedRestaurant[]) => void;
  availableCities?: string[];
}) {
  const [filterCity, setFilterCity] = useState("Все города");
  const [filterCuisine, setFilterCuisine] = useState("Все кухни");
  const [filterRating, setFilterRating] = useState("Любой рейтинг");
  const [filterPrice, setFilterPrice] = useState("Все цены");
  const [filterStatus, setFilterStatus] = useState("Все статусы");
  const [query, setQuery] = useState("");
  const [citySuggestionsOpen, setCitySuggestionsOpen] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [activeRestaurantId, setActiveRestaurantId] = useState<string | null>(null);
  const [activePhotos, setActivePhotos] = useState<Record<string, number>>({});
  const [expandedPhoto, setExpandedPhoto] = useState<{
    photos: string[];
    index: number;
  } | null>(null);
  const [editing, setEditing] = useState<ImportedRestaurant | null>(null);
  const cityValues = [
    ...availableCities,
    ...places.map((place) => place.city),
  ].filter(Boolean);
  const tripCities = Array.from(
    new globalThis.Map(
      cityValues.map((city) => [city.split(",")[0].trim().toLocaleLowerCase(), city] as const),
    ).values(),
  );
  const cities = [...tripCities].sort((a, b) => a.localeCompare(b, "ru"));
  const citySuggestions = tripCities.filter((city) =>
    !query.trim() || city.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase()),
  );
  const cuisines = Array.from(
    new Set([
      ...restaurantCuisineOptions,
      ...places.map((place) => place.cuisine || ""),
    ].filter(Boolean)),
  ).sort((a, b) => a.localeCompare(b, "ru"));
  const statusLabels: Record<string, string> = {
    "Все статусы": "Все статусы",
    хочу: "Хочу",
    бронь: "Забронировано",
    были: "Были",
  };
  const ratingOptions = [
    { value: "Любой рейтинг", label: "Любой рейтинг", min: 0 },
    { value: "4.0", label: "★ 4,0+", min: 4 },
    { value: "4.5", label: "★ 4,5+", min: 4.5 },
    { value: "4.7", label: "★ 4,7+", min: 4.7 },
    { value: "5.0", label: "★ 5,0", min: 5 },
  ];
  const cuisineFor = (place: ImportedRestaurant) => {
    const explicit = place.cuisine?.trim() ||
      place.categories?.find((category) => restaurantCuisineOptions.includes(category));
    if (explicit) return explicit;
    const text = `${place.name} ${place.city} ${place.note || ""}`.toLowerCase();
    if (/sushi|ramen|izakaya|япон/.test(text)) return "Японская";
    if (/pizza|pizzeria|trattoria|osteria|italian|итальян|рим|флоренц|венеци|верон|милан|пиза/.test(text)) return "Итальянская";
    if (/prague|прага|чеш|koleno|piv|beer|пиво/.test(text)) return "Чешская";
    if (/munich|мюнх|german|немец|salzburg|зальцбург|австр/.test(text)) return "Европейская";
    if (/морепродукт|seafood|fish|рыб/.test(text)) return "Морепродукты";
    return "Европейская";
  };
  const selectedRating = ratingOptions.find((item) => item.value === filterRating) || ratingOptions[0];
  const activeFilterCount = [
    filterCity !== "Все города",
    filterCuisine !== "Все кухни",
    filterRating !== "Любой рейтинг",
    filterPrice !== "Все цены",
    filterStatus !== "Все статусы",
  ].filter(Boolean).length;
  const visible = places.filter(
    (place) => {
      const rating = place.googleRating || 0;
      return (
        (filterCity === "Все города" || place.city.split(",")[0].trim().toLocaleLowerCase() === filterCity.split(",")[0].trim().toLocaleLowerCase()) &&
        (filterCuisine === "Все кухни" || cuisineFor(place) === filterCuisine) &&
        (selectedRating.min === 0 || rating >= selectedRating.min) &&
        (filterPrice === "Все цены" || place.price === filterPrice) &&
        (filterStatus === "Все статусы" || place.status === filterStatus) &&
        `${place.name} ${place.city}`.toLowerCase().includes(query.trim().toLowerCase())
      );
    },
  );
  const resetFilters = () => {
    setFilterCity("Все города");
    setFilterCuisine("Все кухни");
    setFilterRating("Любой рейтинг");
    setFilterPrice("Все цены");
    setFilterStatus("Все статусы");
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
      <div className="restaurant-controls">
        <label
          className="restaurant-search restaurant-search-with-suggestions"
          onFocus={() => setCitySuggestionsOpen(true)}
          onBlur={() => window.setTimeout(() => setCitySuggestionsOpen(false), 140)}
        >
          <span>⌕</span>
          <input
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setCitySuggestionsOpen(true);
            }}
            placeholder="Поиск по ресторану или городу"
          />
          {citySuggestionsOpen && citySuggestions.length > 0 && tripCities.length > 0 && (
            <div className="restaurant-city-search-suggestions" role="listbox" aria-label="Города путешествия">
              {citySuggestions.map((city) => (
                <button
                  type="button"
                  role="option"
                  key={city}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => {
                    setFilterCity(city);
                    setQuery(city.split(",")[0].trim());
                    setCitySuggestionsOpen(false);
                  }}
                >
                  {cityFlag(city)}
                  <span>{city.split(",")[0].trim()}</span>
                </button>
              ))}
            </div>
          )}
        </label>
        <div className="restaurant-filter-wrap">
          <button
            type="button"
            className="restaurant-filter-button"
            aria-expanded={filtersOpen}
            onClick={() => setFiltersOpen((open) => !open)}
          >
            <span>{activeFilterCount || "☷"}</span>
            Фильтр
            <i>⌄</i>
          </button>
          {filtersOpen && (
            <div className="restaurant-filter-popover">
              <div className="restaurant-filter-head">
                <b>Фильтры</b>
                <button type="button" onClick={resetFilters}>Сбросить</button>
              </div>
              <label className="restaurant-filter-field">
                Город
                <AccommodationCityPicker
                  value={filterCity}
                  onChange={setFilterCity}
                  cities={cities}
                  allOption="Все города"
                  placeholder="Все города"
                  className="restaurant-filter-city-picker"
                />
              </label>
              <label className="restaurant-filter-field">
                Кухня
                <select value={filterCuisine} onChange={(event) => setFilterCuisine(event.target.value)}>
                  <option>Все кухни</option>
                  {cuisines.map((item) => <option key={item}>{item}</option>)}
                </select>
              </label>
              <div className="restaurant-filter-field">
                <span>Рейтинг</span>
                <div className="restaurant-filter-options rating-options">
                  {ratingOptions.slice(1).map((item) => (
                    <button
                      type="button"
                      className={filterRating === item.value ? "selected" : ""}
                      onClick={() => setFilterRating(filterRating === item.value ? "Любой рейтинг" : item.value)}
                      key={item.value}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              </div>
              <div className="restaurant-filter-field">
                <span>Цена</span>
                <div className="restaurant-filter-options price-filter-options">
                  {["€", "€€", "€€€", "€€€€"].map((item) => (
                    <button
                      type="button"
                      className={filterPrice === item ? "selected" : ""}
                      onClick={() => setFilterPrice(filterPrice === item ? "Все цены" : item)}
                      key={item}
                    >
                      {item}
                    </button>
                  ))}
                </div>
              </div>
              <label className="restaurant-filter-field restaurant-filter-status">
                Статус
                <select value={filterStatus} onChange={(event) => setFilterStatus(event.target.value)}>
                  {Object.keys(statusLabels).map((item) => (
                    <option value={item} key={item}>{statusLabels[item]}</option>
                  ))}
                </select>
              </label>
              <button
                type="button"
                className="restaurant-filter-apply"
                onClick={() => setFiltersOpen(false)}
              >
                Показать {visible.length} ресторанов
              </button>
            </div>
          )}
        </div>
      </div>
      <div className="restaurants-map-layout">
        <div className="restaurant-list-column">
          <div className="restaurant-list-head">
            <span>{visible.length} {visible.length === 1 ? "место" : visible.length < 5 ? "места" : "мест"}</span>
            <span>Нажмите на карточку, чтобы увидеть точку на карте</span>
          </div>
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
            <article
              className={`restaurant-card c${index % 6}${activeRestaurantId === place.id ? " active" : ""}`}
              key={place.id}
              onClick={() => setActiveRestaurantId(place.id)}
            >
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
        </div>
        <aside className="restaurants-map-panel">
          <header>
            <span>Карта ресторанов</span>
            <b>{visible.length} точек</b>
          </header>
          <RestaurantMap
            places={visible}
            activeRestaurantId={activeRestaurantId || undefined}
            onSelect={setActiveRestaurantId}
            onCoordinatesResolved={(updates) => {
              const changed = places.some((place) => updates[place.id] && !place.lnglat);
              if (!changed) return;
              onChange(places.map((place) => updates[place.id] ? { ...place, lnglat: updates[place.id] } : place));
            }}
          />
        </aside>
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
        cities={cities}
        onClose={() => setEditing(null)}
        onSave={saveRestaurant}
        onDelete={() => deleteRestaurant(editing.id)}
      />
    )}
    </>
  );
}

function RestaurantForm({
  onClose,
  onSave,
  cities,
}: {
  onClose: () => void;
  onSave: (restaurant: ImportedRestaurant) => void;
  cities: string[];
}) {
  const [name, setName] = useState("Trattoria Mario");
  const [city, setCity] = useState(cities[0] || "Рим");
  const [status, setStatus] = useState("хочу");
  const [priority, setPriority] = useState(false);
  const [price, setPrice] = useState("€€");
  const [cuisine, setCuisine] = useState("Тоскана");
  const [note, setNote] = useState("");
  const [link, setLink] = useState("");
  const [cuisineFocused, setCuisineFocused] = useState(false);
  const [photos, setPhotos] = useState(["", "", ""]);
  const [catalogOpen, setCatalogOpen] = useState(false);
  const matchingCuisines = restaurantCuisineOptions
    .filter((item) =>
      item.toLocaleLowerCase().includes(cuisine.trim().toLocaleLowerCase()),
    )
    .slice(0, 8);
  const chooseCuisine = (value: string) => {
    setCuisine(value);
    setCuisineFocused(false);
  };
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
  const chooseFromCatalog = (restaurant: ImportedRestaurant) => {
    setName(restaurant.name);
    setCity(restaurant.city);
    setCuisine(restaurant.cuisine || "");
    setNote(restaurant.note || "");
    setLink(restaurant.link || "");
    setPrice(restaurant.price || "€€");
    setPhotos([...(restaurant.photos || []), "", "", ""].slice(0, 3));
    setCatalogOpen(false);
  };
  return (
    <div className="restaurant-modal-backdrop" onClick={onClose}>
      <form
        className="restaurant-modal restaurant-full-form"
        onSubmit={(event) => {
          event.preventDefault();
          const nextRestaurant: ImportedRestaurant = {
            id: crypto.randomUUID(),
            name: name.trim() || "Новый ресторан",
            city: city.trim() || cities[0] || "Рим",
            status,
            priority,
            price,
            cuisine: cuisine.trim() || undefined,
            note: note.trim() || undefined,
            link: link.trim() || undefined,
            // Local previews are intentionally not persisted as restaurant data.
            // Uploaded photos can be added later from the editor.
            photos: photos.filter((photo) => photo && !photo.startsWith("blob:")),
          };
          onSave(nextRestaurant);
          onClose();
        }}
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <div className="restaurant-form-title">
            <h2>Новый ресторан</h2>
            <button
              type="button"
              className="restaurant-form-catalog-button"
              onClick={() => setCatalogOpen(true)}
            >
              Каталог
            </button>
          </div>
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
        <button
          type="button"
          className="restaurant-form-catalog-wide"
          onClick={() => setCatalogOpen(true)}
        >
          ＋ Выбрать ресторан из каталога
        </button>
        <label>
          Название
          <input value={name} onChange={(event) => setName(event.target.value)} />
        </label>
        <div className="restaurant-form-grid">
          <label>
            Город
            <AccommodationCityPicker
              value={city}
              onChange={setCity}
              cities={cities}
              placeholder="Начните вводить город"
            />
          </label>
          <label>
            Кухня
            <div className="restaurant-cuisine-field">
              <input
                value={cuisine}
                onChange={(event) => setCuisine(event.target.value)}
                onFocus={() => setCuisineFocused(true)}
                onBlur={() => window.setTimeout(() => setCuisineFocused(false), 120)}
                aria-label="Кухня"
                aria-autocomplete="list"
                aria-controls="restaurant-cuisine-suggestions"
                placeholder="Начните вводить кухню"
              />
              {cuisineFocused && matchingCuisines.length > 0 && (
                <div
                  id="restaurant-cuisine-suggestions"
                  className="restaurant-cuisine-suggestions"
                  role="listbox"
                  aria-label="Варианты кухни"
                >
                  {matchingCuisines.map((item) => (
                    <button
                      type="button"
                      role="option"
                      aria-selected={cuisine === item}
                      key={item}
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => chooseCuisine(item)}
                    >
                      {item}
                    </button>
                  ))}
                </div>
              )}
            </div>
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
              {["€", "€€", "€€€", "€€€€"].map((item) => (
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
          <input
            value={note}
            onChange={(event) => setNote(event.target.value)}
            placeholder="Например, Via Rosina, 2"
          />
        </label>
        <label>
          Ссылка Google Maps
          <input
            type="url"
            value={link}
            onChange={(event) => setLink(event.target.value)}
            placeholder="https://maps.google.com/..."
          />
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
      {catalogOpen && (
        <RestaurantCatalog
          cities={cities}
          onClose={() => setCatalogOpen(false)}
          onPick={chooseFromCatalog}
        />
      )}
    </div>
  );
}

function RestaurantEditor({
  restaurant,
  cities,
  onClose,
  onSave,
  onDelete,
}: {
  restaurant: ImportedRestaurant;
  cities: string[];
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
      const url = await signedTripPhotoUrl(path);
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
        <label>
          Город
          <AccommodationCityPicker
            value={city}
            onChange={setCity}
            cities={cities}
            placeholder="Начните вводить город"
          />
        </label>
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

function RestaurantCatalog({
  cities,
  onClose,
  onAdd,
  onPick,
}: {
  cities: string[];
  onClose: () => void;
  onAdd?: (restaurant: ImportedRestaurant) => void;
  onPick?: (restaurant: ImportedRestaurant) => void;
}) {
  const [city, setCity] = useState("");
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<ImportedRestaurant[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [addedIds, setAddedIds] = useState<string[]>([]);

  useEffect(() => {
    const selectedCity = city.trim();
    if (selectedCity.length < 2) {
      setItems([]);
      setLoading(false);
      setError("");
      return;
    }
    const controller = new AbortController();
    const timeout = window.setTimeout(async () => {
      setLoading(true);
      setError("");
      try {
        const nextItems = await fetchRestaurantCatalog(selectedCity, query, controller.signal);
        setItems(nextItems);
        void enrichRestaurantCatalogPhotos(nextItems, controller.signal)
          .then((enrichedItems) => {
            if (controller.signal.aborted || enrichedItems === nextItems) return;
            const byId = new globalThis.Map(enrichedItems.map((item) => [item.id, item]));
            setItems((current) => current.map((item) => byId.get(item.id) || item));
          })
          .catch(() => {
            // Photo enrichment is optional; the catalog cards are already usable.
          });
        if (!nextItems.length) {
          setError("По этому городу пока не нашли рестораны. Попробуйте уточнить поиск.");
        }
      } catch {
        if (!controller.signal.aborted) {
          setItems([]);
          setError("Каталог временно недоступен. Проверьте подключение и попробуйте ещё раз.");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }, 360);
    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [city, query]);

  const selectRestaurant = (restaurant: ImportedRestaurant) => {
    if (onPick) {
      onPick(restaurant);
      return;
    }
    if (!onAdd) return;
    onAdd({ ...restaurant, id: crypto.randomUUID() });
    setAddedIds((current) => [...current, restaurant.id]);
  };

  return (
    <div className="restaurant-modal-backdrop" onClick={onClose}>
      <section
        className="restaurant-modal restaurant-catalog-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="restaurant-catalog-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <div>
            <p className="restaurant-catalog-eyebrow">КАТАЛОГ РЕСТОРАНОВ</p>
            <h2 id="restaurant-catalog-title">Добавить ресторан</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Закрыть">×</button>
        </header>
        <div className="restaurant-catalog-toolbar">
          <label>
            Город
            <AccommodationCityPicker
              value={city}
              onChange={setCity}
              cities={cities}
              placeholder="Начните вводить город"
            />
          </label>
          <label className="restaurant-catalog-search">
            Поиск
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Например, пицца или trattoria"
            />
          </label>
        </div>
        <p className="restaurant-catalog-source">
          Google Places: до 60 ресторанов с рейтингом, отзывами, фотографиями и точками на карте.
        </p>
        {loading && <div className="restaurant-catalog-state">Загружаем рестораны…</div>}
        {!loading && error && <div className="restaurant-catalog-state is-error">{error}</div>}
        {!loading && !error && !items.length && (
          <div className="restaurant-catalog-state">
            Выберите город, чтобы загрузить рестораны.
          </div>
        )}
        {!loading && !error && items.length > 0 && (
          <div className="restaurant-catalog-grid">
            {items.map((restaurant) => {
              const added = addedIds.includes(restaurant.id);
              return (
                <article className="restaurant-catalog-card" key={restaurant.id}>
                  <div className="restaurant-catalog-card-photo">
                    {restaurant.photos?.[0] ? (
                      <img src={restaurant.photos[0]} alt="" loading="lazy" />
                    ) : (
                      <span aria-hidden="true">🍽</span>
                    )}
                  </div>
                  <div className="restaurant-catalog-card-body">
                    <p>{restaurant.placeType || "Ресторан"}</p>
                    <h3>{restaurant.name}</h3>
                    <span className="restaurant-catalog-card-city">{restaurant.city}</span>
                    {restaurant.googleRating !== undefined && (
                      <span className="restaurant-catalog-rating">
                        <b>★ {restaurant.googleRating.toFixed(1)}</b>
                        {restaurant.googleReviews !== undefined && ` · ${restaurant.googleReviews.toLocaleString("ru-RU")} отзывов`}
                      </span>
                    )}
                    {restaurant.note && <small>{restaurant.note}</small>}
                    <button
                      type="button"
                      className={added && !onPick ? "added" : ""}
                      disabled={added && !onPick}
                      onClick={() => selectRestaurant(restaurant)}
                    >
                      {added && !onPick ? "Добавлено" : onPick ? "Выбрать" : "＋ Добавить"}
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
        <footer className="restaurant-catalog-footer">
          <span>{items.length ? `${items.length} мест найдено` : ""}</span>
          <button type="button" onClick={onClose}>Готово</button>
        </footer>
      </section>
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
  const restaurantCities = Array.from(
    new Set(
      [
        ...(trip.cities || "").split("·").map((city) => city.trim()),
        ...(trip.days || []).flatMap((day) =>
          day.roadLeg ? [day.roadLeg.from, day.roadLeg.to] : [],
        ),
      ].filter(Boolean),
    ),
  );
  return (
    <div className="restaurants-with-add">
      <RestaurantPage
        places={trip.restaurants || []}
        availableCities={restaurantCities}
        onChange={(restaurants) => onUpdateTrip({ ...trip, restaurants })}
      />
      <div className="restaurant-actions">
        <button
          className="restaurant-add-button"
          type="button"
          onClick={() => setAddingRestaurant(true)}
        >
          ＋ Добавить
        </button>
      </div>
      {addingRestaurant && (
        <RestaurantForm
          cities={restaurantCities}
          onSave={(restaurant) =>
            onUpdateTrip({
              ...trip,
              restaurants: [...(trip.restaurants || []), restaurant],
            })
          }
          onClose={() => setAddingRestaurant(false)}
        />
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
                    Ссылка на жильё →
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
          <input placeholder="Например, Hotel Artemide" />
        </label>
        <div className="accommodation-form-grid">
          <label>
            Город
            <select defaultValue="">
              <option value="">Выберите город</option>
              <option>Рим, Италия</option>
              <option>Флоренция, Италия</option>
              <option>Венеция, Италия</option>
            </select>
          </label>
          <label>
            Цена
            <input placeholder="Например, €434" />
          </label>
          <label>
            Заезд
            <select defaultValue="">
              <option value="">Выберите дату</option>
              <option>27 сен</option>
              <option>28 сен</option>
              <option>29 сен</option>
            </select>
          </label>
          <label>
            Выезд
            <select defaultValue="">
              <option value="">Выберите дату</option>
              <option>30 сен</option>
              <option>1 окт</option>
              <option>2 окт</option>
            </select>
          </label>
        </div>
        <label>
          Ссылка на жильё
          <input placeholder="https://..." />
        </label>
        <label>
          Адрес / заметка
          <textarea placeholder="Адрес, условия или заметка" />
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

function AccommodationCatalog({
  cities,
  onClose,
  onPick,
}: {
  cities: string[];
  onClose: () => void;
  onPick: (stay: ImportedAccommodation) => void;
}) {
  const [city, setCity] = useState("");
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<ImportedAccommodation[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const selectedCity = city.trim();
    if (selectedCity.length < 2) {
      setItems([]);
      setLoading(false);
      setError("");
      return;
    }
    const controller = new AbortController();
    const timeout = window.setTimeout(async () => {
      setLoading(true);
      setError("");
      try {
        const nextItems = await fetchGoogleAccommodationCatalog(
          selectedCity,
          query,
          controller.signal,
        );
        setItems(nextItems);
        void enrichAccommodationCatalogPhotos(nextItems, controller.signal)
          .then((enrichedItems) => {
            if (controller.signal.aborted || enrichedItems === nextItems) return;
            const byId = new globalThis.Map(enrichedItems.map((item) => [item.id, item]));
            setItems((current) => current.map((item) => byId.get(item.id) || item));
          })
          .catch(() => {
            // Photos are optional enrichment; cards remain usable without them.
          });
        if (!nextItems.length) setError("По этому городу пока ничего не нашли.");
      } catch {
        if (!controller.signal.aborted) {
          setItems([]);
          setError("Каталог временно недоступен. Попробуйте ещё раз.");
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }, 360);
    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [city, query]);

  return (
    <div className="restaurant-modal-backdrop accommodation-catalog-backdrop" onClick={onClose}>
      <section
        className="restaurant-modal accommodation-catalog-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="accommodation-catalog-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header>
          <div>
            <p className="accommodation-catalog-eyebrow">КАТАЛОГ ЖИЛЬЯ</p>
            <h2 id="accommodation-catalog-title">Выбрать жильё</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Закрыть">×</button>
        </header>
        <div className="accommodation-catalog-toolbar">
          <label>
            Город
            <AccommodationCityPicker
              value={city}
              onChange={setCity}
              cities={cities}
              placeholder="Начните вводить город"
            />
          </label>
          <label>
            Поиск
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Например, hotel или apartment"
            />
          </label>
        </div>
        <p className="accommodation-catalog-source">
          Google Places: жильё с фотографиями, рейтингом, отзывами и адресом.
        </p>
        {loading && <div className="accommodation-catalog-state">Загружаем варианты…</div>}
        {!loading && error && <div className="accommodation-catalog-state is-error">{error}</div>}
        {!loading && !error && !items.length && (
          <div className="accommodation-catalog-state">Выберите город, чтобы загрузить жильё.</div>
        )}
        {!loading && !error && items.length > 0 && (
          <div className="accommodation-catalog-grid">
            {items.map((stay) => (
              <article className="accommodation-catalog-card" key={stay.id}>
                <div className="accommodation-catalog-card-photo">
                  {stay.photos?.[0] ? <img src={stay.photos[0]} alt="" loading="lazy" /> : <span aria-hidden="true">⌂</span>}
                </div>
                <div className="accommodation-catalog-card-body">
                  <small>{stay.placeType || "Жильё"}</small>
                  <h3>{stay.name}</h3>
                  <span className="accommodation-catalog-card-city">{stay.city}</span>
                  {stay.googleRating !== undefined && (
                    <span className="accommodation-catalog-rating">
                      <b>★ {stay.googleRating.toFixed(1)}</b>
                      {stay.googleReviews !== undefined && ` · ${stay.googleReviews.toLocaleString("ru-RU")} отзывов`}
                    </span>
                  )}
                  <p>{stay.description || stay.address || "Адрес будет добавлен после выбора"}</p>
                  <div className="accommodation-catalog-card-actions">
                    <button type="button" onClick={() => onPick(stay)}>Выбрать</button>
                    {stay.link && (
                      <a
                        className="accommodation-catalog-card-link"
                        href={externalUrl(stay.link)}
                        target="_blank"
                        rel="noreferrer"
                      >
                        Открыть ссылку →
                      </a>
                    )}
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
        <footer className="accommodation-catalog-footer">
          <span>{items.length ? `${items.length} вариантов найдено` : ""}</span>
          <button type="button" onClick={onClose}>Готово</button>
        </footer>
      </section>
    </div>
  );
}

function AccommodationForm({
  onClose,
  onSaved,
  onDelete,
  initial,
  tripId,
  cities = accommodationCities,
}: {
  onClose: () => void;
  onSaved?: (accommodation: SavedAccommodation) => void;
  onDelete?: () => void;
  initial?: SavedAccommodation;
  tripId: string;
  cities?: string[];
}) {
  const dateParts = initial
    ? accommodationDateParts(initial.dates)
    : { checkIn: "", checkOut: "" };
  const [checkIn, setCheckIn] = useState(dateParts.checkIn);
  const [checkOut, setCheckOut] = useState(dateParts.checkOut);
  const [freeCancellation, setFreeCancellation] = useState(initial?.deadline || "");
  const [status, setStatus] = useState(initial?.status || "бронь");
  const [name, setName] = useState(initial?.name || "");
  const [city, setCity] = useState(initial?.city || "");
  const [bookingUrl, setBookingUrl] = useState(initial?.bookingUrl || "");
  const [details, setDetails] = useState(initial?.details || "");
  const [googleRating, setGoogleRating] = useState(initial?.googleRating);
  const [googleReviews, setGoogleReviews] = useState(initial?.googleReviews);
  const [catalogOpen, setCatalogOpen] = useState(false);
  const parsedPrice = parseAccommodationPrice(initial?.price);
  const [price, setPrice] = useState(parsedPrice.amount);
  const [priceCurrency, setPriceCurrency] = useState<AccommodationCurrency>(parsedPrice.currency);
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
  const [photosToDelete, setPhotosToDelete] = useState<string[]>([]);
  const [deletingPhotos, setDeletingPhotos] = useState(false);
  const isUploading = uploadingPhotos.some(Boolean);
  const isBusy = isUploading || deletingPhotos;
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
      const path = `${session.user.id}/${tripId}/accommodations/${crypto.randomUUID()}.${extension}`;
      const { error } = await supabase.storage
        .from("trip-photos")
        .upload(path, file, {
          cacheControl: "31536000",
          contentType: file.type,
          upsert: false,
        });
      if (error) throw error;
      const publicUrl = await signedTripPhotoUrl(path);
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
  const removePhoto = (index: number) => {
    const photo = photos[index];
    const path = photo ? tripPhotoPath(photo) : null;
    if (path) {
      setPhotosToDelete((current) =>
        current.includes(path) ? current : [...current, path],
      );
    }
    setPhotos((current) =>
      current.map((item, photoIndex) => (photoIndex === index ? "" : item)),
    );
  };
  return (
    <div className="accommodation-modal-backdrop">
      <form
        className="accommodation-modal"
        onSubmit={async (event) => {
          event.preventDefault();
          if (isBusy) return;
          const data = new FormData(event.currentTarget);
          const storagePathsToDelete = photosToDelete.filter(
            (path) => path.split("/")[1] === tripId,
          );
          if (storagePathsToDelete.length) {
            setDeletingPhotos(true);
            try {
              const { error } = await supabase.storage
                .from("trip-photos")
                .remove(storagePathsToDelete);
              if (error) throw error;
            } catch {
              setDeletingPhotos(false);
              window.alert("Не удалось удалить фотографию. Попробуйте ещё раз.");
              return;
            }
            setDeletingPhotos(false);
          }
          const cancellation: SavedAccommodation = {
            id: initial?.id || crypto.randomUUID(),
            name: name || "Новое жильё",
            city: city || "Город не указан",
            dates: `${data.get("checkIn")} – ${data.get("checkOut")}`,
            days: 30,
            deadline: String(data.get("freeCancellation") || ""),
            progress: 42,
            status,
            price: formatAccommodationPriceValue(price, priceCurrency),
            bookingUrl,
            details,
            photos: photos.filter(Boolean),
            googleRating,
            googleReviews,
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
          <div className="accommodation-modal-header-actions">
            <button
              type="button"
              className="accommodation-header-catalog-button"
              onClick={() => setCatalogOpen(true)}
              disabled={isBusy}
            >
              ＋ Добавить из каталога
            </button>
            <button type="button" onClick={onClose} disabled={isBusy} aria-label="Закрыть">
              ×
            </button>
          </div>
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
                  <>
                    <span
                      className="accommodation-preview-frame"
                      style={{
                        backgroundImage: `url(${photo})`,
                        backgroundPosition: `${photoTransforms[index].offsetX}% ${photoTransforms[index].offsetY}%`,
                      }}
                    />
                    <button
                      type="button"
                      className="accommodation-photo-remove"
                      aria-label={`Удалить фото ${index + 1}`}
                      disabled={isBusy}
                      onMouseDown={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                      }}
                      onClick={(event) => {
                        event.preventDefault();
                        event.stopPropagation();
                        removePhoto(index);
                      }}
                    >
                      ×
                    </button>
                  </>
                ) : (
                  <>
                    <span className="accommodation-upload-icon" aria-hidden="true">
                      <svg viewBox="0 0 32 28" focusable="false">
                        <rect x="2" y="3" width="28" height="22" rx="3" />
                        <circle cx="10" cy="10" r="2.5" />
                        <path d="m5 22 7-7 5 5 3-3 7 5" />
                      </svg>
                    </span>
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
        {catalogOpen && (
          <AccommodationCatalog
            cities={cities}
            onClose={() => setCatalogOpen(false)}
            onPick={(stay) => {
              setName(stay.name);
              setCity(stay.city);
              setBookingUrl(stay.link || "");
              setDetails(stay.address || stay.description || "");
              setGoogleRating(stay.googleRating);
              setGoogleReviews(stay.googleReviews);
              setPhotos([...(stay.photos || []), "", ""].slice(0, 3));
              setCatalogOpen(false);
            }}
          />
        )}
        <label>
          Название
          <input
            name="name"
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Например, Hotel Artemide"
          />
        </label>
        <div className="accommodation-form-grid">
          <label>
            Город
            <AccommodationCityPicker
              value={city}
              onChange={setCity}
              cities={cities}
            />
          </label>
          <label>
            Цена
            <div className="accommodation-price-field">
              <input
                name="price"
                value={price}
                onChange={(event) => setPrice(event.target.value)}
                placeholder="Например, 434"
                inputMode="decimal"
              />
              <select
                value={priceCurrency}
                onChange={(event) => setPriceCurrency(event.target.value as AccommodationCurrency)}
                aria-label="Валюта цены"
              >
                {accommodationCurrencies.map((currency) => (
                  <option value={currency.value} key={currency.value}>
                    {currency.symbol} {currency.label}
                  </option>
                ))}
              </select>
            </div>
          </label>
          <DatePicker
            label="Заезд"
            name="checkIn"
            value={checkIn}
            onChange={setCheckIn}
            className="accommodation-date-picker"
          />
          <DatePicker
            label="Выезд"
            name="checkOut"
            value={checkOut}
            onChange={setCheckOut}
            className="accommodation-date-picker"
          />
        </div>
        <DatePicker
          label="Бесплатная отмена до"
          name="freeCancellation"
          value={freeCancellation}
          onChange={setFreeCancellation}
          className="accommodation-date-picker"
        />
        <label>
          Ссылка на жильё
          <input name="bookingUrl" value={bookingUrl} onChange={(event) => setBookingUrl(event.target.value)} placeholder="https://..." />
        </label>
        <label>
          Адрес / заметка
          <textarea
            name="details"
            value={details}
            onChange={(event) => setDetails(event.target.value)}
            placeholder="Адрес, условия или заметка"
          />
        </label>
        <footer>
          {initial && onDelete && (
            <button
              type="button"
              className="accommodation-delete-button"
              disabled={isBusy}
              onClick={() => {
                if (window.confirm(`Удалить жильё «${name || "без названия"}»? Это действие нельзя отменить.`)) {
                  onDelete();
                }
              }}
            >
              Удалить жильё
            </button>
          )}
          <button type="button" onClick={onClose} disabled={isBusy}>
            Отмена
          </button>
          <button className="accent" disabled={isBusy}>
            {deletingPhotos
              ? "Удаляем фото..."
              : isUploading
                ? "Загружаем фото..."
                : "Сохранить жильё"}
          </button>
        </footer>
      </form>
    </div>
  );
}

function AccommodationList({
  stays,
  onChange,
  tripId,
  cities = accommodationCities,
}: {
  stays: SavedAccommodation[];
  onChange: (accommodations: SavedAccommodation[]) => void;
  tripId: string;
  cities?: string[];
}) {
  const [filter, setFilter] = useState("Все");
  const [orderMode, setOrderMode] = useState<"date" | "manual">("date");
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
  const [draggedStayId, setDraggedStayId] = useState<string | null>(null);
  const [dropTargetStayId, setDropTargetStayId] = useState<string | null>(null);
  const saveStay = (stay: SavedAccommodation) => {
    const index = stays.findIndex((item) => item.id === stay.id);
    onChange(
      index === -1
        ? [...stays, stay]
        : stays.map((item) => (item.id === stay.id ? stay : item)),
    );
    setStatuses((current) => ({ ...current, [stay.name]: stay.status }));
  };
  const orderedStays = orderMode === "date"
    ? stays
        .map((stay, index) => ({ stay, index }))
        .sort((first, second) =>
          accommodationStartTime(first.stay) - accommodationStartTime(second.stay) ||
          first.index - second.index,
        )
        .map(({ stay }) => stay)
    : stays;
  const visible = orderedStays.filter(
    (stay) => filter === "Все" || statuses[stay.name] === filter,
  );
  const canReorder = orderMode === "manual" && filter === "Все";
  const reorderStays = (fromId: string, toId: string) => {
    if (fromId === toId) return;
    const fromIndex = stays.findIndex((stay) => stay.id === fromId);
    const toIndex = stays.findIndex((stay) => stay.id === toId);
    if (fromIndex === -1 || toIndex === -1) return;
    const next = [...stays];
    const [moved] = next.splice(fromIndex, 1);
    if (!moved) return;
    next.splice(toIndex, 0, moved);
    onChange(next);
  };
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
        <div className="accommodation-order-controls" aria-label="Порядок жилья">
          <span>Порядок:</span>
          <button
            className={orderMode === "date" ? "active" : ""}
            onClick={() => {
              setOrderMode("date");
              setDraggedStayId(null);
              setDropTargetStayId(null);
            }}
          >
            По датам
          </button>
          <button
            className={orderMode === "manual" ? "active" : ""}
            onClick={() => setOrderMode("manual")}
          >
            Вручную
          </button>
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
                className={`accommodation-card c${index % 6}${canReorder ? " is-draggable" : ""}${draggedStayId === stay.id ? " is-dragging" : ""}${dropTargetStayId === stay.id ? " is-drop-target" : ""}`}
                key={stay.name}
                draggable={canReorder}
                onDragStart={(event) => {
                  if (!canReorder) return;
                  event.dataTransfer.effectAllowed = "move";
                  setDraggedStayId(stay.id);
                }}
                onDragOver={(event) => {
                  if (!canReorder || !draggedStayId || draggedStayId === stay.id) return;
                  event.preventDefault();
                  event.dataTransfer.dropEffect = "move";
                  setDropTargetStayId(stay.id);
                }}
                onDrop={(event) => {
                  event.preventDefault();
                  if (canReorder && draggedStayId) reorderStays(draggedStayId, stay.id);
                  setDraggedStayId(null);
                  setDropTargetStayId(null);
                }}
                onDragEnd={() => {
                  setDraggedStayId(null);
                  setDropTargetStayId(null);
                }}
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
                {stay.googleRating !== undefined && (
                  <div className="stay-rating">
                    <span>★</span> {stay.googleRating.toFixed(1)}
                    {stay.googleReviews !== undefined && ` · ${stay.googleReviews.toLocaleString("ru-RU")} отзывов`}
                  </div>
                )}
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
                    Ссылка на жильё →
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
          tripId={tripId}
          cities={cities}
          onClose={() => setAdding(false)}
          onSaved={(stay) => {
            saveStay(stay);
          }}
        />
      )}
      {editing && (
        <AccommodationForm
          tripId={tripId}
          cities={cities}
          initial={editing}
          onClose={() => setEditing(null)}
          onDelete={() => {
            onChange(stays.filter((item) => item.id !== editing.id));
            setEditing(null);
          }}
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
  const [nearestFirst, setNearestFirst] = useState(true);
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
    .sort((first, second) =>
      nearestFirst ? first.days - second.days : second.days - first.days,
    );
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
        <button
          type="button"
          aria-pressed={nearestFirst}
          onClick={() => setNearestFirst((current) => !current)}
        >
          ↕ {nearestFirst ? "Сначала ближайшие" : "Сначала дальние"}
        </button>
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
  const tripCityOptions = Array.from(
    new Set([
      ...accommodationCities,
      ...(trip.cities || "").split("·").map((city) => city.trim()),
      ...(trip.days || []).flatMap((day) =>
        day.roadLeg ? [day.roadLeg.from, day.roadLeg.to] : [],
      ),
    ].filter(Boolean)),
  );
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
        {adding && (
          <AccommodationForm
            tripId={trip.id}
            cities={tripCityOptions}
            onClose={() => setAdding(false)}
          />
        )}
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
        tripId={trip.id}
        cities={tripCityOptions}
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
  const [name, setName] = useState(initial?.name || "");
  const [entryCurrency, setEntryCurrency] = useState<BudgetCurrency>(
    initial?.currency || currency,
  );
  const [category, setCategory] = useState(
    () => inferBudgetCategory(initial?.name || "") || initial?.category || "Еда и рестораны",
  );
  const [categoryManuallySelected, setCategoryManuallySelected] = useState(false);
  const [scope, setScope] = useState<BudgetScope>(initial?.scope || "общий");
  const [error, setError] = useState("");
  useEffect(() => {
    if (categoryManuallySelected) return;
    const inferred = inferBudgetCategory(name);
    if (inferred && inferred !== category) setCategory(inferred);
  }, [category, categoryManuallySelected, name]);
  return (
    <div className="expense-modal-backdrop" onClick={onClose}>
      <form
        className="expense-modal"
        onSubmit={(event) => {
          event.preventDefault();
          const form = new FormData(event.currentTarget);
          const amount = Number(String(form.get("amount") || "").replace(",", "."));
          const expenseName = String(form.get("name") || "").trim();
          if (!expenseName) {
            setError("Укажите название траты.");
            return;
          }
          if (
            !Number.isFinite(amount) ||
            amount < 0 ||
            (!initial && amount === 0)
          ) {
            setError("Укажите корректную сумму.");
            return;
          }
          const selectedCurrency = budgetCurrencies[entryCurrency]
            ? entryCurrency
            : currency;
          setError("");
          onSave({
            id: initial?.id || crypto.randomUUID(),
            name: expenseName,
            amount: amount / budgetCurrencies[selectedCurrency].rate,
            currency: selectedCurrency,
            category: categoryManuallySelected ? category : inferBudgetCategory(expenseName) || category,
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
          Название<input name="name" autoFocus value={name} onChange={(event) => setName(event.target.value)} placeholder="Напр. жильё в Равенсбурге" />
        </label>
        <div className="expense-form-grid">
          <label>
            Сумма
            <div className="expense-amount-control">
              <input
                name="amount"
                inputMode="decimal"
                defaultValue={
                  initial
                    ? initial.amount * budgetCurrencies[entryCurrency].rate
                    : undefined
                }
                placeholder="0"
              />
              <select
                name="currency"
                aria-label="Валюта траты"
                value={entryCurrency}
                onChange={(event) =>
                  setEntryCurrency(event.target.value as BudgetCurrency)
                }
              >
                {(Object.keys(budgetCurrencies) as BudgetCurrency[]).map(
                  (item) => (
                    <option value={item} key={item}>
                      {item}
                    </option>
                  ),
                )}
              </select>
            </div>
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
            {budgetCategories.map(
              (item) => (
                <button
                  type="button"
                  className={category === item ? "active" : ""}
                  onClick={() => {
                    setCategoryManuallySelected(true);
                    setCategory(item);
                  }}
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
        {error && <p className="form-error" role="alert">{error}</p>}
        <footer>
          <button type="button" onClick={onClose}>
            Отмена
          </button>
          <button className="accent" type="submit">{initial ? "Сохранить" : "Добавить"}</button>
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
  const storedExpenses = trip.budgetExpenses || [];
  const expenses = storedExpenses.map(normalizeBudgetExpense);
  const hasAutoCategories = expenses.some(
    (expense, index) => expense.category !== storedExpenses[index]?.category,
  );
  useEffect(() => {
    if (hasAutoCategories) onUpdateTrip({ ...trip, budgetExpenses: expenses });
  }, [hasAutoCategories]);
  const currency = trip.budgetCurrency || "EUR";
  const split = trip.budgetSplit || {
    groups: [
      { id: "family", name: "Моя семья", people: 2 },
      { id: "friend", name: "Друг", people: 1 },
    ],
  };
  const categories = budgetCategories;
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
      <article className="panel budget-table-panel">
        <header className="budget-table-heading">
          <div>
            <h2>Траты по категориям</h2>
            <p>Все расходы в одной таблице</p>
          </div>
          <span>{expenses.length} {expenses.length === 1 ? "трата" : expenses.length < 5 ? "траты" : "трат"}</span>
        </header>
        <div className="budget-table-scroll">
          <table className="budget-table">
            <thead>
              <tr>
                <th>Категория</th>
                <th>Трата</th>
                <th>Кто платил</th>
                <th>Дата</th>
                <th>Сумма</th>
                <th aria-label="Действия" />
              </tr>
            </thead>
            <tbody>
              {categories.flatMap((category) => {
                const categoryExpenses = expenses.filter(
                  (expense) => expense.category === category,
                );
                const total = categoryExpenses.reduce(
                  (sum, expense) => sum + expense.amount,
                  0,
                );
                const all = expenses.reduce(
                  (sum, expense) => sum + expense.amount,
                  0,
                );
                return [
                  <tr className="budget-table-category" key={`category-${category}`}>
                    <td colSpan={6}>
                      <div className="budget-table-category-head">
                        <b>{category}</b>
                        <span>{formatAmount(total)}</span>
                      </div>
                      <div className="budget-table-progress">
                        <i style={{ width: `${all ? (total / all) * 100 : 0}%` }} />
                      </div>
                    </td>
                  </tr>,
                  ...categoryExpenses.map((expense) => (
                    <tr key={expense.id}>
                      <td data-label="Категория">{category}</td>
                      <td data-label="Трата" className="budget-table-expense-name">
                        <b>{expense.name}</b>
                        <small>
                          {expense.scope === "общий"
                            ? "Общий"
                            : expense.scope === "семья"
                              ? "Семья"
                              : "Личный"}
                        </small>
                      </td>
                      <td data-label="Кто платил">{expense.paidBy || "—"}</td>
                      <td data-label="Дата">
                        {expense.date
                          ? expense.date.split("-").reverse().join(".")
                          : "Без даты"}
                      </td>
                      <td data-label="Сумма" className="budget-table-amount">
                        {formatAmount(expense.amount)}
                      </td>
                      <td className="budget-table-action">
                        <button
                          type="button"
                          className="budget-expense-edit"
                          aria-label={`Редактировать трату ${expense.name}`}
                          title="Редактировать"
                          onClick={() => setEditing(expense)}
                        >
                          ✎
                        </button>
                      </td>
                    </tr>
                  )),
                ];
              })}
              {!expenses.length && (
                <tr>
                  <td className="budget-table-empty" colSpan={6}>
                    Добавьте первую трату и выберите: общий, семейный или личный бюджет.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </article>
      {adding && <ExpenseForm currency={currency} onClose={() => setAdding(false)} onSave={saveExpense} />}
      {editing && <ExpenseForm currency={currency} initial={editing} onClose={() => setEditing(null)} onSave={saveExpense} />}
      {splitting && <BudgetSplitForm currency={currency} initial={split} total={sharedTotal} onClose={() => setSplitting(false)} onSave={saveSplit} />}
    </div>
  );
}

function Photos({
  trip,
  onUpdateTrip,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
}) {
  const [query, setQuery] = useState("");
  const [uploading, setUploading] = useState(false);
  const input = useRef<HTMLInputElement>(null);
  const uploadPhotos = async (files: FileList | null) => {
    if (!files?.length) return;
    const validFiles = Array.from(files).filter(
      (file) =>
        /^image\/(jpeg|png|webp)$/.test(file.type) &&
        file.size <= 10 * 1024 * 1024,
    );
    if (!validFiles.length) {
      window.alert("Выберите JPG, PNG или WebP до 10 МБ.");
      return;
    }
    setUploading(true);
    try {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session?.user) throw new Error("No active session");
      const uploadedPhotos = await Promise.all(
        validFiles.map(async (file) => {
          const extension = file.name.split(".").pop()?.toLowerCase() || "jpg";
          const path = `${session.user.id}/${trip.id}/album/${crypto.randomUUID()}.${extension}`;
          const { error } = await supabase.storage
            .from("trip-photos")
            .upload(path, file, {
              cacheControl: "31536000",
              contentType: file.type,
              upsert: false,
            });
          if (error) throw error;
          return {
            id: crypto.randomUUID(),
            image: await signedTripPhotoUrl(path),
            ...(await readPhotoMetadata(file)),
          };
        }),
      );
      const currentPhotos = trip.photos?.length
        ? trip.photos
        : trip.coverPhotos || [];
      onUpdateTrip({
        ...trip,
        photos: [...currentPhotos, ...uploadedPhotos],
      });
    } catch (error) {
      console.error("Could not upload trip photos.", error);
      window.alert("Не удалось загрузить фотографии. Попробуйте ещё раз.");
    } finally {
      setUploading(false);
    }
  };
  const photos = trip.photos?.length ? trip.photos : trip.coverPhotos || [];
  const visiblePhotos = photos.filter((photo) =>
    `${photo.city || ""} ${photo.date || ""} ${photo.description || ""}`
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
          <p>
            {photos.length} {photos.length === 1 ? "фото" : "фото"} · фотографии поездки
          </p>
        </div>
        <button
          className="accent"
          onClick={() => input.current?.click()}
          disabled={uploading}
        >
          {uploading ? "Загружаем…" : "↑ Загрузить"}
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
        {visiblePhotos.map((photo, index) => (
          <div
            className={`photo ${index === 0 ? "hero-photo" : `p${index % 6}`}`}
            key={photo.id}
          >
            <img
              className="photo-image"
              src={photo.image}
              alt={photo.city || "Фотография поездки"}
              loading="lazy"
            />
            <span className="photo-label">
              {photo.city || "Место не определено"}
              {photo.date ? ` · ${photo.date}` : ""}
            </span>
          </div>
        ))}
        {!visiblePhotos.length && (
          <div className="photos-empty">
            {photos.length
              ? "По вашему запросу фотографии не найдены."
              : "В поездке пока нет фотографий."}
          </div>
        )}
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
  onDeleteTrip,
  onLeaveTrip,
  onClose,
}: {
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
  onDeleteTrip: (trip: TripSummary) => Promise<void>;
  onLeaveTrip: (trip: TripSummary) => Promise<void>;
  onClose: () => void;
}) {
  const parsedDates = parseTripDateRange(trip.dates);
  const photoInput = useRef<HTMLInputElement>(null);
  const [title, setTitle] = useState(trip.title);
  const [cities, setCities] = useState(trip.cities);
  const [startDate, setStartDate] = useState(trip.startDate || parsedDates?.[0] || "");
  const [endDate, setEndDate] = useState(trip.endDate || parsedDates?.[1] || "");
  const [status, setStatus] = useState(trip.status);
  const [coverImage, setCoverImage] = useState(trip.coverImage || "");
  const [coverChanged, setCoverChanged] = useState(false);
  const [accessKind, setAccessKind] = useState<"loading" | "owner" | "collaborator">("loading");
  const [actionBusy, setActionBusy] = useState(false);
  useEffect(() => {
    let active = true;
    void supabase.auth.getSession().then(async ({ data }) => {
      if (!data.session?.user) return;
      const { data: membership, error } = await supabase
        .from("trip_collaborators")
        .select("role")
        .eq("trip_id", trip.id)
        .eq("user_id", data.session.user.id)
        .maybeSingle();
      if (!active) return;
      if (error) {
        console.error("Could not determine trip access.", error);
        return;
      }
      setAccessKind(membership ? "collaborator" : "owner");
    });
    return () => {
      active = false;
    };
  }, [trip.id]);
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
      cities: cities.trim(),
      startDate,
      endDate,
      dates: startDate && endDate ? formatTripDates(startDate, endDate) : trip.dates,
      status,
      isDraft: status === "Черновик",
      coverImage,
      coverPhotos: coverChanged
        ? [{ id: crypto.randomUUID(), image: coverImage }]
        : trip.coverPhotos,
    });
    onClose();
  };
  const remove = async () => {
    const isCollaborator = accessKind === "collaborator";
    const confirmation = isCollaborator
      ? `Выйти из путешествия «${trip.title}»? Вы потеряете к нему доступ.`
      : `Удалить путешествие «${trip.title}»? Это действие нельзя отменить.`;
    if (!window.confirm(confirmation)) {
      return;
    }
    setActionBusy(true);
    try {
      if (isCollaborator) await onLeaveTrip(trip);
      else await onDeleteTrip(trip);
      onClose();
    } catch {
      window.alert(
        isCollaborator
          ? "Не удалось выйти из путешествия. Попробуйте ещё раз."
          : "Не удалось удалить путешествие. Попробуйте ещё раз.",
      );
      setActionBusy(false);
    }
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
          <div>
            <small>Настройки поездки</small>
            <h2 id="trip-card-editor-title">{trip.title}</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Закрыть">
            ×
          </button>
        </header>
        <div className="trip-card-editor-body">
          <div className="trip-editor-summary">
            <span>R</span>
            <div>
              <b>{status === "Черновик" ? "Черновик" : status === "Завершённое" ? "Прошедшее путешествие" : "Предстоящее путешествие"}</b>
              <small>{startDate && endDate ? formatTripDates(startDate, endDate) : trip.dates}</small>
            </div>
          </div>

          <p className="trip-editor-section-label">Основное</p>
          <label>
            Название путешествия
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="Название путешествия"
            />
          </label>

          <label>
            Маршрут
            <span className="trip-editor-input-with-icon">
              <i aria-hidden="true">⌖</i>
              <input
                value={cities}
                onChange={(event) => setCities(event.target.value)}
                placeholder="Города путешествия"
              />
            </span>
          </label>

          <div className="trip-editor-cover-field">
            <p>Фото путешествия</p>
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
              onClick={() => photoInput.current?.click()}
            >
              <span
                className={coverImage ? "has-image" : ""}
                style={coverImage ? { backgroundImage: `url(${coverImage})` } : undefined}
              >
                {!coverImage && "R"}
              </span>
              <span>
                <b>{coverImage ? "Изменить фото" : "Добавить фото"}</b>
                <small>JPG, PNG или WebP</small>
              </span>
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z" />
              </svg>
            </button>
          </div>

          <div className="trip-editor-dates">
            <p>Даты поездки</p>
            <div>
              <DatePicker
                label="Начало"
                value={startDate}
                onChange={setStartDate}
              />
              <DatePicker
                label="Окончание"
                value={endDate}
                onChange={setEndDate}
              />
            </div>
          </div>

          <div className="trip-editor-status-group">
            <p>Статус</p>
            <div className="trip-editor-status">
              {[
                ["Предстоящее", "Предстоящие"],
                ["Черновик", "Черновики"],
                ["Завершённое", "Прошедшие"],
              ].map(([value, label]) => (
                <button
                  className={status === value ? "selected" : ""}
                  type="button"
                  onClick={() => setStatus(value)}
                  key={value}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <footer>
            <button type="button" onClick={onClose}>Отмена</button>
            <button className="accent" type="button" onClick={save}>Сохранить</button>
          </footer>

          {accessKind !== "loading" && (
            <button
              className={`trip-editor-delete ${accessKind === "collaborator" ? "leave" : ""}`}
              type="button"
              onClick={() => void remove()}
              disabled={actionBusy}
            >
              <span aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  {accessKind === "collaborator" ? (
                    <path d="M10 4H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h5M14 8l4 4-4 4M8 12h10" />
                  ) : (
                    <path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13M10 11v5M14 11v5" />
                  )}
                </svg>
              </span>
              <b>
                {actionBusy
                  ? accessKind === "collaborator" ? "Выходим…" : "Удаляем…"
                  : accessKind === "collaborator" ? "Выйти из путешествия" : "Удалить путешествие"}
              </b>
              <small>
                {accessKind === "collaborator"
                  ? "Доступ к поездке будет закрыт"
                  : "Удаление нельзя отменить"}
              </small>
            </button>
          )}
        </div>
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
  const [expandedPhoto, setExpandedPhoto] = useState<number | null>(null);
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
  useEffect(() => {
    if (expandedPhoto === null) return;
    const previousOverflow = document.body.style.overflow;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setExpandedPhoto(null);
      if (event.key === "ArrowLeft" && coverPhotos.length > 1)
        setExpandedPhoto(
          (current) =>
            ((current ?? 0) - 1 + coverPhotos.length) % coverPhotos.length,
        );
      if (event.key === "ArrowRight" && coverPhotos.length > 1)
        setExpandedPhoto(
          (current) => ((current ?? 0) + 1) % coverPhotos.length,
        );
    };
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [expandedPhoto, coverPhotos.length]);
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
    return signedTripPhotoUrl(path);
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
  const reorderCoverPhotos = (from: number, to: number) => {
    if (
      from === to ||
      from < 0 ||
      to < 0 ||
      from >= coverPhotos.length ||
      to >= coverPhotos.length
    ) {
      return;
    }

    const activePhotoId = activeCover?.id;
    const nextPhotos = [...coverPhotos];
    const [movedPhoto] = nextPhotos.splice(from, 1);
    if (!movedPhoto) return;
    nextPhotos.splice(to, 0, movedPhoto);

    const nextActiveIndex = activePhotoId
      ? nextPhotos.findIndex((photo) => photo.id === activePhotoId)
      : activePhoto;
    setActivePhoto(
      nextActiveIndex >= 0
        ? nextActiveIndex
        : Math.min(activePhoto, Math.max(0, nextPhotos.length - 1)),
    );
    onUpdateTrip({
      ...trip,
      coverImage: nextPhotos[0]?.image,
      coverPhotos: nextPhotos,
    });
  };
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
      <>
        <div className="trip-overview">
          <div className="overview-draft">
            <div className="cover-photo-stack">
              <section
              className={activeCover ? "has-draft-cover cover-photo-preview" : ""}
              style={
                activeCover
                  ? {
                      backgroundImage: `linear-gradient(rgba(27, 28, 31, 0.3), rgba(27, 28, 31, 0.3)), url(${activeCover.image})`,
                    }
                  : undefined
              }
              role={activeCover ? "button" : undefined}
              tabIndex={activeCover ? 0 : undefined}
              aria-label={activeCover ? "Увеличить фотографию" : undefined}
              onClick={(event) => {
                if (
                  activeCover &&
                  !(event.target as HTMLElement).closest("button, input")
                )
                  setExpandedPhoto(
                    Math.min(activePhoto, Math.max(0, coverPhotos.length - 1)),
                  );
              }}
              onKeyDown={(event) => {
                if (
                  activeCover &&
                  (event.key === "Enter" || event.key === " ")
                ) {
                  event.preventDefault();
                  setExpandedPhoto(
                    Math.min(activePhoto, Math.max(0, coverPhotos.length - 1)),
                  );
                }
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
        {expandedPhoto !== null && coverPhotos[expandedPhoto] && (
          <div
            className="accommodation-photo-lightbox cover-photo-lightbox"
            role="dialog"
            aria-modal="true"
            aria-label="Просмотр фотографии путешествия"
            onClick={() => setExpandedPhoto(null)}
          >
            <img
              src={coverPhotos[expandedPhoto].image}
              alt={coverPhotos[expandedPhoto].city || "Фотография путешествия"}
              onClick={(event) => event.stopPropagation()}
            />
            {coverPhotos.length > 1 && (
              <>
                <button
                  className="lightbox-previous"
                  type="button"
                  aria-label="Предыдущее фото"
                  onClick={(event) => {
                    event.stopPropagation();
                    setExpandedPhoto(
                      (expandedPhoto - 1 + coverPhotos.length) %
                        coverPhotos.length,
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
                    setExpandedPhoto((expandedPhoto + 1) % coverPhotos.length);
                  }}
                >
                  ›
                </button>
              </>
            )}
            <button
              className="lightbox-close"
              type="button"
              aria-label="Закрыть"
              onClick={() => setExpandedPhoto(null)}
            >
              ×
            </button>
          </div>
        )}
      </>
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
  onFocusSight,
}: {
  sights: StoredSight[];
  city?: string;
  activeSightId?: string;
  travelMode?: "walking" | "driving";
  onFocusSight?: (sight: StoredSight) => void;
}) {
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const markerElements = useRef(new globalThis.Map<string, HTMLSpanElement>());
  const browserLocation = useBrowserLocation();
  const [stats, setStats] = useState<{
    distance: number;
    duration: number;
  } | null>(null);
  const [routeCoordinates, setRouteCoordinates] = useState<
    [number, number][] | null
  >(null);
  const sightPoints = sights
    .map((sight, index) => {
      const base = sight.lnglat || mapLocation(sight.city) || (city ? mapLocation(city) : undefined);
      if (!base) return null;
      const sameCityFallbackIndex = sights
        .slice(0, index)
        .filter((item) => !item.lnglat && item.city === sight.city).length;
      const radius = sight.lnglat
        ? 0
        : sameCityFallbackIndex
          ? 0.004 + (sameCityFallbackIndex % 3) * 0.002
          : 0;
      const angle = sameCityFallbackIndex * 2.4;
      return {
        sight,
        coordinate: [
          base[0] + Math.cos(angle) * radius,
          base[1] + Math.sin(angle) * radius,
        ] as [number, number],
      };
    })
    .filter(
      (point): point is { sight: StoredSight; coordinate: [number, number] } =>
        Boolean(point),
    );
  const coordinates = sightPoints.map((point) => point.coordinate);
  const routeKey = sightPoints
    .map(({ sight, coordinate }) => `${sight.id}:${coordinate.join(",")}`)
    .join(";");
  const browserLocationKey = browserLocation.state.coordinates?.join(",") || "";

  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    if (coordinates.length < 2) return;
    let cancelled = false;
    const path = coordinates.map((coordinate) => coordinate.join(",")).join(";");
    const routeUrl = token
      ? `https://api.mapbox.com/directions/v5/mapbox/${travelMode}/${path}?geometries=geojson&overview=full&access_token=${token}`
      : `https://router.project-osrm.org/route/v1/driving/${path}?geometries=geojson&overview=full&steps=false`;
    void fetch(routeUrl)
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
    if (!container.current || (!coordinates.length && !fallbackLocation))
      return;
    let map: Map | undefined;
    let disposed = false;
    const userCoordinates = browserLocation.state.coordinates;
    void import("mapbox-gl").then(({ default: mapboxgl }) => {
      if (disposed || !container.current) return;
      if (token) mapboxgl.accessToken = token;
      map = new mapboxgl.Map({
        container: container.current,
        style: mapStyle(),
        center: userCoordinates || coordinates[0] || fallbackLocation!,
        zoom: userCoordinates ? 13 : coordinates.length ? 13 : 11,
        attributionControl: true,
      });
      mapRef.current = map;
      if (userCoordinates) {
        const element = document.createElement("span");
        element.className = "map-user-location-marker";
        element.title = "Ваше местоположение";
        new mapboxgl.Marker({ element }).setLngLat(userCoordinates).addTo(map);
      }
      markerElements.current.clear();
      map.addControl(
        new mapboxgl.NavigationControl({ showCompass: false }),
        "top-right",
      );
      sightPoints.forEach(({ sight, coordinate }, index) => {
        const marker = document.createElement("span");
        marker.className = "sight-map-marker";
        marker.textContent = String(index + 1);
        marker.addEventListener("click", () => onFocusSight?.(sight));
        markerElements.current.set(sight.id, marker);
        new mapboxgl.Marker({ element: marker })
          .setLngLat(coordinate)
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
        if (userCoordinates) {
          map!.flyTo({ center: userCoordinates, zoom: 15, duration: 700, essential: true });
        }
      });
    });
    return () => {
      disposed = true;
      map?.remove();
      mapRef.current = null;
      markerElements.current.clear();
    };
  }, [routeKey, city, routeCoordinates, onFocusSight, browserLocationKey]);
  useEffect(() => {
    if (!activeSightId) return;
    const marker = markerElements.current.get(activeSightId);
    const point = sightPoints.find(({ sight }) => sight.id === activeSightId);
    if (!marker || !point) return;
    marker.classList.remove("bounce");
    void marker.offsetWidth;
    marker.classList.add("bounce");
    mapRef.current?.flyTo({
      center: point.coordinate,
      zoom: 15,
      duration: 700,
      essential: true,
    });
  }, [activeSightId, sights]);
  useEffect(() => {
    const focusSight = (event: Event) => {
      const id = (event as CustomEvent<string>).detail;
      const marker = markerElements.current.get(id);
      const point = sightPoints.find(({ sight }) => sight.id === id);
      if (!marker || !point) return;
      marker.classList.remove("bounce");
      void marker.offsetWidth;
      marker.classList.add("bounce");
      mapRef.current?.flyTo({
        center: point.coordinate,
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
  const staticActiveIndex = activeSightId
    ? sightPoints.findIndex(({ sight }) => sight.id === activeSightId)
    : -1;
  if (!import.meta.env.VITE_MAPBOX_ACCESS_TOKEN) {
    return (
      <div className="walking-map-wrap">
        <div className="map-location-wrap">
          <StaticTripMap
            coordinates={coordinates}
            routeCoordinates={routeCoordinates || undefined}
            activeDay={staticActiveIndex >= 0 ? staticActiveIndex : undefined}
            focusIndex={staticActiveIndex >= 0 ? staticActiveIndex : undefined}
            mapClassName="walking-map"
            markerClassName="sight-map-marker"
            userLocation={browserLocation.state.coordinates}
            onMarkerClick={(index) => {
              const sight = sightPoints[index]?.sight;
              if (sight) onFocusSight?.(sight);
            }}
          />
          <BrowserLocationButton state={browserLocation.state} onRequest={browserLocation.request} />
        </div>
        <footer>
          <span>Маршрут дня · {travelMode === "driving" ? "на машине" : "пешком"}</span>
          <b>{`${sights.length} ${sights.length === 1 ? "точка" : sights.length < 5 ? "точки" : "точек"}`}</b>
        </footer>
      </div>
    );
  }
  return (
    <div className="walking-map-wrap">
      <div className="map-location-wrap">
        <div className="walking-map" ref={container} />
        <BrowserLocationButton state={browserLocation.state} onRequest={browserLocation.request} />
      </div>
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
  onDeleteDay,
}: {
  sights: StoredSight[];
  days: { id: string; title: string; photo?: string; photoPosition?: number }[];
  defaultCity?: string;
  onToggle: (id: string) => void;
  onAddDay: (title: string) => void;
  onCreateDay: (dayIndex: number, city: string, places: DayPlaceDraft[]) => void;
  onRenameDay: (id: string, title: string) => void;
  onDeleteDay: (id: string, dayIndex: number) => void;
}) {
  const [addingDay, setAddingDay] = useState(false);
  const [dayEditorOpen, setDayEditorOpen] = useState(false);
  const [routeCopied, setRouteCopied] = useState(false);
  const [selectedDay, setSelectedDay] = useState(0);
  const [activeSightId, setActiveSightId] = useState<string | null>(null);
  const [focusVersion, setFocusVersion] = useState(0);
  const [expandedSightId, setExpandedSightId] = useState<string | null>(null);
  const [expandedPhoto, setExpandedPhoto] = useState<{
    url: string;
    alt: string;
  } | null>(null);
  const [newDayCity, setNewDayCity] = useState("");
  const [editingDay, setEditingDay] = useState<{
    id: string;
    index: number;
    title: string;
  } | null>(null);
  const [editingDayTitle, setEditingDayTitle] = useState("");
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
  const cityOptions = Array.from(new Set([
    ...cities,
    ...days.map((day) => day.title),
    defaultCity || "",
  ].filter(Boolean))).sort((first, second) => first.localeCompare(second, "ru"));
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
  const shortDescriptionFor = (sight: StoredSight) => sightDescriptionFor(sight);
  const focusSight = (sight: StoredSight) => {
    setActiveSightId(sight.id);
    setFocusVersion((current) => current + 1);
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
                        setEditingDay({ id: day.id, index, title: day.title });
                        setEditingDayTitle(day.title);
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
                  const title = newDayCity.trim();
                  if (!title) return;
                  onAddDay(title);
                  setNewDayCity("");
                  setAddingDay(false);
                }}
              >
                <AccommodationCityPicker
                  value={newDayCity}
                  onChange={setNewDayCity}
                  cities={cityOptions}
                  placeholder="Например, Рим"
                  className="sights-city-picker"
                />
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
                  const tone = markerToneFor(sight);
                  const sightNumber = routeSights.findIndex((item) => item.id === sight.id) + 1;
                  const photoUrl = sight.photo || defaultSightPhotos[(sightNumber - 1) % defaultSightPhotos.length];
                  const description = shortDescriptionFor(sight);
                  const rating = sightRatingFor(sight);
                  return (
                    <article
                      className={
                        `${sight.done ? "sights-timeline-event done" : "sights-timeline-event"}${activeSightId === sight.id ? " focused" : ""}`
                      }
                      key={`${sight.id}-${activeSightId === sight.id ? focusVersion : 0}`}
                    >
                      <span className={`sights-timeline-marker ${tone}`} aria-hidden="true">
                        {sightNumber}
                      </span>
                      <div
                        className="sights-timeline-card has-photo"
                        onClick={(event) => {
                          const target = event.target as HTMLElement;
                          if (target.closest("button, input, label, a")) return;
                          focusSight(sight);
                        }}
                      >
                        <button
                          type="button"
                          className="sights-event-photo-button"
                          aria-label={`Увеличить фото: ${sight.name}`}
                          onClick={() => {
                            focusSight(sight);
                            setExpandedPhoto({
                              url: photoUrl,
                              alt: sight.name,
                            });
                          }}
                        >
                          <img
                            className="sights-event-thumb"
                            src={photoUrl}
                            alt=""
                            loading="lazy"
                          />
                        </button>
                        <div className="sights-timeline-card-content">
                          <small className="sights-event-category">{categoryFor(sight)}</small>
                          <div className="sights-event-top">
                            <button
                              type="button"
                              className="sights-event-name"
                              onClick={() => focusSight(sight)}
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
                          <div className="sights-event-rating" aria-label={`Рейтинг ${rating.score.toFixed(1)} из 5, ${formatSightReviews(rating.reviews)} оценок`}>
                            <span>★</span> {rating.score.toFixed(1)} <small>· {formatSightReviews(rating.reviews)} оценок</small>
                          </div>
                          <p className={`sights-event-description${expandedSightId === sight.id ? " expanded" : ""}`}>
                            {expandedSightId === sight.id && sight.description?.trim()
                              ? sight.description.trim()
                              : description}
                          </p>
                          <div className="sights-event-meta">
                            {sight.duration && <span>{sight.duration}</span>}
                            {sight.city !== city && <span>· {sight.city}</span>}
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
              onFocusSight={focusSight}
              travelMode={
                routeSights.some((sight) => sight.id.startsWith("stelvio_"))
                  ? "driving"
                  : "walking"
              }
            />
          </aside>
        </div>
      </section>
      {editingDay && (
        <div
          className="sights-day-dialog-backdrop"
          onClick={() => setEditingDay(null)}
        >
          <form
            className="sights-day-dialog"
            onClick={(event) => event.stopPropagation()}
            onSubmit={(event) => {
              event.preventDefault();
              const title = editingDayTitle.trim();
              if (!title) return;
              onRenameDay(editingDay.id, title);
              setEditingDay(null);
            }}
          >
            <header>
              <div>
                <small>ДЕНЬ {editingDay.index + 1}</small>
                <h2>Настройки дня</h2>
              </div>
              <button
                type="button"
                aria-label="Закрыть"
                onClick={() => setEditingDay(null)}
              >
                ×
              </button>
            </header>
            <label>
              Название дня
              <input
                value={editingDayTitle}
                onChange={(event) => setEditingDayTitle(event.target.value)}
                autoFocus
              />
            </label>
            <footer>
              <button
                type="button"
                className="sights-day-dialog-delete"
                onClick={() => {
                  if (days.length <= 1) {
                    window.alert("Нельзя удалить последний день путешествия.");
                    return;
                  }
                  onDeleteDay(editingDay.id, editingDay.index);
                  setEditingDay(null);
                }}
              >
                Удалить день
              </button>
              <button type="button" onClick={() => setEditingDay(null)}>
                Отмена
              </button>
              <button className="accent" type="submit">
                Сохранить
              </button>
            </footer>
          </form>
        </div>
      )}
      {dayEditorOpen && (
        <DayEditor
          dayNumber={selectedDay + 1}
          defaultCity=""
          cities={cityOptions}
          onClose={() => setDayEditorOpen(false)}
          onSave={(nextCity, places) => {
            onCreateDay(selectedDay, nextCity, places);
            setDayEditorOpen(false);
          }}
        />
      )}
      {expandedPhoto && (
        <div
          className="accommodation-photo-lightbox sights-photo-lightbox"
          role="dialog"
          aria-modal="true"
          aria-label={`Просмотр фото: ${expandedPhoto.alt}`}
          onClick={() => setExpandedPhoto(null)}
        >
          <img
            src={expandedPhoto.url}
            alt={expandedPhoto.alt}
            onClick={(event) => event.stopPropagation()}
          />
          <button
            className="lightbox-close"
            type="button"
            aria-label="Закрыть"
            onClick={() => setExpandedPhoto(null)}
          >
            ×
          </button>
        </div>
      )}
    </>
  );
}

function DayEditor({
  dayNumber,
  defaultCity,
  cities = accommodationCities,
  onClose,
  onSave,
}: {
  dayNumber: number;
  defaultCity: string;
  cities?: string[];
  onClose: () => void;
  onSave: (city: string, places: DayPlaceDraft[]) => void;
}) {
  const [city, setCity] = useState(defaultCity);
  const [places, setPlaces] = useState<DayPlaceDraft[]>([]);
  const [place, setPlace] = useState("");
  const [placeDescription, setPlaceDescription] = useState("");
  const [placePhoto, setPlacePhoto] = useState<string>();
  const [placePhotoFile, setPlacePhotoFile] = useState<File | null>(null);
  const [placePhotoPosition, setPlacePhotoPosition] = useState(50);
  const [draggingPlacePhoto, setDraggingPlacePhoto] = useState(false);
  const placePhotoDrag = useRef<{ y: number; position: number } | null>(null);
  const [uploadingPlacePhoto, setUploadingPlacePhoto] = useState(false);
  const [catalogOpen, setCatalogOpen] = useState(false);
  const [catalogQuery, setCatalogQuery] = useState("");
  const [remoteCatalog, setRemoteCatalog] = useState<StoredSight[]>([]);
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [catalogError, setCatalogError] = useState("");
  useEffect(() => {
    const searchCity = city.trim();
    if (!catalogOpen || searchCity.length < 2) {
      setRemoteCatalog([]);
      setCatalogLoading(false);
      setCatalogError("");
      return;
    }
    const controller = new AbortController();
    setCatalogLoading(true);
    setCatalogError("");
    void fetchSightCatalog(searchCity, controller.signal)
      .then((items) => {
        setRemoteCatalog(items);
        void enrichSightCatalogPhotos(items, controller.signal)
          .then((enrichedItems) => {
            if (controller.signal.aborted || enrichedItems === items) return;
            const byId = new globalThis.Map(enrichedItems.map((item) => [item.id, item]));
            setRemoteCatalog((current) => current.map((item) => byId.get(item.id) || item));
          })
          .catch(() => {
            // Photos are optional enrichment; the catalog remains usable.
          });
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === "AbortError") return;
        setRemoteCatalog([]);
        setCatalogError("Внешний каталог временно недоступен");
      })
      .finally(() => {
        if (!controller.signal.aborted) setCatalogLoading(false);
      });
    return () => controller.abort();
  }, [catalogOpen, city]);
  const cityCatalog = attractionCatalog.filter(
    (sight) =>
      !city ||
      sight.city.toLowerCase().includes(city.toLowerCase()) ||
      city.toLowerCase().includes(sight.city.toLowerCase()),
  );
  const localCatalog = cityCatalog.length ? cityCatalog : attractionCatalog;
  const catalogItems = Array.from(
    new globalThis.Map(
      [...remoteCatalog, ...localCatalog].map((item) => [item.name.toLowerCase(), item]),
    ).values(),
  )
    .filter((sight) => {
      const query = catalogQuery.trim().toLowerCase();
      return (
        !query ||
        `${sight.name} ${sight.city} ${sightDescriptionFor(sight)}`
          .toLowerCase()
          .includes(query)
      );
    })
    .slice(0, 24);
  const addCatalogPlace = (sight: StoredSight, index: number) => {
    setPlaces((current) => {
      if (current.some((item) => item.name === sight.name)) return current;
      return [
        ...current,
        {
          name: sight.name,
          subcategory: sight.subcategory || sight.group || "Достопримечательность",
          description: sightDescriptionFor(sight),
          photo: catalogPhotoFor(sight, index),
          photoPosition: sight.photoPosition,
          lnglat: sight.lnglat,
          googleRating: sight.googleRating,
          googleReviews: sight.googleReviews,
        },
      ];
    });
  };
  const addPlace = async () => {
    const value = place.trim();
    if (!value) return;
    const uploadedPhoto = placePhotoFile
      ? await uploadPlacePhoto(placePhotoFile)
      : placePhoto;
    if (placePhotoFile && !uploadedPhoto) return;
    setPlaces((current) => [...current, { name: value, description: placeDescription.trim() || undefined, photo: uploadedPhoto || undefined, photoPosition: placePhotoPosition }]);
    setPlace("");
    setPlaceDescription("");
    setPlacePhoto(undefined);
    setPlacePhotoFile(null);
    setPlacePhotoPosition(50);
  };
  const uploadPlacePhoto = async (file: File) => {
    if (!file.type.match(/^image\/(jpeg|png|webp)$/) || file.size > 10 * 1024 * 1024) {
      window.alert("Выберите JPG, PNG или WebP до 10 МБ.");
      return;
    }
    setUploadingPlacePhoto(true);
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
      return await signedTripPhotoUrl(path);
    } catch {
      window.alert("Не удалось загрузить фото. Попробуйте ещё раз.");
      return null;
    } finally {
      setUploadingPlacePhoto(false);
    }
  };
  return (
    <div className="day-editor-backdrop" onClick={onClose}>
      <form
        className="day-editor"
        onClick={(event) => event.stopPropagation()}
        onSubmit={async (event) => {
          event.preventDefault();
          const nextCity = city.trim();
          if (!nextCity) return;
          onSave(nextCity, places);
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
          <AccommodationCityPicker
            value={city}
            onChange={setCity}
            cities={cities}
            placeholder="Напр. Болонья"
            className="day-editor-city-picker"
          />
        </label>
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
            <input value={placeDescription} onChange={(event) => setPlaceDescription(event.target.value)} placeholder="Короткое описание" />
            <label className="day-place-photo-upload">
              <input type="file" accept="image/jpeg,image/png,image/webp" aria-label="Фото места" onChange={(event) => { const file = event.target.files?.[0] || null; setPlacePhotoFile(file); setPlacePhoto(file ? URL.createObjectURL(file) : undefined); }} />
              {placePhoto ? <img className={draggingPlacePhoto ? "dragging" : ""} src={placePhoto} alt="Фото места" style={{ objectPosition: `center ${placePhotoPosition}%` }} onPointerDown={(event) => { event.currentTarget.setPointerCapture(event.pointerId); placePhotoDrag.current = { y: event.clientY, position: placePhotoPosition }; setDraggingPlacePhoto(true); }} onPointerMove={(event) => { const start = placePhotoDrag.current; if (!start) return; const height = event.currentTarget.getBoundingClientRect().height; const next = start.position - ((event.clientY - start.y) / height) * 100; setPlacePhotoPosition(Math.max(0, Math.min(100, next))); }} onPointerUp={() => { placePhotoDrag.current = null; setDraggingPlacePhoto(false); }} onPointerCancel={() => { placePhotoDrag.current = null; setDraggingPlacePhoto(false); }} /> : <span>＋ Добавить фото</span>}
              {placePhoto && <small>Перетащите фото, чтобы выбрать кадр</small>}
            </label>
          </div>
          <div className="day-place-catalog">
            <button
              type="button"
              className="day-place-catalog-toggle"
              onClick={() => setCatalogOpen((open) => !open)}
            >
              {catalogOpen ? "Скрыть каталог" : "＋ Выбрать из каталога"}
            </button>
            {catalogOpen && (
              <div className="day-place-catalog-panel">
                <input
                  value={catalogQuery}
                  onChange={(event) => setCatalogQuery(event.target.value)}
                  placeholder="Поиск достопримечательности..."
                  aria-label="Поиск в каталоге"
                />
                {catalogLoading && <p className="day-place-catalog-status">Загружаем места для города…</p>}
                {catalogError && <p className="day-place-catalog-status error">{catalogError}. Показываем сохранённый каталог.</p>}
                <div className="day-place-catalog-list">
                  {catalogItems.map((item, index) => {
                    const added = places.some((place) => place.name === item.name);
                    const rating = sightRatingFor(item);
                    return (
                      <button
                        type="button"
                        className={added ? "added" : ""}
                        disabled={added}
                        onClick={() => addCatalogPlace(item, index)}
                        key={item.id}
                      >
                        <img
                          src={catalogPhotoFor(item, index)}
                          alt=""
                          loading="lazy"
                        />
                        <span>
                          <small className="catalog-place-category">
                            {item.subcategory || item.group || "Достопримечательность"}
                          </small>
                          <b>{item.name}</b>
                          <small className="catalog-place-rating">
                            <span>★</span> {rating.score.toFixed(1)} · {formatSightReviews(rating.reviews)} оценок
                          </small>
                          <small>
                            {sightDescriptionFor(item)}
                          </small>
                        </span>
                        <i>{added ? "Добавлено" : "＋"}</i>
                      </button>
                    );
                  })}
                  {!catalogItems.length && !catalogLoading && <p>Ничего не найдено.</p>}
                </div>
                <small className="day-place-catalog-source">Google Places: фото, рейтинг, отзывы и точки на карте. Wikipedia используется как резервный источник.</small>
              </div>
            )}
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
          <button className="accent" disabled={uploadingPlacePhoto}>Сохранить день</button>
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

const petCatalogSeeds: Omit<PetPlace, "id" | "city" | "address">[] = [
  {
    name: "Fressnapf",
    type: "shop",
    rating: 4.5,
    reviewCount: 475,
    photoUrl: "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&w=640&q=80",
    note: "Корм, аксессуары и всё для путешествий с питомцем.",
    distanceKm: 2,
    openNow: true,
  },
  {
    name: "Tierarztzentrum am Stadtpark",
    type: "vet",
    rating: 4.8,
    reviewCount: 308,
    photoUrl: "https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?auto=format&fit=crop&w=640&q=80",
    note: "Ветеринарная клиника с экстренной помощью.",
    distanceKm: 5,
    openNow: true,
    is24h: true,
  },
  {
    name: "Zoo & Co.",
    type: "shop",
    rating: 4.4,
    reviewCount: 382,
    photoUrl: "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=640&q=80",
    note: "Зоомагазин рядом с центром города.",
    distanceKm: 10,
    openNow: false,
  },
  {
    name: "AniCura Veterinary Clinic",
    type: "vet",
    rating: 4.6,
    reviewCount: 242,
    photoUrl: "https://images.unsplash.com/photo-1556760544-74068565f05c?auto=format&fit=crop&w=640&q=80",
    note: "Приём по записи и консультации для собак и кошек.",
    distanceKm: 25,
    openNow: true,
  },
];

function tripPetCities(trip: TripSummary) {
  const routeCities = (trip.days || []).flatMap((day) =>
    day.roadLeg ? [day.roadLeg.from, day.roadLeg.to] : [],
  );
  return Array.from(
    new Set(
      [...routeCities, ...(trip.cities || "").split(/[·,]/)]
        .map((city) => city.trim())
        .filter(Boolean),
    ),
  );
}

function petCatalogForCities(cities: string[]): PetPlace[] {
  return cities.slice(0, 8).flatMap((city, cityIndex) =>
    petCatalogSeeds.map((seed, index) => ({
      ...seed,
      id: `pet-catalog-${cityIndex}-${index}`,
      city,
      address: `${index % 2 === 0 ? "Hauptstraße 18" : "Stadtpark 4"}, ${city}`,
      mapsUrl: `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${seed.name}, ${city}`)}`,
    })),
  );
}

function PetPlaceForm({
  initial,
  defaultCity,
  tripId,
  onClose,
  onSave,
}: {
  initial?: PetPlace;
  defaultCity: string;
  tripId: string;
  onClose: () => void;
  onSave: (place: PetPlace) => void;
}) {
  const [name, setName] = useState(initial?.name || "");
  const [city, setCity] = useState(initial?.city || defaultCity);
  const [type, setType] = useState<PetPlace["type"]>(initial?.type || "shop");
  const [address, setAddress] = useState(initial?.address || "");
  const [note, setNote] = useState(initial?.note || "");
  const [photoUrl, setPhotoUrl] = useState(initial?.photoUrl || "");
  const [uploadingPhoto, setUploadingPhoto] = useState(false);
  const photoInputRef = useRef<HTMLInputElement>(null);
  const uploadPhoto = async (file: File | undefined) => {
    if (!file) return;
    if (!file.type.match(/^image\/(jpeg|png|webp)$/) || file.size > 10 * 1024 * 1024) {
      window.alert("Выберите JPG, PNG или WebP до 10 МБ.");
      return;
    }
    setUploadingPhoto(true);
    try {
      const {
        data: { session },
      } = await supabase.auth.getSession();
      if (!session) throw new Error("No active session");
      const extension = file.name.split(".").pop()?.toLowerCase() || "jpg";
      const path = `${session.user.id}/${tripId}/pets/${crypto.randomUUID()}.${extension}`;
      const { error } = await supabase.storage.from("trip-photos").upload(path, file, {
        cacheControl: "31536000",
        contentType: file.type,
        upsert: false,
      });
      if (error) throw error;
      setPhotoUrl(await signedTripPhotoUrl(path));
    } catch {
      window.alert("Не удалось загрузить фотографию. Попробуйте ещё раз.");
    } finally {
      setUploadingPhoto(false);
    }
  };
  return (
    <div className="pets-form-backdrop" onClick={onClose}>
      <form className="pets-form-modal" onClick={(event) => event.stopPropagation()} onSubmit={(event) => {
        event.preventDefault();
        if (!name.trim() || !city.trim()) return;
        onSave({
          id: initial?.id || crypto.randomUUID(),
          name: name.trim(),
          city: city.trim(),
          type,
          address: address.trim(),
          note: note.trim(),
          photoUrl: photoUrl || undefined,
          photoName: initial?.photoName,
          googlePlaceId: initial?.googlePlaceId,
          rating: initial?.rating,
          reviewCount: initial?.reviewCount,
          mapsUrl: initial?.mapsUrl,
          latitude: initial?.latitude,
          longitude: initial?.longitude,
          distanceKm: initial?.distanceKm,
          openNow: initial?.openNow,
          is24h: initial?.is24h,
        });
      }}>
        <header><div><small>ПИТОМЦЫ</small><h2>{initial ? "Редактировать место" : "Добавить место"}</h2></div><button type="button" onClick={onClose}>×</button></header>
        <label>Название<input value={name} onChange={(event) => setName(event.target.value)} placeholder="Например, Fressnapf" autoFocus /></label>
        <div className="pets-form-grid"><label>Город<input value={city} onChange={(event) => setCity(event.target.value)} placeholder="Город" /></label><label>Адрес<input value={address} onChange={(event) => setAddress(event.target.value)} placeholder="Адрес" /></label></div>
        <section><b>Тип места</b><div className="pets-choice-row"><button type="button" className={type === "shop" ? "active" : ""} onClick={() => setType("shop")}>Зоомагазин</button><button type="button" className={type === "vet" ? "active" : ""} onClick={() => setType("vet")}>Ветеринар</button></div></section>
        <label>Описание<textarea value={note} onChange={(event) => setNote(event.target.value)} placeholder="Что важно знать" rows={3} /></label>
        <section className="pets-form-photo-section"><b>Фото места</b><div className="pets-photo-picker-row"><label className={`pets-photo-picker ${photoUrl ? "has-photo" : ""}`}>
          <input ref={photoInputRef} type="file" accept="image/jpeg,image/png,image/webp" disabled={uploadingPhoto} onChange={(event) => { void uploadPhoto(event.target.files?.[0]); event.target.value = ""; }} />
          {photoUrl ? <img src={photoUrl} alt="Фото места" /> : <span className="pets-photo-placeholder"><span>▧</span><b>Добавить фото</b><small>JPG, PNG или WebP до 10 МБ</small></span>}
        </label>{photoUrl && <button type="button" className="pets-photo-remove" onClick={() => setPhotoUrl("")} disabled={uploadingPhoto}>Удалить фото</button>}</div>{uploadingPhoto && <small className="pets-photo-uploading">Загружаем фото…</small>}</section>
        <footer><button type="button" onClick={onClose} disabled={uploadingPhoto}>Отмена</button><button className="accent" type="submit" disabled={uploadingPhoto}>{uploadingPhoto ? "Загружаем…" : "Сохранить"}</button></footer>
      </form>
    </div>
  );
}

function Pets({ trip, onUpdateTrip }: { trip: TripSummary; onUpdateTrip: (trip: TripSummary) => void }) {
  const cities = tripPetCities(trip);
  const cityOptions = ["Все города", ...cities];
  const [selectedCity, setSelectedCity] = useState("Все города");
  const [selectedType, setSelectedType] = useState<PetPlace["type"]>("shop");
  const [query, setQuery] = useState("");
  const [filterOpen, setFilterOpen] = useState(false);
  const [radius, setRadius] = useState("10");
  const [minRating, setMinRating] = useState("");
  const [openNow, setOpenNow] = useState(false);
  const [aroundTheClock, setAroundTheClock] = useState(false);
  const [manualOpen, setManualOpen] = useState(false);
  const [editing, setEditing] = useState<PetPlace | undefined>();
  const [preview, setPreview] = useState<{ url: string; name: string } | null>(null);
  const [liveCatalog, setLiveCatalog] = useState<PetPlace[] | null>(null);
  const [catalogLoading, setCatalogLoading] = useState(false);
  const saved = trip.petPlaces || [];
  const fallbackCatalog = petCatalogForCities(cities.length ? cities : ["Рим"]);
  const catalogPending = liveCatalog === null;
  const catalog = catalogPending ? [] : liveCatalog.length ? liveCatalog : fallbackCatalog;
  const filterCount = Number(radius !== "10") + Number(Boolean(minRating)) + Number(openNow) + Number(aroundTheClock);
  useEffect(() => {
    const controller = new AbortController();
    const citiesToSearch = selectedCity === "Все города"
      ? (cities.length ? cities : ["Рим"]).slice(0, 6)
      : [selectedCity];
    setLiveCatalog(null);
    const timeout = window.setTimeout(() => {
      setCatalogLoading(true);
      void Promise.all(
        citiesToSearch.map((city) =>
          fetchGooglePetCatalog(city, selectedType, query, controller.signal).catch(() => []),
        ),
      )
        .then(async (groups) => {
          const places = groups.flat();
          return enrichPetCatalogPhotos(places, controller.signal);
        })
        .then((places) => {
          if (!controller.signal.aborted) setLiveCatalog(places);
        })
        .catch(() => {
          if (!controller.signal.aborted) setLiveCatalog([]);
        })
        .finally(() => {
          if (!controller.signal.aborted) setCatalogLoading(false);
        });
    }, 360);
    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [selectedCity, selectedType, query, cities.join("|")]);
  const matches = (place: PetPlace) => {
    const haystack = `${place.name} ${place.city} ${place.address} ${place.note || ""}`.toLocaleLowerCase("ru-RU");
    return place.type === selectedType &&
      (selectedCity === "Все города" || place.city === selectedCity) &&
      (!query.trim() || haystack.includes(query.trim().toLocaleLowerCase("ru-RU"))) &&
      (!minRating || (place.rating || 0) >= Number(minRating)) &&
      (!radius || place.distanceKm === undefined || place.distanceKm <= Number(radius)) &&
      (!openNow || place.openNow === true) &&
      (!aroundTheClock || place.is24h === true);
  };
  const visibleSaved = saved.filter(matches);
  const visibleCatalog = catalog.filter((place) => !saved.some((item) => item.id === place.id || (item.name === place.name && item.city === place.city))).filter(matches);
  const savePlace = (place: PetPlace) => {
    const next = saved.some((item) => item.id === place.id) ? saved.map((item) => item.id === place.id ? place : item) : [...saved, place];
    onUpdateTrip({ ...trip, petPlaces: next });
    setManualOpen(false);
    setEditing(undefined);
  };
  const addCatalogPlace = (place: PetPlace) => savePlace({ ...place, id: crypto.randomUUID() });
  return (
    <section className="pets-page">
      <div className="pets-heading"><div><p className="eyebrow">ПО МАРШРУТУ</p><h1>Питомцы</h1><span>Места для заботы о питомцах в городах поездки</span></div><button className="accent" type="button" onClick={() => { setEditing(undefined); setManualOpen(true); }}>＋ Добавить место</button></div>
      <div className="pets-toolbar"><label className="pets-city-field"><span>Город</span><select value={selectedCity} onChange={(event) => setSelectedCity(event.target.value)}>{cityOptions.map((city) => <option key={city}>{city}</option>)}</select></label><label className="pets-search"><span>Поиск по каталогу</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Название, адрес или город" /></label><button className="pets-filter-button" type="button" aria-expanded={filterOpen} onClick={() => setFilterOpen((open) => !open)}>☷ Фильтры <b>{filterCount}</b></button></div>
      {filterOpen && <div className="pets-filter-panel"><div className="pets-filter-panel-head"><h3>Фильтры</h3><button type="button" className="pets-filter-reset" onClick={() => { setRadius("10"); setMinRating(""); setOpenNow(false); setAroundTheClock(false); }}>Сбросить</button></div><div className="pets-filter-group"><b>Радиус поиска</b><div className="pets-choice-row">{["1", "5", "10", "25"].map((value) => <button type="button" className={radius === value ? "active" : ""} onClick={() => setRadius(value)} key={value}>{value} км</button>)}</div></div><div className="pets-filter-group"><b>Рейтинг от</b><div className="pets-choice-row"><button type="button" className={!minRating ? "active" : ""} onClick={() => setMinRating("")}>Любой</button>{["4.0", "4.5", "4.8"].map((value) => <button type="button" className={minRating === value ? "active" : ""} onClick={() => setMinRating(value)} key={value}>★ {value}</button>)}</div></div><div className="pets-filter-group"><b>Дополнительно</b><div className="pets-choice-row"><button type="button" className={openNow ? "active" : ""} onClick={() => setOpenNow((value) => !value)}>Открыто сейчас</button><button type="button" className={aroundTheClock ? "active" : ""} onClick={() => setAroundTheClock((value) => !value)}>Круглосуточно</button></div></div></div>}
      <div className="pets-type-tabs"><button type="button" className={selectedType === "shop" ? "active" : ""} onClick={() => setSelectedType("shop")}>Зоомагазины</button><button type="button" className={selectedType === "vet" ? "active" : ""} onClick={() => setSelectedType("vet")}>Ветеринары</button></div>
      {visibleSaved.length > 0 && <><h2 className="pets-section-title">Мои места</h2><div className="pets-grid">{visibleSaved.map((place) => <PetCard key={place.id} place={place} saved onPhoto={(url) => setPreview({ url, name: place.name })} onEdit={() => { setEditing(place); setManualOpen(true); }} onDelete={() => onUpdateTrip({ ...trip, petPlaces: saved.filter((item) => item.id !== place.id) })} />)}</div></>}
      <div className="pets-section-title-row"><div><h2 className="pets-section-title">Из каталога</h2><small className="pets-catalog-source">{catalogPending || catalogLoading ? "Загружаем Google Places…" : liveCatalog?.length ? "Фото, рейтинг и ссылки из Google Maps" : "Каталог временно работает в резервном режиме"}</small></div><span>{catalogPending ? "Загрузка…" : `${visibleCatalog.length} мест`}</span></div><div className="pets-grid">{catalogPending ? <PetCatalogSkeleton /> : visibleCatalog.map((place) => <PetCard key={place.id} place={place} onPhoto={(url) => setPreview({ url, name: place.name })} onAdd={() => addCatalogPlace(place)} />)}</div>
      {!catalogPending && !visibleSaved.length && !visibleCatalog.length && <div className="pets-empty">Ничего не найдено. Попробуйте другой город или запрос.</div>}
      {manualOpen && <PetPlaceForm initial={editing} tripId={trip.id} defaultCity={selectedCity === "Все города" ? cities[0] || "Рим" : selectedCity} onClose={() => { setManualOpen(false); setEditing(undefined); }} onSave={savePlace} />}
      {preview && <div className="pets-photo-backdrop" onClick={() => setPreview(null)}><img src={preview.url} alt={preview.name} /><button type="button" onClick={() => setPreview(null)}>×</button></div>}
    </section>
  );
}

function PetCatalogSkeleton({ count = 4 }: { count?: number }) {
  return <>{Array.from({ length: count }, (_, index) => <article className="pets-card pets-card-skeleton" aria-hidden="true" key={index}><div className="pets-skeleton-photo" /><div className="pets-skeleton-body"><span /><b /><i /><em /><small /></div></article>)}</>;
}

function PetCard({ place, saved = false, onPhoto, onAdd, onEdit, onDelete }: { place: PetPlace; saved?: boolean; onPhoto: (url: string) => void; onAdd?: () => void; onEdit?: () => void; onDelete?: () => void }) {
  const mapsUrl = place.mapsUrl || `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${place.name}, ${place.address || place.city}`)}`;
  return <article className="pets-card"><button className="pets-card-photo" type="button" onClick={() => place.photoUrl && onPhoto(place.photoUrl)} disabled={!place.photoUrl}>{place.photoUrl ? <img src={place.photoUrl} alt="" loading="lazy" decoding="async" /> : <span>♡</span>}</button><div className="pets-card-body"><small>{place.type === "vet" ? "ВЕТЕРИНАРНАЯ КЛИНИКА" : "ЗООМАГАЗИН"}</small><h3>{place.name}</h3><span className="pets-card-city">{place.city}</span>{place.rating !== undefined && <div className="pets-rating">★ <b>{place.rating.toFixed(1)}</b>{place.reviewCount ? <span>({place.reviewCount})</span> : null}</div>}<p>{place.address}</p><small className="pets-card-note">{place.note}</small><div className="pets-card-actions"><a href={mapsUrl} target="_blank" rel="noreferrer">↗ Google Карты</a>{saved ? <><button type="button" className="pets-edit-button" onClick={onEdit}>Изменить</button><button type="button" className="pets-delete-button" onClick={onDelete}>Удалить</button></> : <button type="button" className="pets-add-button" onClick={onAdd}>Добавить</button>}</div></div></article>;
}

function Workspace({
  go,
  trip,
  onUpdateTrip,
  tab,
  onTabChange,
  darkTheme = false,
}: {
  go: (view: View) => void;
  trip: TripSummary;
  onUpdateTrip: (trip: TripSummary) => void;
  tab: Tab;
  onTabChange: (tab: Tab) => void;
  darkTheme?: boolean;
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
        ["pets", "Питомцы"],
        ["members", "Участники"],
        ["photos", "Фото"],
      ]
    : [
        ["overview", "Главная"],
        ["route", "Маршрут"],
        ["accommodation", "Жильё"],
        ["bookings", "Транспорт и билеты"],
        ["budget", "Бюджет"],
        ["pets", "Питомцы"],
        ["members", "Участники"],
        ["photos", "Фото"],
      ];
  return (
    <div className={`trip-shell${darkTheme ? " theme-dark" : ""}`}>
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
            onDeleteDraftDay={(day) => {
              if (!window.confirm("Удалить этот день маршрута?")) return;
              setEditingRoadDay(null);
              onUpdateTrip({
                ...trip,
                places: undefined,
                days: draftDays.filter((_, index) => index !== day),
              });
            }}
            onReorderDraftDays={(from, to) => {
              if (from === to) return;
              const nextDays = [...draftDays];
              const [movedDay] = nextDays.splice(from, 1);
              if (!movedDay) return;
              nextDays.splice(to, 0, movedDay);
              setEditingRoadDay(null);
              onUpdateTrip({
                ...trip,
                places: undefined,
                days: nextDays,
              });
            }}
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
              onCreateDay={(dayIndex, city, places) => {
                const dayNumber = dayIndex + 1;
                const newSights = places
                  .filter((place) => place.name)
                  .map((place, index) => ({
                    id: crypto.randomUUID(),
                    ...place,
                    city,
                    // Catalog places keep their exact coordinates; custom places
                    // are placed automatically at the selected city's center.
                    lnglat: place.lnglat || mapLocation(city),
                    walkDay: dayNumber,
                    walkOrder: index,
                  }));
                onUpdateTrip({
                  ...trip,
                  sightDaysVersion: 1,
                  sightDays: sightDays.map((day, index) =>
                    index === dayIndex ? { ...day, title: city } : day,
                  ),
                  sights: [...trip.sights || [], ...newSights],
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
              onDeleteDay={(id, dayIndex) => {
                if (sightDays.length <= 1) return;
                const removedDayNumber = dayIndex + 1;
                const nextSightNotes = { ...trip.sightNotes };
                delete nextSightNotes[id];
                onUpdateTrip({
                  ...trip,
                  sightDaysVersion: 1,
                  sightDays: sightDays.filter((day) => day.id !== id),
                  sights: (trip.sights || [])
                    .filter((sight) => sight.walkDay !== removedDayNumber)
                    .map((sight) =>
                      sight.walkDay && sight.walkDay > removedDayNumber
                        ? { ...sight, walkDay: sight.walkDay - 1 }
                        : sight,
                    ),
                  sightNotes: nextSightNotes,
                });
              }}
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
        {tab === "pets" && <Pets trip={trip} onUpdateTrip={onUpdateTrip} />}
        {tab === "photos" && (
          <Photos trip={trip} onUpdateTrip={onUpdateTrip} />
        )}
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

function PasswordField({ className = "", ...inputProps }: InputHTMLAttributes<HTMLInputElement>) {
  const [visible, setVisible] = useState(false);
  return (
    <span className={`password-field${className ? ` ${className}` : ""}`}>
      <input {...inputProps} type={visible ? "text" : "password"} />
      <button
        type="button"
        className="password-visibility"
        aria-label={visible ? "Скрыть пароль" : "Показать пароль"}
        aria-pressed={visible}
        onClick={() => setVisible((current) => !current)}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" />
          <circle cx="12" cy="12" r="2.8" />
          {!visible && <path className="password-visibility-slash" d="m4 4 16 16" />}
        </svg>
      </button>
    </span>
  );
}

function Auth({
  go,
  onAuthorized,
  rememberedAccounts,
  onRememberedAccount,
  inviteSetup = false,
  inviteNextPath,
  onInviteComplete,
}: {
  go: (view: View) => void;
  onAuthorized: (name: string) => void;
  rememberedAccounts: RememberedAccount[];
  onRememberedAccount: (account: RememberedAccount) => void;
  inviteSetup?: boolean;
  inviteNextPath?: string;
  onInviteComplete?: (nextPath?: string) => void;
}) {
  const [mode, setMode] = useState<"register" | "login">("register");
  const [message, setMessage] = useState("");
  const [rememberMe, setRememberMe] = useState(true);
  const [email, setEmail] = useState("");
  const [selectedAccount, setSelectedAccount] = useState<RememberedAccount | null>(null);
  const [accountListOpen, setAccountListOpen] = useState(false);
  const [manualEmail, setManualEmail] = useState(true);
  const isRegister = mode === "register";
  const canUseRememberedAccount = !isRegister && rememberedAccounts.length > 0 && !manualEmail;
  const accountInitial = (account: RememberedAccount) =>
    account.name.trim().charAt(0).toLocaleUpperCase() || account.email.charAt(0).toLocaleUpperCase();
  const enterRegistration = () => {
    setMode("register");
    setMessage("");
    setAccountListOpen(false);
    setManualEmail(true);
    setSelectedAccount(null);
    setEmail("");
  };
  const enterLogin = () => {
    const firstAccount = rememberedAccounts[0] || null;
    setMode("login");
    setMessage("");
    setSelectedAccount(firstAccount);
    setEmail(firstAccount?.email || "");
    setManualEmail(!firstAccount);
    setAccountListOpen(Boolean(firstAccount));
  };
  useEffect(() => {
    if (!inviteSetup) return;
    void supabase.auth.getUser().then(({ data }) => {
      const invitedEmail = data.user?.email || "";
      if (invitedEmail) setEmail(invitedEmail);
    });
  }, [inviteSetup]);
  const chooseRememberedAccount = (account: RememberedAccount) => {
    setSelectedAccount(account);
    setEmail(account.email);
    setManualEmail(false);
    setAccountListOpen(false);
    setMessage("");
  };
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

    if (inviteSetup) {
      setAuthSessionPersistence(true);
      const { data, error } = await supabase.auth.updateUser({
        password,
        data: { full_name: name, invite_pending: false },
      });
      if (error || !data.user) {
        setMessage(error?.message || "Не удалось завершить регистрацию.");
        return;
      }
      onAuthorized(
        data.user.user_metadata.full_name || data.user.email || name,
      );
      onRememberedAccount({
        email: data.user.email || email,
        name: data.user.user_metadata.full_name || name,
      });
      setMessage("Регистрация завершена. Открываем поездку...");
      window.setTimeout(() => onInviteComplete?.(inviteNextPath), 350);
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
      onRememberedAccount({ email, name });
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
    if (rememberMe) {
      onRememberedAccount({
        email,
        name: data.user.user_metadata.full_name || data.user.email || "Путешественник",
      });
    }
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
          {!inviteSetup && <div className="auth-switch">
            <button
              className={isRegister ? "active" : ""}
              onClick={enterRegistration}
            >
              Регистрация
            </button>
            <button
              className={!isRegister ? "active" : ""}
              onClick={enterLogin}
            >
              Вход
            </button>
          </div>}
          <h1>{inviteSetup ? "Завершите регистрацию" : isRegister ? "Создайте аккаунт" : "С возвращением"}</h1>
          <p>
            {inviteSetup
              ? "Создайте пароль, чтобы войти в приглашённое путешествие."
              : isRegister
              ? "Начните планировать первое путешествие за пару минут."
              : "Войдите, чтобы продолжить планирование путешествий."}
          </p>
          {!inviteSetup && <><div className="auth-providers">
            <button>
              <b className="google-mark">G</b> Google
            </button>
          </div>
          <div className="auth-divider">
            <span>или через e-mail</span>
          </div></>}
          <form autoComplete={isRegister ? "off" : "on"} onSubmit={handleSubmit}>
            <label className={isRegister ? "" : "hidden"}>
              Имя
              <input
                name="name"
                placeholder="Введите имя"
                autoComplete={isRegister ? "off" : "name"}
              />
            </label>
            {canUseRememberedAccount ? (
              <label className="auth-account-picker">
                Аккаунт
                <button
                  type="button"
                  className="auth-account-trigger"
                  aria-expanded={accountListOpen}
                  onClick={() => setAccountListOpen((open) => !open)}
                >
                  <span className="auth-account-avatar">{selectedAccount ? accountInitial(selectedAccount) : "?"}</span>
                  <span>
                    <strong>{selectedAccount?.name}</strong>
                    <small>{selectedAccount?.email}</small>
                  </span>
                  <i>{accountListOpen ? "⌃" : "⌄"}</i>
                </button>
                {accountListOpen && (
                  <div className="auth-account-list" role="listbox" aria-label="Сохранённые аккаунты">
                    {rememberedAccounts.map((account, index) => (
                      <button
                        type="button"
                        role="option"
                        aria-selected={selectedAccount?.email === account.email}
                        className={selectedAccount?.email === account.email ? "active" : ""}
                        key={account.email}
                        onClick={() => chooseRememberedAccount(account)}
                      >
                        <span className="auth-account-avatar">{accountInitial(account)}</span>
                        <span>
                          <strong>{account.name}</strong>
                          <small>{account.email}</small>
                        </span>
                        {index === 0 && <em>последний</em>}
                      </button>
                    ))}
                    <button
                      type="button"
                      className="auth-account-manual"
                      onClick={() => {
                        setManualEmail(true);
                        setAccountListOpen(false);
                        setSelectedAccount(null);
                        setEmail("");
                      }}
                    >
                      Войти с другим аккаунтом
                    </button>
                  </div>
                )}
                <input type="hidden" name="email" value={email} />
              </label>
            ) : (
              <label>
                E-mail
                <input
                  name="email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  placeholder="you@example.com"
                  autoComplete={isRegister ? "off" : "email"}
                  readOnly={inviteSetup}
                />
              </label>
            )}
            <label>
              Пароль
              <PasswordField
                name="password"
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
              {inviteSetup ? "Завершить регистрацию" : isRegister ? "Создать аккаунт" : "Войти"}
            </button>
          </form>
          {message && (
            <p className="auth-message" role="status">
              {message}
            </p>
          )}
          {!inviteSetup && <div className="auth-footer">
            {isRegister ? "Уже есть аккаунт?" : "Впервые в Ramingo?"}{" "}
            <button
              onClick={() => {
                if (isRegister) enterLogin();
                else enterRegistration();
              }}
            >
              {isRegister ? "Войти" : "Зарегистрироваться"}
            </button>
          </div>}
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
              <PasswordField
                value={password}
                onChange={(event) => setPassword(event.target.value)}
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
  const authSearch = new URLSearchParams(
    location.search || window.location.search,
  );
  const inviteSetup = authSearch.get("inviteSetup") === "1";
  const inviteNextPath = authSearch.get("next") || undefined;
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
        "pets",
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
                : location.pathname === "/housing-preview"
                  ? "housing-preview"
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
  const [rememberedAccounts, setRememberedAccounts] = useState<RememberedAccount[]>([]);
  const [darkTheme, setDarkTheme] = useState(false);
  const persistDarkTheme = (value: boolean) => {
    setDarkTheme(value);
    void supabase.auth.updateUser({ data: { dark_theme: value } }).then(({ error }) => {
      if (error) console.error("Could not save the theme preference.", error);
    });
  };
  const rememberAccount = (account: RememberedAccount) => {
    if (!account.email.trim()) return;
    setRememberedAccounts((current) => [
      account,
      ...current.filter((item) => item.email.toLowerCase() !== account.email.toLowerCase()),
    ].slice(0, 5));
  };
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
      "housing-preview": "/housing-preview",
    };
    navigate(next === "trip" ? `/trips/${tripId}/overview` : paths[next]);
    setMenu(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  useEffect(() => {
    const setAuthenticatedUser = (
      user: {
        email?: string;
        user_metadata: {
          full_name?: string;
          invite_pending?: boolean;
          invite_trip_id?: string;
          dark_theme?: boolean;
        };
      },
      shouldNavigate = false,
    ) => {
      setProfileName(
        user.user_metadata.full_name || user.email || "Путешественник",
      );
      if (!shouldNavigate) return;
      if (inviteSetup) return;
      const nextPath = new URLSearchParams(
        location.search || window.location.search,
      ).get("next");
      const inviteTrip = nextPath && matchPath("/trips/:tripId/:tab?", nextPath);
      // Supabase magic/invite links can return with auth data in the hash.
      // HashRouter temporarily sees that hash as a route, so do not require
      // the pathname to be `/` or `/auth` before opening the invited trip.
      const pendingInvite =
        inviteTrip &&
        user.user_metadata.invite_pending === true &&
        user.user_metadata.invite_trip_id === inviteTrip.params.tripId;
      if (pendingInvite) {
        navigate(
          `/auth?inviteSetup=1&next=${encodeURIComponent(nextPath)}`,
          { replace: true },
        );
        return;
      }
      if (inviteTrip) {
        navigate(nextPath, { replace: true });
        return;
      }
      // Auth events can arrive after the component mounted (for example when
      // another browser tab refreshes the Supabase session). The `location`
      // captured by this effect is then stale and may still be `/`, which
      // incorrectly redirects an already-open trip tab to the trips list.
      // Read the current HashRouter route at the moment of the event instead.
      const liveHashPath = window.location.hash.startsWith("#/")
        ? window.location.hash.slice(1).split("?")[0]
        : "";
      const livePath = liveHashPath || window.location.pathname;
      if (livePath === "/auth" || livePath === "/") {
        navigate("/trips", { replace: true });
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
      const signedTrip = await signTripPhotoUrls(trip);
      setStoredPayload(payload);
      setDrafts((items) =>
        items.some((item) => item.id === signedTrip.id)
          ? items
          : [...items, signedTrip],
      );
    };
    const loadUserData = async () => {
      const { data, error } = await supabase
        .from("trips")
        .select("id,payload,owner_id");
      if (error) {
        console.error("Could not load trips.", error);
        return;
      }
      const parsedRemoteDrafts = ((data || []) as TripRow[])
        .map((row) => {
          const trip = tripFromRow(row);
          return trip
            ? markTripOwner(trip, row.owner_id)
            : null;
        })
        .filter((trip): trip is TripSummary => trip !== null);
      const remoteDrafts = await Promise.all(
        parsedRemoteDrafts.map((trip) => signTripPhotoUrls(trip)),
      );
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
      if (!data.session?.user) {
        setDarkTheme(false);
        setAuthReady(true);
        setIsAuthenticated(false);
        if (location.pathname !== "/auth" && view !== "housing-preview") {
          const next = `${location.pathname}${location.search}`;
          navigate(`/auth?next=${encodeURIComponent(next)}`, { replace: true });
        }
        return;
      }
      setDarkTheme(data.session.user.user_metadata.dark_theme === true);
      setAuthReady(true);
      setIsAuthenticated(true);
      setAuthenticatedUser(data.session.user, true);
      rememberAccount({
        email: data.session.user.email || "",
        name: data.session.user.user_metadata.full_name || data.session.user.email || "Путешественник",
      });
      void loadUserData();
      void loadSavedTrip();
    });
    const { data: listener } = supabase.auth.onAuthStateChange(
      (event, session) => {
        if (session?.user) {
          setDarkTheme(session.user.user_metadata.dark_theme === true);
          setAuthReady(true);
          setIsAuthenticated(true);
          setAuthenticatedUser(session.user, event === "SIGNED_IN");
          if (event === "SIGNED_IN") {
            void loadUserData();
            void loadSavedTrip();
          }
        } else if (event === "SIGNED_OUT") {
          setDarkTheme(false);
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
    const nextPayload: StoredTripPayload = canonicalTripPhotoUrls({
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
          photos: trip.photos,
          coverTextColor: trip.coverTextColor,
          overviewMapPoints: trip.overviewMapPoints,
          sightDays: trip.sightDays,
          sightDaysVersion: trip.sightDaysVersion,
          sightNotes: trip.sightNotes,
          petPlaces: trip.petPlaces,
          members: trip.members,
          publicLinkEnabled: trip.publicLinkEnabled,
          published: trip.published,
        },
      },
    });
    setStoredPayload(nextPayload);
    void supabase
      .from("trip_state")
      .update({ payload: nextPayload })
      .eq("id", "main")
      .then(({ error }) => {
        if (error) console.error("Could not save the trip.", error);
      });
  };
  const deleteTrip = async (trip: TripSummary) => {
    const {
      data: { session },
    } = await supabase.auth.getSession();
    if (!session?.user) throw new Error("Not authenticated");
    const { error } = await supabase
      .from("trips")
      .delete()
      .eq("id", trip.id)
      .eq("owner_id", session.user.id);
    if (error) throw error;
    setDrafts((items) => items.filter((item) => item.id !== trip.id));
  };
  const leaveTrip = async (trip: TripSummary) => {
    const { error } = await supabase.functions.invoke("leave-trip", {
      body: { tripId: trip.id },
    });
    if (error) throw error;
    setDrafts((items) => items.filter((item) => item.id !== trip.id));
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
      view !== "housing-preview" &&
      view !== "delete-account" &&
      view !== "privacy" &&
      view !== "terms") ||
    (isAuthenticated && view === "auth" && !inviteSetup)
  ) {
    return null;
  }
  if (view === "delete-account") return <AccountDeletionPage />;
  if (view === "privacy") return <LegalPage kind="privacy" />;
  if (view === "terms") return <LegalPage kind="terms" />;
  if (view === "housing-preview") return <AccommodationPrototype />;
  if (view === "auth")
    return (
        <Auth
          go={go}
          onAuthorized={(name) => {
            setProfileName(name);
            setIsAuthenticated(true);
          }}
          rememberedAccounts={rememberedAccounts}
          onRememberedAccount={rememberAccount}
          inviteSetup={inviteSetup}
          inviteNextPath={inviteNextPath}
          onInviteComplete={(nextPath) =>
            navigate(nextPath || "/trips", { replace: true })
          }
        />
    );
  return (
    <div className={`app${darkTheme ? " app-dark" : ""}`}>
      <Sidebar
        view={view}
        go={go}
        open={menu}
        close={() => setMenu(false)}
        profileName={profileName}
        tripCount={drafts.length}
        darkTheme={darkTheme}
        onDarkThemeChange={persistDarkTheme}
        cityCount={
          new Set(
            drafts.flatMap((trip) =>
              trip.cities
                .split(/[,·]/)
                .map((city) => city.trim())
                .filter(Boolean),
            ),
          ).size
        }
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
            onDeleteTrip={deleteTrip}
            onLeaveTrip={leaveTrip}
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
              darkTheme={darkTheme}
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
