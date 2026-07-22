import { useEffect, useEffectEvent, useRef, useState, type FormEvent, type ReactNode } from "react";
import type { Map } from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import { supabase } from "./supabase";

type View = "auth" | "trips" | "create" | "trip" | "catalog" | "public";
type Tab = "overview" | "route" | "sights" | "bookings" | "budget" | "photos" | "members";
type RoadLeg = { from: string; to: string; checkInFrom: string; checkInTo: string; checkOutFrom: string; checkOutTo: string; notes: string; mapsUrl?: string; completed?: string[] };
type DraftDay = { id: string; places: string[]; roadLeg?: RoadLeg };
type CoverPhoto = { id: string; image: string; city?: string; description?: string };
type TripSummary = { id: string; title: string; dates: string; cities: string; status: string; progress: number; tone: string; isDraft?: boolean; coverImage?: string; coverPhotos?: CoverPhoto[]; coverCity?: string; coverDescription?: string; places?: string[]; days?: DraftDay[]; sights?: StoredSight[]; sightDays?: { id: string; title: string }[]; sightDaysVersion?: number; sightNotes?: Record<string, string> };
type StoredDay = { id?: string; city?: string; dayMapUrl?: string; checkInFrom?: string; checkInTo?: string; checkOutFrom?: string; checkOutTo?: string; completed?: string[]; items?: { id?: string; title?: string; done?: boolean }[] };
type StoredSight = { id: string; name: string; city: string; done?: boolean; group?: string; photo?: string; lnglat?: [number, number]; walkDay?: number; walkOrder?: number; subcategory?: string; description?: string; duration?: string };
type StoredTripPayload = { data?: { days?: StoredDay[]; sights?: StoredSight[]; trip?: { start?: string; end?: string }; [key: string]: unknown }; [key: string]: unknown };

function mapsUrl(from: string, to: string) {
  return `https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(from)}&destination=${encodeURIComponent(to)}&travelmode=driving`;
}

function cityFlag(city: string) {
  if (city.includes("Прага")) return "🇨🇿";
  if (city.includes("Зальцбург")) return "🇦🇹";
  if (city.includes("Мюнхен") || city.includes("Равенсбург")) return "🇩🇪";
  if (["Верона", "Рим", "Фильине", "Кьоджа", "Милан", "Вальдидентро", "Флоренция", "Венеция"].some((name) => city.includes(name))) return "🇮🇹";
  return "";
}

function savedTrip(payload: StoredTripPayload): TripSummary | null {
  const storedDays = payload.data?.days;
  if (!storedDays?.length) return null;
  const days = storedDays.map((day, index) => {
    const [from = "", to = ""] = (day.city || "").split("→").map((city) => city.trim());
    const completed = day.completed || day.items?.flatMap((item) => {
      if (!item.done) return [];
      if (item.title?.startsWith("Выезд")) return ["departure"];
      if (item.title?.startsWith("Заселение")) return ["check-in"];
      if (item.title?.startsWith("Выселение")) return ["check-out"];
      return [];
    }) || [];
    return {
      id: day.id || `saved-day-${index + 1}`,
      places: day.items?.map((item) => item.title || "").filter(Boolean) || [],
      roadLeg: from || to ? { from, to, checkInFrom: day.checkInFrom || "", checkInTo: day.checkInTo || "", checkOutFrom: day.checkOutFrom || "", checkOutTo: day.checkOutTo || "", notes: "", mapsUrl: day.dayMapUrl, completed } : undefined,
    };
  });
  const start = payload.data?.trip?.start;
  const end = payload.data?.trip?.end;
  return {
    id: "supabase-main",
    title: "Путешествие",
    dates: start && end ? `${start} - ${end}` : "Даты путешествия",
    cities: storedDays.map((day) => day.city).filter(Boolean).slice(0, 3).join(" · "),
    status: "Активное",
    progress: 0,
    tone: "stone",
    isDraft: true,
    days,
  };
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
  "Прага": [14.4378, 50.0755],
  "Зальцбург": [13.045, 47.8095],
  "Мюнхен": [11.582, 48.1351],
  "Равенсбург": [9.611, 47.781],
  "Верона": [10.9916, 45.4384],
  "Рим": [12.4964, 41.9028],
  "Фильине-Вальдарно": [11.469, 43.62],
  "Кьоджа": [12.278, 45.219],
  "Милан": [9.19, 45.4642],
  "Вальдидентро": [10.3, 46.49],
  "Флоренция": [11.2558, 43.7696],
  "Венеция": [12.3155, 45.4408],
  "Москва": [37.6173, 55.7558],
};

function mapLocation(city: string) {
  return Object.entries(mapLocations).find(([name]) => city.includes(name))?.[1];
}

function routeCoordinatesFor(days: DraftDay[]) {
  return days.flatMap((day, index) => {
    const leg = day.roadLeg;
    if (!leg) return [];
    return index === 0 ? [leg.from, leg.to] : [leg.to];
  }).map(mapLocation).filter((coordinate): coordinate is [number, number] => Boolean(coordinate));
}

function routeSegment(coordinates: [number, number][], day: number) {
  return coordinates.slice(day, day + 2);
}

const winterPhotoCaptions = [
  ["Мюнхен", "Столица Баварии в декабре превращается в светящуюся рождественскую сцену. Готические башни Новой ратуши возвышаются над ярмаркой на Мариенплац."],
  ["Верона", "Зимняя Пьяцца Бра сияет огнями рождественской ярмарки у стен древней Арены. Вечерняя прогулка здесь соединяет итальянскую историю и праздничное настроение."],
  ["Рим", "Вечный город зимой становится спокойнее, но не теряет своего характера. Тёплый свет площадей делает вечерние прогулки особенно красивыми."],
  ["Кьоджа", "Небольшой город у лагуны хранит морской ритм и тихие каналы. Здесь удобно замедлиться перед дорогой в Венецию."],
  ["Венеция", "Город каналов зимой звучит тише: туман, вода и старые фасады создают почти кинематографичное настроение."],
  ["Милан", "Милан соединяет праздничные витрины, современный ритм и классическую итальянскую архитектуру. Вечерний город особенно живой."],
  ["Равенсбург", "Средневековые башни и цветные фасады делают Равенсбург уютной остановкой на зимнем маршруте."],
  ["Прага", "Прага в праздничный сезон сияет огнями Старого города. Каменные мосты и черепичные крыши создают атмосферу зимней сказки."],
] as const;

const munichDayOneSights: StoredSight[] = [
  { id: "munich-karlsplatz", name: "Karlsplatz (Stachus)", city: "Мюнхен", walkDay: 1, walkOrder: 0, lnglat: [11.5659, 48.1391], duration: "20 мин", description: "Оживлённая площадь у западного входа в исторический центр Мюнхена." },
  { id: "munich-neuhauser", name: "Neuhauser Straße", city: "Мюнхен", walkDay: 1, walkOrder: 1, lnglat: [11.5685, 48.1385], duration: "30 мин", description: "Пешеходная улица с рождественскими витринами, гирляндами и праздничными украшениями." },
  { id: "munich-karlstor", name: "Karlstor", city: "Мюнхен", walkDay: 1, walkOrder: 2, lnglat: [11.5656, 48.1389], duration: "15 мин", description: "Средневековые городские ворота, открывающие путь в Старый город." },
  { id: "munich-marienplatz", name: "Marienplatz", city: "Мюнхен", walkDay: 1, walkOrder: 3, lnglat: [11.5755, 48.1374], duration: "30 мин", description: "Главная площадь Мюнхена и сердце праздничного Старого города." },
  { id: "munich-neues-rathaus", name: "Новая ратуша (Neues Rathaus)", city: "Мюнхен", walkDay: 1, walkOrder: 4, lnglat: [11.5756, 48.1376], duration: "30 мин", description: "Неоготическая ратуша с башней, часами и знаменитым Глокеншпилем." },
  { id: "munich-christkindlmarkt", name: "Christkindlmarkt", city: "Мюнхен", walkDay: 1, walkOrder: 5, lnglat: [11.5752, 48.1372], duration: "1,5 ч", description: "Главная рождественская ярмарка города с ремесленными лавками и баварскими угощениями." },
  { id: "munich-frauenkirche", name: "Frauenkirche", city: "Мюнхен", walkDay: 1, walkOrder: 6, lnglat: [11.5734, 48.1386], duration: "30 мин", description: "Кафедральный собор и один из главных архитектурных символов Мюнхена." },
  { id: "munich-kaufingerstrasse", name: "Kaufingerstraße", city: "Мюнхен", walkDay: 1, walkOrder: 7, lnglat: [11.5712, 48.1379], duration: "30 мин", description: "Праздничная торговая улица, особенно красивая в вечерней подсветке." },
  { id: "munich-residenz-weihnachtsdorf", name: "Residenz Weihnachtsdorf", city: "Мюнхен", walkDay: 1, walkOrder: 8, lnglat: [11.5784, 48.1411], duration: "1 ч", description: "Уютная рождественская деревня во дворе Мюнхенской резиденции." },
  { id: "munich-max-joseph-platz", name: "Max-Joseph-Platz", city: "Мюнхен", walkDay: 1, walkOrder: 9, lnglat: [11.5789, 48.1398], duration: "20 мин", description: "Парадная площадь перед Баварской государственной оперой и Резиденцией." },
  { id: "munich-odeonsplatz", name: "Odeonsplatz", city: "Мюнхен", walkDay: 1, walkOrder: 10, lnglat: [11.5777, 48.1421], duration: "25 мин", description: "Монументальная площадь на границе Старого города и дворцового квартала." },
  { id: "munich-feldherrnhalle", name: "Feldherrnhalle", city: "Мюнхен", walkDay: 1, walkOrder: 11, lnglat: [11.5778, 48.1424], duration: "15 мин", description: "Аркада XIX века, вдохновлённая флорентийской Лоджией Ланци." },
  { id: "munich-theatinerkirche", name: "Theatinerkirche", city: "Мюнхен", walkDay: 1, walkOrder: 12, lnglat: [11.5768, 48.1422], duration: "25 мин", description: "Барочная церковь с выразительным жёлтым фасадом и красивой вечерней подсветкой." },
  { id: "munich-hofgarten", name: "Hofgarten", city: "Мюнхен", walkDay: 1, walkOrder: 13, lnglat: [11.5808, 48.1426], duration: "30 мин", description: "Спокойный придворный сад рядом с Резиденцией, завершающий прогулку." },
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

const veronaDayTwoSights: StoredSight[] = [
  { id: "verona-piazza-bra", name: "Piazza Bra", city: "Верона", walkDay: 2, walkOrder: 0, lnglat: [10.9915, 45.4384], duration: "25 мин" },
  { id: "verona-arena", name: "Арена Вероны (Arena di Verona)", city: "Верона", walkDay: 2, walkOrder: 1, lnglat: [10.9942, 45.438], duration: "45 мин" },
  { id: "verona-rigoletto", name: "Рождественская звезда Rigoletto", city: "Верона", walkDay: 2, walkOrder: 2, lnglat: [10.9923, 45.4384], duration: "20 мин" },
  { id: "verona-mazzini", name: "Via Giuseppe Mazzini", city: "Верона", walkDay: 2, walkOrder: 3, lnglat: [10.9958, 45.4401], duration: "30 мин" },
  { id: "verona-erbe", name: "Piazza delle Erbe", city: "Верона", walkDay: 2, walkOrder: 4, lnglat: [10.9972, 45.4431], duration: "30 мин" },
  { id: "verona-signori", name: "Piazza dei Signori", city: "Верона", walkDay: 2, walkOrder: 5, lnglat: [10.9983, 45.4425], duration: "25 мин" },
  { id: "verona-christkindlmarkt", name: "Рождественская ярмарка Christkindlmarkt", city: "Верона", walkDay: 2, walkOrder: 6, lnglat: [10.998, 45.4427], duration: "1 ч" },
  { id: "verona-juliet", name: "Дворик Джульетты (Casa di Giulietta)", city: "Верона", walkDay: 2, walkOrder: 7, lnglat: [10.9994, 45.4429], duration: "30 мин" },
  { id: "verona-ponte-pietra", name: "Ponte Pietra", city: "Верона", walkDay: 2, walkOrder: 8, lnglat: [11.0053, 45.4472], duration: "35 мин" },
  { id: "verona-adige", name: "Набережная реки Адидже", city: "Верона", walkDay: 2, walkOrder: 9, lnglat: [11.0037, 45.4465], duration: "30 мин" },
];

const romeDayThreeSights: StoredSight[] = [
  { id: "rome-navona", name: "Piazza Navona (рождественская ярмарка)", city: "Рим", walkDay: 3, walkOrder: 0, lnglat: [12.4731, 41.8992], duration: "1 ч" },
  { id: "rome-four-rivers", name: "Фонтан Четырех рек", city: "Рим", walkDay: 3, walkOrder: 1, lnglat: [12.4733, 41.8992], duration: "20 мин" },
  { id: "rome-sant-agnese", name: "Церковь Sant'Agnese in Agone", city: "Рим", walkDay: 3, walkOrder: 2, lnglat: [12.4735, 41.8991], duration: "25 мин" },
  { id: "rome-pantheon", name: "Пантеон", city: "Рим", walkDay: 3, walkOrder: 3, lnglat: [12.4769, 41.8986], duration: "40 мин" },
  { id: "rome-rotonda", name: "Piazza della Rotonda", city: "Рим", walkDay: 3, walkOrder: 4, lnglat: [12.4767, 41.899], duration: "20 мин" },
  { id: "rome-hadrian", name: "Храм Адриана", city: "Рим", walkDay: 3, walkOrder: 5, lnglat: [12.4793, 41.9002], duration: "20 мин" },
  { id: "rome-colonna", name: "Piazza Colonna", city: "Рим", walkDay: 3, walkOrder: 6, lnglat: [12.4792, 41.901], duration: "20 мин" },
  { id: "rome-marcus-aurelius", name: "Колонна Марка Аврелия", city: "Рим", walkDay: 3, walkOrder: 7, lnglat: [12.4794, 41.9014], duration: "15 мин" },
  { id: "rome-trevi", name: "Фонтан Треви", city: "Рим", walkDay: 3, walkOrder: 8, lnglat: [12.4833, 41.9009], duration: "30 мин" },
  { id: "rome-spagna", name: "Piazza di Spagna", city: "Рим", walkDay: 3, walkOrder: 9, lnglat: [12.4824, 41.906], duration: "20 мин" },
  { id: "rome-spanish-steps", name: "Испанская лестница", city: "Рим", walkDay: 3, walkOrder: 10, lnglat: [12.4828, 41.906], duration: "30 мин" },
  { id: "rome-spagna-tree", name: "Рождественская елка на Piazza di Spagna", city: "Рим", walkDay: 3, walkOrder: 11, lnglat: [12.4824, 41.906], duration: "20 мин" },
  { id: "rome-condotti", name: "Via Condotti", city: "Рим", walkDay: 3, walkOrder: 12, lnglat: [12.4798, 41.9055], duration: "30 мин" },
  { id: "rome-corso", name: "Via del Corso", city: "Рим", walkDay: 3, walkOrder: 13, lnglat: [12.4793, 41.9014], duration: "30 мин" },
];

const romeDayFourSights: StoredSight[] = [
  { id: "rome-colosseum", name: "Колизей", city: "Рим", walkDay: 4, walkOrder: 0, lnglat: [12.4922, 41.8902], duration: "1 ч" },
  { id: "rome-constantine", name: "Арка Константина", city: "Рим", walkDay: 4, walkOrder: 1, lnglat: [12.4909, 41.8899], duration: "20 мин" },
  { id: "rome-forum", name: "Римский форум", city: "Рим", walkDay: 4, walkOrder: 2, lnglat: [12.4853, 41.8925], duration: "1,5 ч" },
  { id: "rome-palatine", name: "Палатинский холм", city: "Рим", walkDay: 4, walkOrder: 3, lnglat: [12.4882, 41.889], duration: "1 ч" },
  { id: "rome-palatine-view", name: "Холм Палатин (смотровые площадки)", city: "Рим", walkDay: 4, walkOrder: 4, lnglat: [12.4871, 41.8898], duration: "30 мин" },
  { id: "rome-capitoline", name: "Капитолийская площадь", city: "Рим", walkDay: 4, walkOrder: 5, lnglat: [12.4828, 41.8933], duration: "30 мин" },
  { id: "rome-marcus-statue", name: "Статуя Марка Аврелия", city: "Рим", walkDay: 4, walkOrder: 6, lnglat: [12.4829, 41.8934], duration: "15 мин" },
  { id: "rome-forum-view", name: "Смотровая площадка на Форум", city: "Рим", walkDay: 4, walkOrder: 7, lnglat: [12.4835, 41.8927], duration: "25 мин" },
  { id: "rome-venezia", name: "Piazza Venezia", city: "Рим", walkDay: 4, walkOrder: 8, lnglat: [12.4828, 41.8962], duration: "25 мин" },
  { id: "rome-vittoriano", name: "Монумент Виктору Эммануилу II", city: "Рим", walkDay: 4, walkOrder: 9, lnglat: [12.4826, 41.8947], duration: "40 мин" },
  { id: "rome-altare-terrazza", name: "Панорамная терраса Altare della Patria", city: "Рим", walkDay: 4, walkOrder: 10, lnglat: [12.4824, 41.8949], duration: "30 мин" },
  { id: "rome-marcellus", name: "Театр Марцелла", city: "Рим", walkDay: 4, walkOrder: 11, lnglat: [12.4788, 41.8919], duration: "25 мин" },
  { id: "rome-octavia", name: "Портик Октавии", city: "Рим", walkDay: 4, walkOrder: 12, lnglat: [12.4778, 41.8924], duration: "20 мин" },
  { id: "rome-ghetto", name: "Еврейский квартал", city: "Рим", walkDay: 4, walkOrder: 13, lnglat: [12.4774, 41.8921], duration: "45 мин" },
  { id: "rome-campo", name: "Campo de' Fiori", city: "Рим", walkDay: 4, walkOrder: 14, lnglat: [12.4722, 41.8957], duration: "30 мин" },
  { id: "rome-popolo", name: "Piazza del Popolo", city: "Рим", walkDay: 4, walkOrder: 15, lnglat: [12.4769, 41.91], duration: "30 мин" },
  { id: "rome-pincio", name: "Терраса Pincio", city: "Рим", walkDay: 4, walkOrder: 16, lnglat: [12.4778, 41.9122], duration: "35 мин" },
];

const romeDayFiveSights: StoredSight[] = [
  { id: "rome-st-peter-square", name: "Площадь Святого Петра", city: "Рим", walkDay: 5, walkOrder: 0, lnglat: [12.4539, 41.9022], duration: "30 мин" },
  { id: "rome-st-peter-basilica", name: "Собор Святого Петра", city: "Рим", walkDay: 5, walkOrder: 1, lnglat: [12.4539, 41.9022], duration: "1,5 ч" },
  { id: "rome-vatican-tree", name: "Главная рождественская елка Ватикана", city: "Рим", walkDay: 5, walkOrder: 2, lnglat: [12.4538, 41.9023], duration: "20 мин" },
  { id: "rome-vatican-nativity", name: "Рождественский вертеп", city: "Рим", walkDay: 5, walkOrder: 3, lnglat: [12.4536, 41.9024], duration: "20 мин" },
  { id: "rome-conciliazione", name: "Via della Conciliazione", city: "Рим", walkDay: 5, walkOrder: 4, lnglat: [12.4595, 41.902], duration: "30 мин" },
  { id: "rome-borgo-pio", name: "Район Borgo Pio", city: "Рим", walkDay: 5, walkOrder: 5, lnglat: [12.4574, 41.904], duration: "40 мин" },
  { id: "rome-umberto", name: "Мост Умберто I", city: "Рим", walkDay: 5, walkOrder: 6, lnglat: [12.4752, 41.903], duration: "30 мин" },
  { id: "rome-tiber-walk", name: "Прогулка по набережной Тибра", city: "Рим", walkDay: 5, walkOrder: 7, lnglat: [12.471, 41.899], duration: "45 мин" },
  { id: "rome-tiberina", name: "Остров Тиберина", city: "Рим", walkDay: 5, walkOrder: 8, lnglat: [12.4781, 41.8932], duration: "30 мин" },
  { id: "rome-trastevere", name: "Район Трастевере", city: "Рим", walkDay: 5, walkOrder: 9, lnglat: [12.4699, 41.888], duration: "1 ч" },
  { id: "rome-santa-maria", name: "Базилика Santa Maria in Trastevere", city: "Рим", walkDay: 5, walkOrder: 10, lnglat: [12.4708, 41.8896], duration: "30 мин" },
  { id: "rome-santa-maria-square", name: "Piazza Santa Maria in Trastevere", city: "Рим", walkDay: 5, walkOrder: 11, lnglat: [12.4707, 41.8897], duration: "25 мин" },
  { id: "rome-janiculum", name: "Холм Джаниколо (Janiculum Hill)", city: "Рим", walkDay: 5, walkOrder: 12, lnglat: [12.4608, 41.8934], duration: "45 мин" },
  { id: "rome-acqua-paola", name: "Фонтан Аква Паола", city: "Рим", walkDay: 5, walkOrder: 13, lnglat: [12.456, 41.8893], duration: "25 мин" },
];

const sanMarinoDaySixSights: StoredSight[] = [
  { id: "san-marino-porta", name: "Porta San Francesco", city: "Сан-Марино", walkDay: 6, walkOrder: 0, lnglat: [12.4475, 43.9357], duration: "15 мин" },
  { id: "san-marino-streets", name: "Средневековые улочки исторического центра", city: "Сан-Марино", walkDay: 6, walkOrder: 1, lnglat: [12.4478, 43.9354], duration: "20 мин" },
  { id: "san-marino-basilica", name: "Базилика Сан-Марино", city: "Сан-Марино", walkDay: 6, walkOrder: 2, lnglat: [12.4473, 43.9361], duration: "20 мин" },
  { id: "san-marino-liberty", name: "Piazza della Libertà", city: "Сан-Марино", walkDay: 6, walkOrder: 3, lnglat: [12.447, 43.9363], duration: "20 мин" },
  { id: "san-marino-guard", name: "Смена караула у Правительственного дворца", city: "Сан-Марино", walkDay: 6, walkOrder: 4, lnglat: [12.447, 43.9363], duration: "15 мин" },
  { id: "san-marino-palazzo", name: "Palazzo Pubblico", city: "Сан-Марино", walkDay: 6, walkOrder: 5, lnglat: [12.447, 43.9363], duration: "20 мин" },
  { id: "san-marino-liberty-view", name: "Смотровая площадка Piazza della Libertà", city: "Сан-Марино", walkDay: 6, walkOrder: 6, lnglat: [12.4468, 43.9364], duration: "15 мин" },
  { id: "san-marino-eugippo", name: "Via Eugippo и Contrada del Collegio", city: "Сан-Марино", walkDay: 6, walkOrder: 7, lnglat: [12.4462, 43.9354], duration: "20 мин" },
  { id: "san-marino-guaita", name: "Первая башня Гуаита (Guaita)", city: "Сан-Марино", walkDay: 6, walkOrder: 8, lnglat: [12.4477, 43.934], duration: "25 мин" },
  { id: "san-marino-passo", name: "Панорамная тропа Passo delle Streghe", city: "Сан-Марино", walkDay: 6, walkOrder: 9, lnglat: [12.4487, 43.9327], duration: "25 мин" },
  { id: "san-marino-cesta", name: "Виды на Вторую башню Честа", city: "Сан-Марино", walkDay: 6, walkOrder: 10, lnglat: [12.4504, 43.9316], duration: "20 мин" },
  { id: "san-marino-panorama", name: "Панорамные виды на Апеннины и побережье Адриатики", city: "Сан-Марино", walkDay: 6, walkOrder: 11, lnglat: [12.4495, 43.932], duration: "20 мин" },
  { id: "san-marino-tree", name: "Главная рождественская елка на Piazza della Libertà", city: "Сан-Марино", walkDay: 6, walkOrder: 12, lnglat: [12.447, 43.9363], duration: "15 мин" },
  { id: "san-marino-lights", name: "Рождественская иллюминация исторического центра", city: "Сан-Марино", walkDay: 6, walkOrder: 13, lnglat: [12.4474, 43.9358], duration: "20 мин" },
  { id: "san-marino-market", name: "Рождественские ярмарочные домики", city: "Сан-Марино", walkDay: 6, walkOrder: 14, lnglat: [12.4471, 43.936], duration: "20 мин" },
];

function compressCoverPhoto(file: File) {
  return new Promise<string>((resolve, reject) => {
    const source = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      const scale = Math.min(1, 1280 / Math.max(image.width, image.height));
      const canvas = document.createElement("canvas");
      canvas.width = Math.round(image.width * scale);
      canvas.height = Math.round(image.height * scale);
      canvas.getContext("2d")?.drawImage(image, 0, 0, canvas.width, canvas.height);
      URL.revokeObjectURL(source);
      resolve(canvas.toDataURL("image/jpeg", 0.76));
    };
    image.onerror = () => { URL.revokeObjectURL(source); reject(new Error("Image decoding failed")); };
    image.src = source;
  });
}

function TripMap({ city, places = [], routeDays = [], activeDay }: { city?: string; places?: string[]; routeDays?: DraftDay[]; activeDay?: number }) {
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const markerElements = useRef<HTMLSpanElement[]>([]);
  const location = city ? mapLocation(city) : undefined;

  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    if (!container.current || !token) return;
    let disposed = false;
    let map: Map | undefined;

    void import("mapbox-gl").then(({ default: mapboxgl }) => {
      if (disposed || !container.current) return;
      mapboxgl.accessToken = token;
      const routeCoordinates = routeCoordinatesFor(routeDays);
      map = new mapboxgl.Map({
        container: container.current,
        style: "mapbox://styles/mapbox/streets-v12",
        center: routeCoordinates[0] ?? location ?? mapLocations["Москва"],
        zoom: routeCoordinates.length ? 5 : location ? 12 : 3,
        attributionControl: false,
      });
      mapRef.current = map;
      map.addControl(new mapboxgl.NavigationControl({ showCompass: false }), "top-right");

      if (routeCoordinates.length > 1) {
        markerElements.current = [];
        routeCoordinates.forEach((coordinate, index) => {
          const element = document.createElement("span");
          element.className = index === activeDay ? "map-marker active" : "map-marker";
          element.textContent = String(index + 1);
          markerElements.current.push(element);
          new mapboxgl.Marker({ element }).setLngLat(coordinate).addTo(map!);
        });
        map.on("load", () => {
          map!.addSource("route", { type: "geojson", data: { type: "Feature", properties: {}, geometry: { type: "LineString", coordinates: routeCoordinates } } });
          map!.addLayer({ id: "route", type: "line", source: "route", paint: { "line-color": "#4c46d6", "line-width": 3, "line-opacity": 0.72 } });
          const activeSegment = routeSegment(routeCoordinates, activeDay || 0);
          if (activeSegment.length > 1) {
            map!.addSource("active-route", { type: "geojson", data: { type: "Feature", properties: {}, geometry: { type: "LineString", coordinates: activeSegment } } });
            map!.addLayer({ id: "active-route", type: "line", source: "active-route", paint: { "line-color": "#ff7a45", "line-width": 6, "line-opacity": 0.95 } });
          }
          const bounds = new mapboxgl.LngLatBounds(routeCoordinates[0], routeCoordinates[0]);
          routeCoordinates.slice(1).forEach((coordinate) => bounds.extend(coordinate));
          map!.fitBounds(bounds, { padding: 42, maxZoom: 8 });
        });
      } else if (location && places.length) {
        const coordinates = places.map((_, index) => [
          location[0] + (index - 2) * 0.012,
          location[1] + ((index % 2 ? 1 : -1) * (index + 1)) * 0.006,
        ] as [number, number]);
        coordinates.forEach((coordinate, index) => {
          const element = document.createElement("span");
          element.className = "map-marker";
          element.textContent = String(index + 1);
          new mapboxgl.Marker({ element }).setLngLat(coordinate).addTo(map!);
        });
        map.on("load", () => {
          map!.addSource("route", {
            type: "geojson",
            data: { type: "Feature", properties: {}, geometry: { type: "LineString", coordinates } },
          });
          map!.addLayer({
            id: "route",
            type: "line",
            source: "route",
            paint: { "line-color": "#4c46d6", "line-width": 3, "line-opacity": 0.72 },
          });
        });
      }
    });

    return () => {
      disposed = true;
      map?.remove();
      mapRef.current = null;
      markerElements.current = [];
    };
  }, [city, location, places, routeDays]);

  useEffect(() => {
    const coordinate = activeDay === undefined ? undefined : routeCoordinatesFor(routeDays)[activeDay];
    if (!coordinate || !mapRef.current) return;
    markerElements.current.forEach((element, index) => element.classList.toggle("active", index === activeDay));
    mapRef.current.flyTo({ center: coordinate, zoom: 8, duration: 900, essential: true });
    const source = mapRef.current.getSource("active-route") as { setData: (data: object) => void } | undefined;
    const activeSegment = routeSegment(routeCoordinatesFor(routeDays), activeDay ?? 0);
    if (source && activeSegment.length > 1) source.setData({ type: "Feature", properties: {}, geometry: { type: "LineString", coordinates: activeSegment } });
  }, [activeDay, routeDays]);

  if (!import.meta.env.VITE_MAPBOX_ACCESS_TOKEN) {
    return <div className="map map-unavailable">Карта станет доступна после настройки Mapbox.</div>;
  }

  return <div ref={container} className="map" aria-label={city ? `Карта ${city}` : "Карта путешествия"} />;
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

function DatePicker({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  const selected = value ? new Date(`${value}T12:00:00`) : null;
  const [open, setOpen] = useState(false);
  const [month, setMonth] = useState(() => selected ? new Date(selected.getFullYear(), selected.getMonth(), 1) : new Date());
  const weekdayLabels = ["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"];
  const monthStart = (month.getDay() + 6) % 7;
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate();
  const formatted = selected ? new Intl.DateTimeFormat("ru-RU", { day: "numeric", month: "long", year: "numeric" }).format(selected) : "Выберите дату";
  const chooseDay = (day: number) => {
    const next = `${month.getFullYear()}-${String(month.getMonth() + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    onChange(next);
    setOpen(false);
  };
  return <label className="date-field">{label}<div className="date-picker"><button type="button" className="date-trigger" onClick={() => setOpen(!open)}><span className={value ? "" : "placeholder"}>{formatted}</span><svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M8 3v4m8-4v4M4 10h16" /></svg></button>{open && <div className="calendar-popover"><div className="calendar-header"><button type="button" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}>‹</button><b>{new Intl.DateTimeFormat("ru-RU", { month: "long", year: "numeric" }).format(month)}</b><button type="button" onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}>›</button></div><div className="calendar-grid calendar-weekdays">{weekdayLabels.map((day) => <span key={day}>{day}</span>)}</div><div className="calendar-grid">{Array.from({ length: monthStart }, (_, index) => <span key={`empty-${index}`} />)}{Array.from({ length: daysInMonth }, (_, index) => { const day = index + 1; const isSelected = selected?.getFullYear() === month.getFullYear() && selected?.getMonth() === month.getMonth() && selected?.getDate() === day; return <button type="button" className={isSelected ? "selected" : ""} onClick={() => chooseDay(day)} key={day}>{day}</button>; })}</div></div>}</div></label>;
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
  return (
    <>
      <button
        className={`scrim ${open ? "show" : ""}`}
        onClick={close}
        aria-label="Закрыть меню"
      />
      <aside className={`sidebar ${open ? "open" : ""}`}>
        <div className="brand">
          <span>О</span>
          <b>Одиссея</b>
          <button onClick={close}>×</button>
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
          <button className="account" onClick={() => setSettings(!settings)}>
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

function Trips({ go, profileName, drafts, onOpenTrip }: { go: (view: View) => void; profileName: string; drafts: TripSummary[]; onOpenTrip: (trip: TripSummary) => void }) {
  const [filter, setFilter] = useState("all");
  const allTrips = [...trips, ...drafts];
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
  const filteredTrips = filter === "all" ? allTrips : allTrips.filter((trip) => trip.status === statusByFilter[filter]);
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
          <button className={filter === value ? "selected" : ""} onClick={() => setFilter(value)} key={value}>{label}</button>
        ))}
      </div>
      <div className="trip-grid">
        {filteredTrips.map((trip, index) => (
          <article
            className="trip-card"
            key={trip.title}
            onClick={() => onOpenTrip(trip)}
          >
            <div className={`cover ${trip.tone}`}>
              <span className="status">● {trip.status}</span>
              <span className="cover-label">обложка</span>
              <div className="avatars">
                <Avatar>АС</Avatar>
                {index === 0 && (
                  <>
                    <Avatar tone="green">МК</Avatar>
                    <Avatar>+1</Avatar>
                  </>
                )}
              </div>
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
        {filteredTrips.length === 0 && <div className="empty-state">В этой категории пока нет путешествий.</div>}
        <button className="new-card" onClick={() => go("create")}>
          <i>＋</i>
          <b>Новое путешествие</b>
          <span>С нуля или из шаблона</span>
        </button>
      </div>
    </div>
  );
}

function CreateTrip({ go, onCreate }: { go: (view: View) => void; onCreate: (trip: TripSummary) => void }) {
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteName, setInviteName] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [invitees, setInvitees] = useState<{ name: string; email: string }[]>([]);
  const [inviteMessage, setInviteMessage] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [coverImage, setCoverImage] = useState("");
  const photoInputRef = useRef<HTMLInputElement>(null);
  const addInvitee = async () => {
    const email = inviteEmail.trim().toLowerCase();
    const name = inviteName.trim() || email;
    if (!email || invitees.some((person) => person.email === email)) return;
    const { data: { session } } = await supabase.auth.getSession();
    if (!session) {
      setInviteMessage("Сессия истекла. Войдите в аккаунт ещё раз.");
      return;
    }
    const response = await fetch(`${import.meta.env.VITE_SUPABASE_URL}/functions/v1/dynamic-function`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${session.access_token}`,
        apikey: import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ email, name, redirectTo: `${window.location.origin}${import.meta.env.BASE_URL}?invite=trip` }),
    });
    if (!response.ok) {
      const payload = await response.json().catch(() => null) as { error?: string } | null;
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
      <button className="back back-icon" onClick={() => go("trips")} aria-label="Вернуться к моим путешествиям" title="Мои путешествия">
        <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M19 12H5m0 0 6-6m-6 6 6 6" /></svg>
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
          const title = String(formData.get("title") || "").trim() || "Без названия";
          const cities = String(formData.get("cities") || "").trim();
          onCreate({ id: crypto.randomUUID(), title, cities, dates: startDate && endDate ? `${startDate} – ${endDate}` : "Даты не выбраны · черновик", status: "Черновик", progress: 0, tone: "stone", isDraft: true, coverImage });
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
          <DatePicker label="Дата начала" value={startDate} onChange={setStartDate} />
          <DatePicker label="Дата окончания" value={endDate} onChange={setEndDate} />
        </div>
          <label>
            Участники
            <div className="people">
              {invitees.map((person) => (
                <span className="participant-chip" key={person.email} title={person.email}>
                  <Avatar tone="blue">{person.name.split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase()}</Avatar>
                  <b>{person.name}</b>
                  <button type="button" className="remove-invite" onClick={() => setInvitees(invitees.filter((item) => item.email !== person.email))}>×</button>
                </span>
              ))}
              {inviteOpen ? (
                <div className="invite-person">
                  <input type="text" value={inviteName} onChange={(event) => setInviteName(event.target.value)} placeholder="Имя" autoFocus />
                  <input type="email" value={inviteEmail} onChange={(event) => setInviteEmail(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") { event.preventDefault(); addInvitee(); } }} placeholder="name@example.com" />
                  <button type="button" onClick={addInvitee}>Пригласить</button>
                  <button type="button" className="cancel-invite" onClick={() => { setInviteOpen(false); setInviteName(""); setInviteEmail(""); }}>×</button>
                </div>
              ) : (
                <button type="button" onClick={() => setInviteOpen(true)}>＋ Пригласить по e-mail</button>
              )}
            </div>
            {inviteMessage && <small className="invite-message">{inviteMessage}</small>}
          </label>
        <label>
          Обложка
          <input ref={photoInputRef} className="cover-file-input" type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => selectCoverImage(event.target.files?.[0])} />
          <button type="button" className={`upload ${coverImage ? "has-cover" : ""}`} style={coverImage ? { backgroundImage: `linear-gradient(rgba(27, 28, 31, 0.28), rgba(27, 28, 31, 0.28)), url(${coverImage})` } : undefined} onClick={() => photoInputRef.current?.click()}>
            {coverImage ? <span className="upload-photo-button">Сменить фото</span> : <><b>↑</b><span>Перетащите фото или выберите</span><small>1600×900 · jpg / png</small><span className="upload-photo-button">Загрузить фото</span></>}
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

function RoadLegEditor({ roadLeg, onChange, onSave, onCancel }: { roadLeg?: RoadLeg; onChange: (roadLeg: RoadLeg) => void; onSave: (roadLeg: RoadLeg) => void; onCancel: () => void }) {
  const [from, setFrom] = useState(roadLeg?.from || "");
  const [to, setTo] = useState(roadLeg?.to || "");
  const [checkInFrom, setCheckInFrom] = useState(roadLeg?.checkInFrom || "");
  const [checkInTo, setCheckInTo] = useState(roadLeg?.checkInTo || "");
  const [checkOutFrom, setCheckOutFrom] = useState(roadLeg?.checkOutFrom || "");
  const [checkOutTo, setCheckOutTo] = useState(roadLeg?.checkOutTo || "");
  const [notes, setNotes] = useState(roadLeg?.notes || "");
  const [customMapsUrl, setCustomMapsUrl] = useState(roadLeg?.mapsUrl || "");
  const generatedMapsUrl = from.trim() && to.trim() ? mapsUrl(from.trim(), to.trim()) : "";
  const routeMapsUrl = customMapsUrl.trim() || generatedMapsUrl;
  const emitChange = useEffectEvent(onChange);
  const latestRoadLeg = useRef<RoadLeg | null>(null);
  latestRoadLeg.current = { from: from.trim(), to: to.trim(), checkInFrom, checkInTo, checkOutFrom, checkOutTo, notes: notes.trim(), mapsUrl: customMapsUrl.trim() || undefined };
  useEffect(() => {
    if (![from, to, checkInFrom, checkInTo, checkOutFrom, checkOutTo, notes, customMapsUrl].some((value) => value.trim())) return;
    const timeout = window.setTimeout(() => emitChange({ from: from.trim(), to: to.trim(), checkInFrom, checkInTo, checkOutFrom, checkOutTo, notes: notes.trim(), mapsUrl: customMapsUrl.trim() || undefined }), 500);
    return () => window.clearTimeout(timeout);
  }, [from, to, checkInFrom, checkInTo, checkOutFrom, checkOutTo, notes, customMapsUrl]);
  useEffect(() => () => {
    const latest = latestRoadLeg.current;
    if (latest && Object.values(latest).some(Boolean)) emitChange(latest);
  }, []);
  return <form className="road-leg-editor" onSubmit={(event) => { event.preventDefault(); if (!from.trim() || !to.trim()) return; onSave({ from: from.trim(), to: to.trim(), checkInFrom, checkInTo, checkOutFrom, checkOutTo, notes: notes.trim(), mapsUrl: customMapsUrl.trim() || undefined }); }}>
    <div className="road-leg-editor-title"><b>Автомобильный маршрут</b><span>Заполните переезд на этот день</span></div>
    <div className="road-leg-fields"><label>Откуда<input value={from} onChange={(event) => setFrom(event.target.value)} placeholder="Например, Мюнхен" autoFocus /></label><label>Куда<input value={to} onChange={(event) => setTo(event.target.value)} placeholder="Например, Верона" /></label></div>
    <div className="road-leg-fields road-leg-times"><label>Заселение: с<input type="time" value={checkInFrom} onChange={(event) => setCheckInFrom(event.target.value)} /></label><label>Заселение: до<input type="time" value={checkInTo} onChange={(event) => setCheckInTo(event.target.value)} /></label><label>Выселение: с<input type="time" value={checkOutFrom} onChange={(event) => setCheckOutFrom(event.target.value)} /></label><label>Выселение: до<input type="time" value={checkOutTo} onChange={(event) => setCheckOutTo(event.target.value)} /></label></div>
    <label className="road-notes">Заметки<textarea value={notes} onChange={(event) => setNotes(event.target.value)} placeholder="Например, заправиться перед выездом" /></label>
    <label className="road-notes">Ссылка Google Maps<input type="url" value={customMapsUrl} onChange={(event) => setCustomMapsUrl(event.target.value)} placeholder="https://maps.app.goo.gl/..." /></label>
    {routeMapsUrl && <GoogleMapsLink url={routeMapsUrl} />}
    <div className="road-leg-actions"><button type="button" className="secondary" onClick={onCancel}>Отмена</button><button className="accent">Сохранить маршрут</button></div>
  </form>;
}

function GoogleMapsLink({ url }: { url: string }) {
  const [copied, setCopied] = useState(false);
  const copy = async () => {
    await navigator.clipboard.writeText(url).catch(() => undefined);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };
  return <div className="google-maps-link"><span><b>Google Maps</b><small>Автомобильный маршрут</small></span><a href={url} target="_blank" rel="noreferrer">Открыть ↗</a><button onClick={copy}>{copied ? "Скопировано" : "Копировать"}</button></div>;
}

function DraftRouteCard({ day, index, editing, selected, onSelect, onEdit, onChange, onSave, onCancel }: { day: DraftDay; index: number; editing: boolean; selected: boolean; onSelect: () => void; onEdit: () => void; onChange: (roadLeg: RoadLeg) => void; onSave: (roadLeg: RoadLeg) => void; onCancel: () => void }) {
  const roadLeg = day.roadLeg;
  const routeMapsUrl = roadLeg ? roadLeg.mapsUrl || mapsUrl(roadLeg.from, roadLeg.to) : "";
  const checkIn = [roadLeg?.checkInFrom, roadLeg?.checkInTo].filter(Boolean).join(" - ");
  const checkOut = [roadLeg?.checkOutFrom, roadLeg?.checkOutTo].filter(Boolean).join(" - ");
  const itemCount = roadLeg ? 1 + Number(Boolean(checkIn)) + Number(Boolean(checkOut)) + Number(Boolean(roadLeg.notes)) : 0;
  const checklist = [{ id: "departure", label: `Выезд из ${roadLeg?.from || "города"}` }, { id: "check-in", label: `Заселение в отель${checkIn ? ` ${checkIn}` : ""}` }, { id: "check-out", label: `Выселение из отеля${checkOut ? ` ${checkOut}` : ""}` }];
  return <article className={selected ? "draft-route-card selected" : "draft-route-card"}><header onClick={onSelect}><div className="draft-day-number"><b>{index + 1}</b><span>ДЕНЬ</span></div><div className="draft-route-title"><h2>{roadLeg ? <>{cityFlag(roadLeg.from)} {roadLeg.from || "Откуда"} <b>→</b> {cityFlag(roadLeg.to)} {roadLeg.to || "Куда"}</> : "Новый автопереезд"}</h2><span>{itemCount}/{roadLeg ? itemCount : 4} пунктов</span></div><div className="draft-route-actions">{roadLeg && <a href={routeMapsUrl} target="_blank" rel="noreferrer">↗ Карта</a>}<button onClick={onEdit}>{roadLeg ? "Изменить" : "＋ Маршрут"}</button></div></header>{editing ? <RoadLegEditor roadLeg={roadLeg} onChange={onChange} onSave={onSave} onCancel={onCancel} /> : roadLeg ? <><div className="route-checklist">{checklist.map((item) => <label className={roadLeg.completed?.includes(item.id) ? "completed" : ""} key={item.id}><input type="checkbox" checked={roadLeg.completed?.includes(item.id) || false} onChange={() => onChange({ ...roadLeg, completed: roadLeg.completed?.includes(item.id) ? roadLeg.completed.filter((id) => id !== item.id) : [...(roadLeg.completed || []), item.id] })} /><span>{item.label}</span></label>)}{roadLeg.notes && <p><i />{roadLeg.notes}</p>}</div><GoogleMapsLink url={routeMapsUrl} /></> : <div className="route-card-empty">Добавьте направление, время заселения и дорожные заметки.</div>}</article>;
}

function RouteTab({ isDraft = false, draftDays = [], editingRoadDay = null, onEditingRoadDayChange, onAddDraftDay, onUpdateDraftDay }: { isDraft?: boolean; draftDays?: DraftDay[]; editingRoadDay?: number | null; onEditingRoadDayChange?: (day: number | null) => void; onAddDraftDay?: () => void; onUpdateDraftDay?: (day: number, changes: Partial<DraftDay>) => void }) {
  const [day, setDay] = useState(0);
  const [selectedRouteDay, setSelectedRouteDay] = useState(0);
  const [routeTotals, setRouteTotals] = useState<{ distance: number; duration: number } | null>(null);
  const [variant, setVariant] = useState<"rail" | "tabs" | "feed">("rail");
  useEffect(() => setDay((current) => Math.min(current, Math.max(0, draftDays.length - 1))), [draftDays.length]);
  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    const legs = draftDays.flatMap((day) => day.roadLeg ? [[mapLocation(day.roadLeg.from), mapLocation(day.roadLeg.to)]] : []).filter((leg): leg is [[number, number], [number, number]] => Boolean(leg[0] && leg[1]));
    if (!token || !legs.length) {
      setRouteTotals(null);
      return;
    }
    let cancelled = false;
    void Promise.all(legs.map(async ([from, to]) => {
      const response = await fetch(`https://api.mapbox.com/directions/v5/mapbox/driving/${from.join(",")};${to.join(",")}?overview=false&access_token=${token}`);
      const data = await response.json() as { routes?: { distance: number; duration: number }[] };
      return data.routes?.[0];
    })).then((routes) => {
      const validRoutes = routes.filter((route): route is { distance: number; duration: number } => Boolean(route));
      if (cancelled || validRoutes.length !== legs.length) return;
      setRouteTotals(validRoutes.reduce<{ distance: number; duration: number }>((total, route) => ({ distance: total.distance + route.distance, duration: total.duration + route.duration }), { distance: 0, duration: 0 }));
    }).catch(() => { if (!cancelled) setRouteTotals(null); });
    return () => { cancelled = true; };
  }, [draftDays]);
  const currentDraftDay: DraftDay = draftDays[day] || { id: "day-1", places: [] };
  if (isDraft) return <div className="draft-route-with-map"><div className="draft-route-cards"><div className="route-toolbar"><span>Планирование по дням · добавляйте автопереезды и дорожные заметки</span></div>{draftDays.map((draftDay, index) => <DraftRouteCard day={draftDay} index={index} editing={editingRoadDay === index} selected={selectedRouteDay === index} onSelect={() => setSelectedRouteDay(index)} onEdit={() => onEditingRoadDayChange?.(index)} onChange={(roadLeg) => onUpdateDraftDay?.(index, { roadLeg })} onSave={(roadLeg) => { onUpdateDraftDay?.(index, { roadLeg }); onEditingRoadDayChange?.(null); }} onCancel={() => onEditingRoadDayChange?.(null)} key={draftDay.id} />)}<button className="add-route-day" onClick={onAddDraftDay}>＋ Добавить день</button></div><aside className="map-card"><TripMap routeDays={draftDays} activeDay={selectedRouteDay} /><footer><span>Общий маршрут</span><b>{draftDays.length} дней{routeTotals && ` · ${Math.round(routeTotals.distance / 1000).toLocaleString("ru-RU")} км · ${Math.round(routeTotals.duration / 3600)} ч`}</b></footer></aside></div>;
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

function Budget() {
  const cats = [
    ["Жильё", 64, "67 100 ₽"],
    ["Транспорт", 41, "43 300 ₽"],
    ["Еда и рестораны", 36, "38 000 ₽"],
    ["Активности и билеты", 20, "21 500 ₽"],
    ["Прочее", 8, "6 800 ₽"],
  ] as const;
  return (
    <div className="budget">
      <div className="budget-cards">
        <article>
          <span>Общий бюджет</span>
          <b>240 000 ₽</b>
        </article>
        <article className="accent-card">
          <span>Запланировано</span>
          <b>176 700 ₽</b>
          <small>73% бюджета</small>
        </article>
        <article>
          <span>Осталось</span>
          <b>63 300 ₽</b>
          <small>≈ 21 100 ₽ / чел.</small>
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
          <p>Поровну между 3 участниками</p>
          {[
            ["АС", "Анна", "+ 39 600 ₽"],
            ["МК", "Максим", "− 6 600 ₽"],
            ["ДВ", "Дарья", "− 33 000 ₽"],
          ].map((item) => (
            <div className="split" key={item[1]}>
              <Avatar>{item[0]}</Avatar>
              <span>
                <b>{item[1]}</b>
                <small>оплачено участником</small>
              </span>
              <b>{item[2]}</b>
            </div>
          ))}
          <button className="send-reminders">Отправить напоминания</button>
        </article>
      </div>
    </div>
  );
}

function Photos() {
  return (
    <div>
      <SectionHead title="Фотоальбом" />
      <p className="lead">48 фото · снимки всех участников поездки</p>
      <div className="chips">
        <button className="selected">Все · 48</button>
        <button>Рим · 21</button>
        <button>Флоренция · 14</button>
        <button>Венеция · 13</button>
      </div>
      <div className="photo-grid">
        {Array.from({ length: 10 }, (_, index) => (
          <div
            className={`photo p${index % 6} ${index === 0 ? "hero-photo" : ""}`}
            key={index}
          >
            {index === 0 && <span>Колизей · 13 сен</span>}
          </div>
        ))}
        <button className="photo-add">
          ＋<small>Добавить</small>
        </button>
      </div>
    </div>
  );
}

function Members() {
  const people: [string, string, string, string, "sand" | "green" | "blue"][] =
    [
      ["АС", "Анна Соколова", "anna@mail.ru", "Владелец", "sand"],
      ["МК", "Максим Крылов", "maxim@mail.ru", "Редактор", "green"],
      ["ДВ", "Дарья Волкова", "darya@mail.ru", "Читатель", "blue"],
    ];
  return (
    <div className="members">
      <article className="panel">
        {people.map(([initials, name, email, role, tone]) => (
          <div className="member" key={name}>
            <Avatar tone={tone}>{initials}</Avatar>
            <span>
              <b>{name}</b>
              <small>{email}</small>
            </span>
            <button>{role}⌄</button>
          </div>
        ))}
        <div className="invite">
          <input placeholder="e-mail нового участника" />
          <button>Редактор⌄</button>
          <button className="accent">Пригласить</button>
        </div>
      </article>
      <article className="panel public-link">
        <h2>
          Публичная ссылка <i />
        </h2>
        <p>
          Любой, у кого есть ссылка, может просматривать маршрут без прав на
          редактирование.
        </p>
        <div>
          <code>odyssey.travel/p/italy-8d-a1b2</code>
          <button>Копировать</button>
        </div>
        <div className="public-catalog">
          <span>
            <b>Опубликовать в каталоге</b>
            <small>Другие смогут найти и скопировать ваш маршрут</small>
          </span>
          <button>Опубликовать</button>
        </div>
      </article>
    </div>
  );
}

const winterCities = [
  { name: "Мюнхен", latitude: 48.1374, longitude: 11.5755 },
  { name: "Верона", latitude: 45.4384, longitude: 10.9916 },
  { name: "Рим", latitude: 41.9028, longitude: 12.4964 },
  { name: "Кьоджа", latitude: 45.2186, longitude: 12.2789 },
  { name: "Венеция", latitude: 45.4408, longitude: 12.3155 },
  { name: "Милан", latitude: 45.4642, longitude: 9.19 },
  { name: "Равенсбург", latitude: 47.781, longitude: 9.612 },
  { name: "Прага", latitude: 50.0755, longitude: 14.4378 },
];

const weatherDescription = (code: number) => {
  if (code === 0) return "Ясно";
  if (code <= 3) return "Облачно";
  if (code <= 48) return "Туман";
  if (code <= 67) return "Дождь";
  if (code <= 77) return "Снег";
  return "Ливень";
};

function WeatherOverview() {
  const [mode, setMode] = useState<"now" | "trip">("now");
  const [weather, setWeather] = useState<Record<string, { temperature: number; code: number }>>({});
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let cancelled = false;
    const latitude = winterCities.map((city) => city.latitude).join(",");
    const longitude = winterCities.map((city) => city.longitude).join(",");
    void fetch(`https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,weather_code&temperature_unit=celsius`).then(async (response) => {
      if (!response.ok) throw new Error("Weather request failed");
      return response.json() as Promise<{ current: { temperature_2m: number; weather_code: number } }[]>;
    }).then((data) => {
      const entries = data.map((item, index) => [winterCities[index].name, { temperature: item.current.temperature_2m, code: item.current.weather_code }] as const);
      if (!cancelled) setWeather(Object.fromEntries(entries));
    }).catch(() => {
      if (!cancelled) setFailed(true);
    });
    return () => { cancelled = true; };
  }, []);

  return <section className="weather-overview">
    <header className="overview-section-head weather-heading">
      <div><h2>Погода по маршруту</h2><p>{mode === "now" ? "Текущая погода в городах поездки" : "19 декабря 2026 - 3 января 2027"}</p></div>
      <div className="weather-switch" role="group" aria-label="Период погоды"><button className={mode === "now" ? "active" : ""} onClick={() => setMode("now")}>Сейчас</button><button className={mode === "trip" ? "active" : ""} onClick={() => setMode("trip")}>На даты поездки</button></div>
    </header>
    {mode === "trip" && <p className="weather-notice">Точный прогноз для поездки появится 3 декабря 2026 года, примерно за 16 дней до выезда.</p>}
    <div className="weather-grid">
      {winterCities.map((city) => {
        const current = weather[city.name];
        return <article className="weather-card" key={city.name}><h3>{city.name}</h3>{mode === "now" ? failed ? <p>Не удалось обновить погоду</p> : current ? <><b>{Math.round(current.temperature)}°C</b><span>{weatherDescription(current.code)}</span></> : <p>Обновляем...</p> : <><b>19 дек - 3 янв</b><span>Прогноз появится позже</span></>}</article>;
      })}
    </div>
  </section>;
}

function TripOverview({ trip, onUpdateTrip }: { trip: TripSummary; onUpdateTrip: (trip: TripSummary) => void }) {
  const photoInputRef = useRef<HTMLInputElement>(null);
  const [activePhoto, setActivePhoto] = useState(0);
  const [draggedPhoto, setDraggedPhoto] = useState<number | null>(null);
  const isWinterRoute = trip.title.toLowerCase().includes("рождествен") || trip.cities.includes("Мюнхен") || trip.cities.includes("Прага");
  const coverPhotos = (trip.coverPhotos?.length ? trip.coverPhotos : trip.coverImage ? [{ id: "legacy-cover", image: trip.coverImage, city: trip.coverCity, description: trip.coverDescription }] : []).filter((photo) => photo.id !== "verona-cover").map((photo, index) => {
    const caption = isWinterRoute ? winterPhotoCaptions[index] : undefined;
    return { ...photo, city: photo.city || caption?.[0], description: photo.description || caption?.[1] };
  });
  const activeCover = coverPhotos[Math.min(activePhoto, Math.max(0, coverPhotos.length - 1))];
  const uploadCoverPhoto = async (file: Blob, extension: string) => {
    const { data: { session } } = await supabase.auth.getSession();
    if (!session) throw new Error("No active session");
    const path = `${session.user.id}/${trip.id}/${crypto.randomUUID()}.${extension}`;
    const { error } = await supabase.storage.from("trip-photos").upload(path, file, { cacheControl: "31536000", upsert: false, contentType: file.type || "image/jpeg" });
    if (error) throw error;
    return supabase.storage.from("trip-photos").getPublicUrl(path).data.publicUrl;
  };
  const addCoverPhotos = async (files: FileList | null) => {
    if (!files?.length) return;
    try {
      const uploadedPhotos = await Promise.all(Array.from(files).map(async (file) => ({ id: crypto.randomUUID(), image: await uploadCoverPhoto(file, file.name.split(".").pop()?.toLowerCase() || "jpg") })));
      const nextPhotos = [...coverPhotos, ...uploadedPhotos];
      onUpdateTrip({ ...trip, coverImage: nextPhotos[0]?.image, coverPhotos: nextPhotos });
      setActivePhoto(nextPhotos.length - uploadedPhotos.length);
    } catch {
      window.alert("Не удалось загрузить фотографию. Попробуйте файл JPG, PNG или WebP до 10 МБ.");
    }
  };
  useEffect(() => {
    const localPhotos = coverPhotos.filter((photo) => photo.image.startsWith("data:image/"));
    if (!localPhotos.length) return;
    let cancelled = false;
    void Promise.all(coverPhotos.map(async (photo) => {
      if (!photo.image.startsWith("data:image/")) return photo;
      const file = await fetch(photo.image).then((response) => response.blob());
      const extension = file.type.split("/")[1] || "jpg";
      return { ...photo, image: await uploadCoverPhoto(file, extension) };
    })).then((migratedPhotos) => {
      if (!cancelled) onUpdateTrip({ ...trip, coverImage: migratedPhotos[0]?.image, coverPhotos: migratedPhotos });
    }).catch(() => undefined);
    return () => { cancelled = true; };
  }, [trip.id]);
  const reorderCoverPhotos = (_from: number, _to: number) => undefined;
  if (trip.isDraft) return <div className="trip-overview"><div className="overview-draft"><div className="cover-photo-stack"><section className={activeCover ? "has-draft-cover" : ""} style={activeCover ? { backgroundImage: `linear-gradient(rgba(27, 28, 31, 0.3), rgba(27, 28, 31, 0.3)), url(${activeCover.image})` } : undefined}><input ref={photoInputRef} className="cover-file-input" type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={(event) => void addCoverPhotos(event.target.files)} />{activeCover ? <><button type="button" className="cover-arrow previous" onClick={() => setActivePhoto((activePhoto - 1 + coverPhotos.length) % coverPhotos.length)} disabled={coverPhotos.length < 2} aria-label="Предыдущее фото">‹</button><button type="button" className="cover-arrow next" onClick={() => setActivePhoto((activePhoto + 1) % coverPhotos.length)} disabled={coverPhotos.length < 2} aria-label="Следующее фото">›</button><button type="button" className="add-cover-photo" onClick={() => photoInputRef.current?.click()}>＋ Фото</button>{activeCover.city && <div className="cover-photo-caption"><b>{activeCover.city}</b>{activeCover.description && <span>{activeCover.description}</span>}</div>}</> : <><p>ГЛАВНАЯ</p><h2>Начните планировать путешествие</h2><span>Добавьте первую фотографию путешествия.</span><button type="button" className="add-cover-photo" onClick={() => photoInputRef.current?.click()}>＋ Фото</button></>}</section>{coverPhotos.length > 1 && <div className="cover-order"><p>Перетащите фото в порядке городов маршрута</p><div>{coverPhotos.map((photo, index) => <button className={`${index === activePhoto ? "active" : ""} ${index === draggedPhoto ? "dragging" : ""}`} style={{ backgroundImage: `url(${photo.image})` }} draggable onDragStart={() => setDraggedPhoto(index)} onDragEnd={() => setDraggedPhoto(null)} onDragOver={(event) => event.preventDefault()} onDrop={() => { if (draggedPhoto !== null) reorderCoverPhotos(draggedPhoto, index); setDraggedPhoto(null); }} onClick={() => setActivePhoto(index)} aria-label={photo.city || `Фото ${index + 1}`} key={photo.id}><span>{photo.city || index + 1}</span></button>)}</div></div>}</div><aside className="map-card"><TripMap /><footer><span>Общий маршрут</span><b>0 городов</b></footer></aside></div>{isWinterRoute && <WeatherOverview />}</div>;
  const cities = [
    { name: "Рим", dates: "12–14 сентября", weather: "22°C · ясно", image: "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=900&q=80" },
    { name: "Флоренция", dates: "15–16 сентября", weather: "24°C · солнечно", image: "https://images.unsplash.com/photo-1544986581-efac024faf62?auto=format&fit=crop&w=900&q=80" },
    { name: "Венеция", dates: "17–19 сентября", weather: "20°C · облачно", image: "https://images.unsplash.com/photo-1514890547357-a9ee288728e0?auto=format&fit=crop&w=900&q=80" },
  ];
  return <div className="trip-overview"><section className="overview-route"><span>ОБЩИЙ МАРШРУТ</span><h2>Москва <b>→</b> Рим <b>→</b> Флоренция <b>→</b> Венеция</h2><p>12–19 сентября 2026 · 8 дней · 3 города</p></section><section><div className="overview-section-head"><div><h2>Города поездки</h2><p>Прогноз предварительный</p></div></div><div className="city-overview-grid">{cities.map((city) => <article className="city-overview-card" key={city.name}><img src={city.image} alt={city.name} /><div><h3>{cityFlag(city.name)} {city.name}</h3><p>{city.dates}</p><b>{city.weather}</b></div></article>)}</div></section></div>;
}

function WalkingMap({ sights, city, activeSightId }: { sights: StoredSight[]; city?: string; activeSightId?: string }) {
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const markerElements = useRef(new globalThis.Map<string, HTMLSpanElement>());
  const [stats, setStats] = useState<{ distance: number; duration: number } | null>(null);
  const routeKey = sights.map((sight) => `${sight.id}:${sight.lnglat?.join(",") || ""}`).join(";");
  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_ACCESS_TOKEN;
    const coordinates = sights.map((sight) => sight.lnglat).filter((coordinate): coordinate is [number, number] => Boolean(coordinate));
    const fallbackLocation = city ? mapLocation(city) : undefined;
    if (!container.current || !token || (!coordinates.length && !fallbackLocation)) return;
    if (coordinates.length > 1) {
      const path = coordinates.map((coordinate) => coordinate.join(",")).join(";");
      void fetch(`https://api.mapbox.com/directions/v5/mapbox/walking/${path}?overview=false&access_token=${token}`).then((response) => response.json()).then((data: { routes?: { distance: number; duration: number }[] }) => setStats(data.routes?.[0] || null)).catch(() => setStats(null));
    } else {
      setStats(null);
    }
    let map: Map | undefined;
    let disposed = false;
    void import("mapbox-gl").then(({ default: mapboxgl }) => {
      if (disposed || !container.current) return;
      mapboxgl.accessToken = token;
      map = new mapboxgl.Map({ container: container.current, style: "mapbox://styles/mapbox/streets-v12", center: coordinates[0] || fallbackLocation!, zoom: coordinates.length ? 13 : 11, attributionControl: false });
      mapRef.current = map;
      markerElements.current.clear();
      map.addControl(new mapboxgl.NavigationControl({ showCompass: false }), "top-right");
      sights.forEach((sight, index) => {
        if (!sight.lnglat) return;
        const marker = document.createElement("span");
        marker.className = "sight-map-marker";
        marker.textContent = String(index + 1);
        markerElements.current.set(sight.id, marker);
        new mapboxgl.Marker({ element: marker }).setLngLat(sight.lnglat).addTo(map!);
      });
      map.on("load", () => {
        if (coordinates.length > 1) {
          map!.addSource("walking-route", { type: "geojson", data: { type: "Feature", properties: {}, geometry: { type: "LineString", coordinates } } });
          map!.addLayer({ id: "walking-route", type: "line", source: "walking-route", paint: { "line-color": "#ef7b48", "line-width": 4, "line-opacity": 0.9 } });
        }
        const bounds = new mapboxgl.LngLatBounds(coordinates[0], coordinates[0]);
        coordinates.slice(1).forEach((coordinate) => bounds.extend(coordinate));
        map!.fitBounds(bounds, { padding: 38, maxZoom: 14 });
      });
    });
    return () => { disposed = true; map?.remove(); mapRef.current = null; markerElements.current.clear(); };
  }, [routeKey, city]);
  useEffect(() => {
    if (!activeSightId) return;
    const marker = markerElements.current.get(activeSightId);
    const sight = sights.find((item) => item.id === activeSightId);
    if (!marker || !sight?.lnglat) return;
    marker.classList.remove("bounce");
    void marker.offsetWidth;
    marker.classList.add("bounce");
    mapRef.current?.flyTo({ center: sight.lnglat, zoom: 15, duration: 700, essential: true });
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
      mapRef.current?.flyTo({ center: sight.lnglat, zoom: 15, duration: 700, essential: true });
    };
    window.addEventListener("odyssey-focus-sight", focusSight);
    return () => window.removeEventListener("odyssey-focus-sight", focusSight);
  }, [sights]);
  const hours = stats ? Math.floor(stats.duration / 3600) : 0;
  const minutes = stats ? Math.round((stats.duration % 3600) / 60) : 0;
  return <div className="walking-map-wrap"><div className="walking-map" ref={container} /><footer><span>Пеший маршрут</span><b>{stats ? `${(stats.distance / 1000).toLocaleString("ru-RU", { maximumFractionDigits: 1 })} км · ${hours ? `${hours} ч ` : ""}${minutes} мин` : "Добавьте минимум 2 точки"}</b></footer></div>;
}

function Sights({ sights, days, defaultCity, onToggle, onAdd, onAddDay, onRenameDay }: { sights: StoredSight[]; days: { id: string; title: string }[]; defaultCity?: string; onToggle: (id: string) => void; onAdd: (sight: StoredSight) => void; onAddDay: (title: string) => void; onRenameDay: (id: string, title: string) => void }) {
  const [adding, setAdding] = useState(false);
  const [addingDay, setAddingDay] = useState(false);
  const [selectedDay, setSelectedDay] = useState(() => Number(localStorage.getItem("odyssey-selected-sight-day") || 0));
  useEffect(() => {
    if (selectedDay >= days.length) { setSelectedDay(0); return; }
    localStorage.setItem("odyssey-selected-sight-day", String(selectedDay));
    const dayId = days[selectedDay]?.id;
    if (dayId) window.dispatchEvent(new CustomEvent("odyssey-select-sight-day", { detail: dayId }));
  }, [selectedDay, days]);
  const cities = Array.from(new Set(sights.map((sight) => sight.city))).sort();
  if (!cities.length && defaultCity) cities.push(defaultCity);
  const [city, setCity] = useState(cities[0] || "");
  const citySights = sights.filter((sight) => sight.city === city);
  const walkDays = Array.from(new Set(citySights.map((sight) => sight.walkDay || 1))).sort((a, b) => a - b);
  const [walkDay, setWalkDay] = useState(walkDays[0] || 1);
  useEffect(() => {
    const dayCity = days[selectedDay]?.title;
    if (dayCity) setCity(dayCity);
    setWalkDay(selectedDay + 1);
  }, [selectedDay, days]);
  const routeSights = sights.filter((sight) => (sight.walkDay || 1) === selectedDay + 1).sort((a, b) => (a.walkOrder || 0) - (b.walkOrder || 0));
  const addSight = (event: FormEvent<HTMLFormElement>) => { event.preventDefault(); const form = new FormData(event.currentTarget); const name = String(form.get("name") || "").trim(); const placeCity = String(form.get("city") || city).trim(); if (!name || !placeCity) return; onAdd({ id: crypto.randomUUID(), name, city: placeCity, walkDay, walkOrder: routeSights.length }); setAdding(false); };
  const featuredSource = routeSights.find((sight) => sight.id === "munich-christkindlmarkt" || sight.id === "verona-signori") || routeSights[0];
  const featured = featuredSource?.id === "munich-christkindlmarkt" ? { ...featuredSource, name: "Marienplatz (Christkindlmarkt)" } : featuredSource;
  useEffect(() => {
    document.documentElement.dataset.emptySightDay = featured ? "false" : "true";
    document.documentElement.dataset.sightCity = city;
    document.documentElement.dataset.sightDay = String(selectedDay + 1);
    return () => { delete document.documentElement.dataset.emptySightDay; delete document.documentElement.dataset.sightCity; delete document.documentElement.dataset.sightDay; };
  }, [Boolean(featured), city, selectedDay]);
  return <section className="sights-page"><header className="sights-heading"><div><p className="eyebrow">{city || "Путешествие"} · День {selectedDay + 1}</p><h2>Места дня</h2></div><div className="sights-view"><button className="active">Журнал</button><button>Карта</button></div></header><div className="sight-day-tabs">{days.map((day, index) => <div className={selectedDay === index ? "sight-day active" : "sight-day"} key={day.id}><button onClick={() => { setSelectedDay(index); setCity(day.title); setWalkDay(index + 1); }}><small>День {index + 1}</small><b>{day.title}</b></button><button className="rename-day" onClick={() => { const title = window.prompt("Название дня", day.title)?.trim(); if (title) onRenameDay(day.id, title); }}>✎</button></div>)}{addingDay ? <form className="add-sight-day" onSubmit={(event) => { event.preventDefault(); const title = String(new FormData(event.currentTarget).get("title") || "").trim(); if (!title) return; onAddDay(title); setAddingDay(false); }}><input name="title" placeholder="Например, Рим" autoFocus /><button className="accent">Добавить</button></form> : <button className="add-sight-day" onClick={() => setAddingDay(true)}>＋ День</button>}</div><section className="sight-feature"><span>★ Место дня</span><div><p>{featured ? featured.city : "Ваш маршрут"}</p><h3>{featured ? featured.name : "Добавьте первое место"}</h3><small>{featured ? "Откройте карточку, чтобы добавить детали и время посещения." : "Соберите собственный список достопримечательностей для этого дня."}</small><button className="accent" onClick={() => setAdding(true)}>＋ Добавить в маршрут</button></div></section><section className="walking-planner"><header><div><b>Карта прогулки</b><p>Точки дня и их порядок будут показаны на карте.</p></div><span>{routeSights.length} мест</span></header><div className="walking-layout"><WalkingMap sights={routeSights} city={city} /><div className="walking-points"><h3>Список мест</h3><ol className="walking-list">{routeSights.length ? routeSights.map((sight, index) => <li key={sight.id}><button onClick={() => onToggle(sight.id)} className={sight.done ? "done" : ""}><b>{index + 1}</b><span>{sight.name}</span><small>{sight.done ? "Посещено" : "Отметить"}</small></button></li>) : <li className="walking-empty">{adding ? <form onSubmit={addSight}><input name="name" placeholder="Название места" autoFocus /><input name="city" placeholder="Город" defaultValue={city} /><button className="accent">Добавить</button></form> : <button onClick={() => setAdding(true)}><b>＋</b><span>Добавить первую точку</span></button>}</li>}</ol></div></div></section><section className="sights-collection"><header><div><p className="eyebrow">Ещё рядом</p><h2>Места дня</h2></div><span>{routeSights.length} точек</span></header>{routeSights.length ? <div className="sights-grid">{routeSights.map((sight, index) => <article className={sight.done ? "sight-card visited" : "sight-card"} key={sight.id}>{sight.photo && <img src={sight.photo} alt="" />}<div><b className="sight-number">{index + 1}</b><p>{sight.subcategory || sight.group || "Достопримечательность"}</p><h3>{sight.name}</h3><label><input type="checkbox" checked={sight.done || false} onChange={() => onToggle(sight.id)} />{sight.done ? "Посещено" : "Отметить посещение"}</label></div></article>)}</div> : <div className="sights-empty"><b>День пока свободен</b><p>Добавьте места, которые хотите посетить.</p></div>}</section></section>;
}

function SightNotes({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  return <section className="sight-notes"><div><span>✎</span><div><h3>Заметки</h3><p>Адреса, билеты, идеи и всё, что пригодится в прогулке.</p></div></div><textarea value={value} onChange={(event) => onChange(event.target.value)} placeholder="Например: купить билеты заранее, прийти к открытию..." /></section>;
}

function Workspace({ go, trip, onUpdateTrip }: { go: (view: View) => void; trip: TripSummary; onUpdateTrip: (trip: TripSummary) => void }) {
  const [tab, setTab] = useState<Tab>(() => (localStorage.getItem("odyssey-trip-tab") as Tab | null) || "overview");
  const [editingRoadDay, setEditingRoadDay] = useState<number | null>(null);
  const [selectedSightDayId, setSelectedSightDayId] = useState("sights-day-1");
  const draftDays = trip.days?.length ? trip.days : [{ id: "day-1", places: trip.places || [] }];
  const firstDraftDay = draftDays[0];
  const isChristmasTrip = trip.isDraft || trip.title.toLowerCase().includes("рождествен");
  const savedSightDays = trip.sightDaysVersion === 1 && trip.sightDays?.length ? trip.sightDays : [{ id: "sights-day-1", title: firstDraftDay.roadLeg?.to || firstDraftDay.roadLeg?.from || "Первый день" }];
  const sightDays = isChristmasTrip && savedSightDays.length === 3 && savedSightDays[2].title === "Рим"
    ? [...savedSightDays, { id: "sights-day-4", title: "Рим" }, { id: "sights-day-5", title: "Рим" }, { id: "sights-day-6", title: "Сан-Марино" }]
    : isChristmasTrip && savedSightDays.length === 5 && savedSightDays[4].title === "Рим"
      ? [...savedSightDays, { id: "sights-day-6", title: "Сан-Марино" }]
    : isChristmasTrip && savedSightDays.length === 4 && savedSightDays[3].title === "Рим"
      ? [...savedSightDays, { id: "sights-day-5", title: "Рим" }, { id: "sights-day-6", title: "Сан-Марино" }]
    : isChristmasTrip && savedSightDays.length === 1 && savedSightDays[0].id === "sights-day-1"
      ? [...savedSightDays, { id: "sights-day-2", title: "Верона" }]
      : savedSightDays;
  useEffect(() => {
    const index = Math.min(Number(localStorage.getItem("odyssey-selected-sight-day") || 0), sightDays.length - 1);
    setSelectedSightDayId(sightDays[index]?.id || sightDays[0].id);
  }, [sightDays]);
  useEffect(() => {
    const selectDay = (event: Event) => setSelectedSightDayId((event as CustomEvent<string>).detail);
    window.addEventListener("odyssey-select-sight-day", selectDay);
    return () => window.removeEventListener("odyssey-select-sight-day", selectDay);
  }, []);
  useEffect(() => {
    if (trip.sightNotes?.["sights-day-2"] !== legacyVeronaDayTwoNotes) return;
    onUpdateTrip({ ...trip, sightNotes: { ...trip.sightNotes, "sights-day-2": veronaDayTwoNotes } });
  }, [trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (selectedDay?.title !== "Верона" || trip.sightNotes?.[selectedDay.id]) return;
    onUpdateTrip({ ...trip, sightNotes: { ...trip.sightNotes, [selectedDay.id]: veronaDayTwoNotes } });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (selectedDay?.title !== "Рим" || trip.sightNotes?.[selectedDay.id]) return;
    onUpdateTrip({ ...trip, sightNotes: { ...trip.sightNotes, [selectedDay.id]: romeDayThreeNotes } });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (selectedDay?.title !== "Пиза" || trip.sightNotes?.[selectedDay.id]) return;
    onUpdateTrip({ ...trip, sightNotes: { ...trip.sightNotes, [selectedDay.id]: pisaDaySixNotes } });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  useEffect(() => {
    const selectedDay = sightDays.find((day) => day.id === selectedSightDayId);
    if (selectedDay?.title !== "Сан-Марино" || trip.sightNotes?.[selectedDay.id] !== pisaDaySixNotes) return;
    onUpdateTrip({ ...trip, sightNotes: { ...trip.sightNotes, [selectedDay.id]: "" } });
  }, [selectedSightDayId, sightDays, trip, onUpdateTrip]);
  const defaultChristmasSights = [...munichDayOneSights, ...veronaDayTwoSights, ...romeDayThreeSights, ...romeDayFourSights, ...romeDayFiveSights, ...sanMarinoDaySixSights];
  const tripSights = isChristmasTrip
    ? [...defaultChristmasSights.map((sight) => ({ ...sight, done: trip.sights?.find((saved) => saved.id === sight.id)?.done })), ...(trip.sights || []).filter((sight) => !defaultChristmasSights.some((defaultSight) => defaultSight.id === sight.id) && !(sight.walkDay === 6 && sight.city === "Пиза"))]
    : trip.sights || [];
  const labels: [Tab, string][] = trip.isDraft ? [["overview", "Главная"], ["route", "Маршрут"], ["sights", "Достопримечательности"]] : [
    ["overview", "Главная"],
    ["route", "Маршрут"],
    ["bookings", "Жильё и транспорт"],
    ["budget", "Бюджет"],
    ["photos", "Фото"],
    ["members", "Участники"],
  ];
  return (
    <div>
      <header className="trip-header">
        <button className="back back-icon" onClick={() => go("trips")} aria-label="На главную" title="На главную">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M19 12H5m0 0 6-6m-6 6 6 6" /></svg>
        </button>
        <div className="trip-heading">
          <div>
            <h1>
              {trip.title} <span>● {trip.status}</span>
            </h1>
            <p>{trip.isDraft ? (trip.cities || "Даты, города и маршрут пока не заполнены") : trip.dates}</p>
          </div>
          {!trip.isDraft && <div className="share">
            <div>
              <Avatar>АС</Avatar>
              <Avatar tone="green">МК</Avatar>
              <Avatar tone="blue">ДВ</Avatar>
            </div>
            <button onClick={() => go("public")}>↗ Публичная ссылка</button>
          </div>}
        </div>
        <nav className="tabs">
          {labels.map(([value, label]) => (
            <button
              className={tab === value ? "active" : ""}
              onClick={() => { setTab(value); localStorage.setItem("odyssey-trip-tab", value); }}
              key={value}
            >
              {label}
            </button>
          ))}
        </nav>
      </header>
      <main className="workspace">
        {tab === "overview" && <TripOverview trip={trip} onUpdateTrip={onUpdateTrip} />}
        {tab === "route" && <RouteTab isDraft={trip.isDraft} draftDays={draftDays} editingRoadDay={editingRoadDay} onEditingRoadDayChange={setEditingRoadDay} onAddDraftDay={() => onUpdateTrip({ ...trip, places: undefined, days: [...draftDays, { id: crypto.randomUUID(), places: [] }] })} onUpdateDraftDay={(day, changes) => onUpdateTrip({ ...trip, places: undefined, days: draftDays.map((item, index) => index === day ? { ...item, ...changes } : item) })} />}
        {tab === "sights" && <><Sights sights={tripSights} days={sightDays} defaultCity={trip.cities.split(",")[0]?.trim()} onToggle={(id) => { const sight = tripSights.find((item) => item.id === id); const isCheckbox = document.activeElement instanceof HTMLInputElement && document.activeElement.type === "checkbox"; if (isCheckbox) { onUpdateTrip({ ...trip, sights: tripSights.map((item) => item.id === id ? { ...item, done: !item.done } : item) }); return; } if (!sight) return; window.dispatchEvent(new CustomEvent("odyssey-focus-sight", { detail: id })); const query = sight.lnglat ? `${sight.lnglat[1]},${sight.lnglat[0]}` : `${sight.name}, ${sight.city}`; window.open(`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`, "_blank", "noopener,noreferrer"); }} onAdd={(sight) => onUpdateTrip({ ...trip, sights: [...tripSights, sight] })} onAddDay={(title) => onUpdateTrip({ ...trip, sightDaysVersion: 1, sightDays: [...sightDays, { id: crypto.randomUUID(), title }] })} onRenameDay={(id, title) => onUpdateTrip({ ...trip, sightDaysVersion: 1, sightDays: sightDays.map((day) => day.id === id ? { ...day, title } : day) })} /><SightNotes value={trip.sightNotes?.[selectedSightDayId] || (selectedSightDayId === "sights-day-1" && trip.title.toLowerCase().includes("рождествен") ? munichDayOneNotes : selectedSightDayId === "sights-day-2" ? veronaDayTwoNotes : "")} onChange={(value) => onUpdateTrip({ ...trip, sightNotes: { ...trip.sightNotes, [selectedSightDayId]: value } })} /></>}
        {tab === "bookings" && <Bookings />}
        {tab === "budget" && <Budget />}
        {tab === "photos" && <Photos />}
        {tab === "members" && <Members />}
      </main>
    </div>
  );
}

function Catalog({ go }: { go: (view: View) => void }) {
  const [filter, setFilter] = useState("Все");
  const [query, setQuery] = useState("");
  const filters = ["Все", "Европа", "Азия", "Города", "Природа", "7–10 дней"];
  const matchesFilter = (item: (typeof catalog)[number]) => {
    if (filter === "Все") return true;
    if (filter === "Европа" || filter === "Города") return item[0].includes("Италия");
    if (filter === "7–10 дней") return item[2] === "8 дней";
    return false;
  };
  const filteredCatalog = catalog.filter((item) => `${item[0]} ${item[1]}`.toLowerCase().includes(query.toLowerCase()) && matchesFilter(item));
  return (
    <div className="page wide">
      <p className="eyebrow">Сообщество путешественников</p>
      <h1>Каталог маршрутов</h1>
      <div className="search">
        ⌕<input placeholder="Куда хотите поехать?" value={query} onChange={(event) => setQuery(event.target.value)} />
      </div>
      <div className="chips">
        {filters.map((value) => <button className={filter === value ? "selected" : ""} onClick={() => setFilter(value)} key={value}>{value}</button>)}
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
        {filteredCatalog.length === 0 && <div className="empty-state">По этому запросу маршрутов не найдено.</div>}
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

function Auth({ go, onAuthorized }: { go: (view: View) => void; onAuthorized: (name: string) => void }) {
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
      const { data, error } = await supabase.auth.signUp({
        email,
        password,
        options: { data: { full_name: name } },
      });
      if (error) {
        setMessage(error.message);
        return;
      }
      if (!data.session) {
        setMessage("Аккаунт создан. Подтвердите e-mail, затем войдите.");
        setMode("login");
        return;
      }
      localStorage.setItem("odyssey-remember-me", "true");
      onAuthorized(name);
      setMessage("Аккаунт создан. Открываем ваши путешествия...");
      window.setTimeout(() => go("trips"), 500);
      return;
    }

    const { data, error } = await supabase.auth.signInWithPassword({ email, password });
    if (error || !data.user) {
      setMessage(error?.message ?? "Не удалось войти. Проверьте e-mail и пароль.");
      return;
    }
    localStorage.setItem("odyssey-remember-me", String(rememberMe));
    onAuthorized(data.user.user_metadata.full_name || data.user.email || "Путешественник");
    setMessage("Вход выполнен. Открываем ваши путешествия...");
    window.setTimeout(() => go("trips"), 500);
  };
  return (
    <main className="auth-page">
      <section className="auth-card">
        <div className="auth-form">
          <div className="auth-brand">
            <span>O</span>
            <b>Одиссея</b>
          </div>
          <div className="auth-switch">
            <button
              className={isRegister ? "active" : ""}
              onClick={() => { setMode("register"); setMessage(""); }}
            >
              Регистрация
            </button>
            <button
              className={!isRegister ? "active" : ""}
              onClick={() => { setMode("login"); setMessage(""); }}
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
          <form onSubmit={handleSubmit}>
            <label className={isRegister ? "" : "hidden"}>
              Имя
              <input name="name" placeholder="Введите имя" autoComplete="name" />
            </label>
            <label>
              E-mail
              <input name="email" type="email" placeholder="you@example.com" />
            </label>
            <label>
              Пароль
              <input
                name="password"
                type="password"
                placeholder={
                  isRegister ? "Минимум 8 символов" : "Введите пароль"
                }
              />
            </label>
            {isRegister && (
              <label className="terms">
                <input name="terms" type="checkbox" defaultChecked />{" "}
                <span>
                  Я принимаю <a href="#terms">условия использования</a> и{" "}
                  <a href="#privacy">политику конфиденциальности</a>
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
          {message && <p className="auth-message" role="status">{message}</p>}
          <div className="auth-footer">
            {isRegister ? "Уже есть аккаунт?" : "Впервые в Одиссее?"}{" "}
            <button onClick={() => { setMode(isRegister ? "login" : "register"); setMessage(""); }}>
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

export function App() {
  const [view, setView] = useState<View>("auth");
  const [menu, setMenu] = useState(false);
  const [storedPayload, setStoredPayload] = useState<StoredTripPayload | null>(null);
  const [drafts, setDrafts] = useState<TripSummary[]>(() => {
    try {
      return JSON.parse(localStorage.getItem("odyssey-drafts") || "[]") as TripSummary[];
    } catch {
      return [];
    }
  });
  const [activeTrip, setActiveTrip] = useState<TripSummary>(() => {
    const savedTripId = localStorage.getItem("odyssey-active-trip");
    return [...trips, ...drafts].find((trip) => trip.id === savedTripId) || trips[0];
  });
  const [profileName, setProfileName] = useState("Путешественник");
  useEffect(() => {
    const setAuthenticatedUser = (user: { email?: string; user_metadata: { full_name?: string } }, shouldNavigate = false) => {
      setProfileName(user.user_metadata.full_name || user.email || "Путешественник");
      if (!shouldNavigate) return;
      const isTripInvitation = new URLSearchParams(window.location.search).get("invite") === "trip";
      const savedView = localStorage.getItem("odyssey-current-view") as View | null;
      const savedTripId = localStorage.getItem("odyssey-active-trip");
      const savedTrip = [...trips, ...drafts].find((trip) => trip.id === savedTripId);
      if (savedTrip) setActiveTrip(savedTrip);
      setView((current) => current === "auth" ? (isTripInvitation ? "trip" : savedView === "trip" && !savedTrip ? "trips" : savedView && savedView !== "auth" ? savedView : "trips") : current);
      if (isTripInvitation) window.history.replaceState({}, "", `${window.location.pathname}${window.location.hash}`);
    };
    const loadSavedTrip = async () => {
      const { data, error } = await supabase.from("trip_state").select("payload").eq("id", "main").maybeSingle();
      if (error) {
        console.error("Could not load the saved trip.", error);
        return;
      }
      const payload = data?.payload as StoredTripPayload | undefined;
      const trip = payload && savedTrip(payload);
      if (!payload || !trip) return;
      setStoredPayload(payload);
      setDrafts((items) => [...items.filter((item) => item.id !== trip.id), trip]);
    };
    void supabase.auth.getSession().then(async ({ data }) => {
      if (!data.session?.user) return;
      if (localStorage.getItem("odyssey-remember-me") === "false") {
        await supabase.auth.signOut();
        return;
      }
      setAuthenticatedUser(data.session.user, true);
      void loadSavedTrip();
    });
    const { data: listener } = supabase.auth.onAuthStateChange((event, session) => {
      if (session?.user) {
        setAuthenticatedUser(session.user, event === "SIGNED_IN");
        if (event === "SIGNED_IN") void loadSavedTrip();
      }
      else if (event === "SIGNED_OUT") { localStorage.removeItem("odyssey-current-view"); setView("auth"); }
    });
    return () => listener.subscription.unsubscribe();
  }, []);
  useEffect(() => {
    try {
      localStorage.setItem("odyssey-drafts", JSON.stringify(drafts));
    } catch {
      // Keep the open trip usable even if browser storage is full.
    }
  }, [drafts]);
  useEffect(() => {
    localStorage.setItem("odyssey-active-trip", activeTrip.id);
  }, [activeTrip.id]);
  const go = (next: View) => {
    localStorage.setItem("odyssey-current-view", next);
    setView(next);
    setMenu(false);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };
  const updateTrip = (trip: TripSummary) => {
    setActiveTrip(trip);
    setDrafts((items) => trip.isDraft ? items.map((item) => item.id === trip.id ? trip : item) : items);
    if (trip.id !== "supabase-main" || !storedPayload?.data) return;
    const currentDays = storedPayload.data.days || [];
    const updatedDays: StoredDay[] = (trip.days || []).map((day, index) => {
      const existing = currentDays[index] || {};
      const leg = day.roadLeg;
      return {
        ...existing,
        id: existing.id || day.id,
        city: leg ? `${leg.from} → ${leg.to}` : existing.city,
        dayMapUrl: leg?.mapsUrl || (leg ? mapsUrl(leg.from, leg.to) : existing.dayMapUrl),
        checkInFrom: leg?.checkInFrom || undefined,
        checkInTo: leg?.checkInTo || undefined,
        checkOutFrom: leg?.checkOutFrom || undefined,
        checkOutTo: leg?.checkOutTo || undefined,
        completed: leg?.completed || [],
        items: day.places.map((title, itemIndex) => ({ ...existing.items?.[itemIndex], id: existing.items?.[itemIndex]?.id || crypto.randomUUID(), title })),
      };
    });
    const nextPayload: StoredTripPayload = { ...storedPayload, data: { ...storedPayload.data, days: updatedDays } };
    setStoredPayload(nextPayload);
    void supabase.from("trip_state").update({ payload: nextPayload }).eq("id", "main").then(({ error }) => {
      if (error) console.error("Could not save the trip.", error);
    });
  };
  const toggleSight = (id: string) => {
    if (!storedPayload?.data?.sights) return;
    const nextPayload: StoredTripPayload = { ...storedPayload, data: { ...storedPayload.data, sights: storedPayload.data.sights.map((sight) => sight.id === id ? { ...sight, done: !sight.done } : sight) } };
    setStoredPayload(nextPayload);
    void supabase.from("trip_state").update({ payload: nextPayload }).eq("id", "main").then(({ error }) => {
      if (error) console.error("Could not save the sight.", error);
    });
  };
  if (view === "auth") return <Auth go={go} onAuthorized={setProfileName} />;
  return (
    <div className="app">
      <Sidebar view={view} go={go} open={menu} close={() => setMenu(false)} profileName={profileName} />
      <div className="main">
        <button className="menu-button" onClick={() => setMenu(true)}>
          ☰
        </button>
        {view === "trips" && <Trips go={go} profileName={profileName} drafts={drafts} onOpenTrip={(trip) => { setActiveTrip(trip); go("trip"); }} />}
        {view === "create" && <CreateTrip go={go} onCreate={(trip) => { setDrafts((items) => [...items, trip]); setActiveTrip(trip); go("trip"); }} />}
        {view === "trip" && <Workspace go={go} trip={activeTrip} onUpdateTrip={updateTrip} />}
        {view === "catalog" && <Catalog go={go} />}
        {view === "public" && <PublicRoute go={go} />}
      </div>
    </div>
  );
}
