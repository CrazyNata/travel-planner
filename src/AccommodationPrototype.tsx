import { useMemo, useState } from "react";

type PrototypeAccommodation = {
  id: string;
  placeId: string;
  name: string;
  city: string;
  address: string;
  type: string;
  rating: number | null;
  reviews: number | null;
  priceLevel: string;
  coordinates: string;
  website: string;
  phone: string;
  mapsUrl: string;
  photos: string[];
  dates: string;
  bookingUrl: string;
  status: string;
};

const prototypePlaces: PrototypeAccommodation[] = [
  {
    id: "google:hotel-artemide",
    placeId: "ChIJ-artemide-demo",
    name: "Hotel Artemide",
    city: "Рим, Италия",
    address: "Via Nazionale, 22, Roma",
    type: "Отель",
    rating: 4.6,
    reviews: 3120,
    priceLevel: "€€€",
    coordinates: "41.9018, 12.4942",
    website: "https://www.hotelartemide.it/",
    phone: "+39 06 4899 111",
    mapsUrl: "https://www.google.com/maps/search/?api=1&query=Hotel%20Artemide%20Rome",
    photos: [
      "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1100&q=85",
      "https://images.unsplash.com/photo-1564501049412-61c2a3083791?auto=format&fit=crop&w=1100&q=85",
      "https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1100&q=85",
    ],
    dates: "19–22 дек",
    bookingUrl: "",
    status: "хочу",
  },
  {
    id: "google:room-mate-filippo",
    placeId: "ChIJ-filippo-demo",
    name: "Room Mate Filippo",
    city: "Рим, Италия",
    address: "Via Mario de' Fiori, 37, Roma",
    type: "Отель",
    rating: 4.3,
    reviews: 1984,
    priceLevel: "€€",
    coordinates: "41.9051, 12.4837",
    website: "https://room-matehotels.com/en/filippo/",
    phone: "+39 06 2036 3640",
    mapsUrl: "https://www.google.com/maps/search/?api=1&query=Room%20Mate%20Filippo%20Rome",
    photos: [
      "https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1100&q=85",
      "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1100&q=85",
    ],
    dates: "19–22 дек",
    bookingUrl: "",
    status: "хочу",
  },
  {
    id: "google:palazzo-navona",
    placeId: "ChIJ-navona-demo",
    name: "Palazzo Navona Hotel",
    city: "Рим, Италия",
    address: "Largo della Sapienza, 8, Roma",
    type: "Бутик-отель",
    rating: 4.7,
    reviews: 856,
    priceLevel: "€€€",
    coordinates: "41.8988, 12.4731",
    website: "https://www.palazzonavonahotel.com/",
    phone: "+39 06 686 1425",
    mapsUrl: "https://www.google.com/maps/search/?api=1&query=Palazzo%20Navona%20Hotel%20Rome",
    photos: [
      "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?auto=format&fit=crop&w=1100&q=85",
      "https://images.unsplash.com/photo-1601918774946-25832a4be0d6?auto=format&fit=crop&w=1100&q=85",
    ],
    dates: "19–22 дек",
    bookingUrl: "",
    status: "хочу",
  },
  {
    id: "google:guardián",
    placeId: "ChIJ-guardian-demo",
    name: "The Guardian",
    city: "Рим, Италия",
    address: "Via Palestro, 13, Roma",
    type: "Апарт-отель",
    rating: 4.2,
    reviews: 560,
    priceLevel: "€€",
    coordinates: "41.9069, 12.5014",
    website: "",
    phone: "+39 06 445 1258",
    mapsUrl: "https://www.google.com/maps/search/?api=1&query=The%20Guardian%20Rome",
    photos: [
      "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1100&q=85",
      "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=1100&q=85",
    ],
    dates: "19–22 дек",
    bookingUrl: "",
    status: "хочу",
  },
];

type ModalMode = "chooser" | "search" | "details" | "manual" | "added" | null;

function bookingSearchUrl(name: string, city: string) {
  const query = encodeURIComponent(`${name} ${city}`.trim());
  return `https://www.booking.com/searchresults.html?ss=${query}`;
}

function mapsSearchUrl(name: string, city: string, address: string) {
  const query = encodeURIComponent(`${name} ${address} ${city}`.trim());
  return `https://www.google.com/maps/search/?api=1&query=${query}`;
}

function nightsLabel(dates: string) {
  return dates === "19–22 дек" ? "3 ночи" : "даты поездки";
}

function ratingLabel(rating: number | null) {
  return rating === null ? "" : rating.toFixed(1).replace(".0", "");
}

function PrototypePlaceImage({
  place,
  index = 0,
  className = "",
}: {
  place: PrototypeAccommodation;
  index?: number;
  className?: string;
}) {
  if (!place.photos.length) {
    return <div className={`${className} housing-photo-placeholder`} role="img" aria-label={place.name}>⌂</div>;
  }
  return (
    <img
      className={className}
      src={place.photos[index % place.photos.length]}
      alt={place.name}
    />
  );
}

function PlaceMeta({ place }: { place: PrototypeAccommodation }) {
  return (
    <div className="housing-place-meta">
      {place.rating !== null && <span className="housing-rating">★ {ratingLabel(place.rating)}</span>}
      {place.reviews !== null && place.reviews > 0 && <span>{place.reviews.toLocaleString("ru-RU")} отзывов</span>}
      {place.priceLevel && <span>{place.priceLevel}</span>}
    </div>
  );
}

function HousingAddChoiceModal({
  onClose,
  onManual,
  onSearch,
}: {
  onClose: () => void;
  onManual: () => void;
  onSearch: () => void;
}) {
  return (
    <div className="housing-modal-backdrop" role="presentation">
      <section className="housing-modal housing-choice-modal" role="dialog" aria-modal="true" aria-labelledby="housing-choice-title">
        <header className="housing-modal-header">
          <div>
            <span className="housing-eyebrow">НОВОЕ ЖИЛЬЁ</span>
            <h2 id="housing-choice-title">Как добавить жильё?</h2>
            <p>Выберите удобный способ — оба варианта сохраняются в поездке одинаково.</p>
          </div>
          <button className="housing-close" type="button" onClick={onClose} aria-label="Закрыть">×</button>
        </header>
        <div className="housing-choice-options">
          <button className="housing-choice-option" type="button" onClick={onManual}>
            <span className="housing-choice-icon">✎</span>
            <span><strong>Добавить вручную</strong><small>Название, даты, адрес и ссылка на бронирование</small></span>
            <b>›</b>
          </button>
          <button className="housing-choice-option housing-choice-option-primary" type="button" onClick={onSearch}>
            <span className="housing-choice-icon">⌕</span>
            <span><strong>Выбрать из списка</strong><small>Поиск отелей и апартаментов через Google Places</small></span>
            <b>›</b>
          </button>
        </div>
        <div className="housing-choice-footnote"><span>✦</span> Можно сохранить собственную booking-ссылку в любом варианте.</div>
      </section>
    </div>
  );
}

function HousingManualModal({
  onClose,
  onSave,
}: {
  onClose: () => void;
  onSave: (stay: PrototypeAccommodation) => void;
}) {
  const [status, setStatus] = useState("хочу");
  const [name, setName] = useState("");
  const [city, setCity] = useState("Рим, Италия");
  const [price, setPrice] = useState("");
  const [checkIn, setCheckIn] = useState("19 дек");
  const [checkOut, setCheckOut] = useState("22 дек");
  const [bookingUrl, setBookingUrl] = useState("");
  const [details, setDetails] = useState("");
  const canSave = name.trim().length > 0;

  return (
    <div className="housing-modal-backdrop" role="presentation">
      <section className="housing-modal housing-manual-modal" role="dialog" aria-modal="true" aria-labelledby="housing-manual-title">
        <header className="housing-modal-header">
          <div>
            <span className="housing-eyebrow">ВРУЧНУЮ</span>
            <h2 id="housing-manual-title">Новое жильё</h2>
            <p>Добавьте место, если его нет в списке или вы уже забронировали его сами.</p>
          </div>
          <button className="housing-close" type="button" onClick={onClose} aria-label="Закрыть">×</button>
        </header>
        <form className="housing-manual-form" onSubmit={(event) => {
          event.preventDefault();
          if (!canSave) return;
          onSave({
            id: "manual:" + Date.now(),
            placeId: "",
            name: name.trim(),
            city: city.trim() || "Город не указан",
            address: details.trim() || "Адрес добавим позже",
            type: "Жильё",
            rating: null,
            reviews: null,
            priceLevel: price.trim(),
            coordinates: "",
            website: "",
            phone: "",
            mapsUrl: details.trim() ? mapsSearchUrl(name.trim(), city.trim() || "Город не указан", details.trim()) : "",
            photos: [],
            dates: checkIn.trim() && checkOut.trim() ? checkIn.trim() + " – " + checkOut.trim() : "Даты не указаны",
            bookingUrl: bookingUrl.trim(),
            status,
          });
        }}>
          <div className="housing-manual-status">
            <span>Статус</span>
            <div>
              {[
                ["хочу", "Хочу"],
                ["бронь", "Забронировано"],
                ["оплачено", "Оплачено"],
                ["пожили", "Пожили"],
              ].map(([value, label]) => (
                <button className={status === value ? "active" : ""} type="button" key={value} onClick={() => setStatus(value)}>{label}</button>
              ))}
            </div>
          </div>
          <label><span>Название</span><input value={name} onChange={(event) => setName(event.target.value)} placeholder="Например, Hotel Artemide" autoFocus /></label>
          <div className="housing-manual-two-columns">
            <label><span>Город</span><input value={city} onChange={(event) => setCity(event.target.value)} placeholder="Рим, Италия" /></label>
            <label><span>Цена / заметка</span><input value={price} onChange={(event) => setPrice(event.target.value)} placeholder="€€ или €450" /></label>
          </div>
          <div className="housing-manual-two-columns">
            <label><span>Заезд</span><input value={checkIn} onChange={(event) => setCheckIn(event.target.value)} placeholder="19 дек" /></label>
            <label><span>Выезд</span><input value={checkOut} onChange={(event) => setCheckOut(event.target.value)} placeholder="22 дек" /></label>
          </div>
          <label><span>Ссылка на бронирование</span><input type="url" value={bookingUrl} onChange={(event) => setBookingUrl(event.target.value)} placeholder="https://booking.com/... или сайт объекта" /></label>
          <label><span>Адрес / заметка</span><textarea value={details} onChange={(event) => setDetails(event.target.value)} placeholder="Адрес, условия, номер брони или дополнительные детали" rows={3} /></label>
          <footer className="housing-manual-actions">
            <button className="housing-secondary-button" type="button" onClick={onClose}>Отмена</button>
            <button className="housing-primary-button" type="submit" disabled={!canSave}>Сохранить жильё</button>
          </footer>
        </form>
      </section>
    </div>
  );
}

function HousingSearchModal({
  onClose,
  onOpenDetails,
}: {
  onClose: () => void;
  onOpenDetails: (place: PrototypeAccommodation) => void;
}) {
  const [city, setCity] = useState("Рим, Италия");
  const [dates, setDates] = useState("19–22 дек · 2 гостя");
  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");
  const filteredPlaces = useMemo(() => {
    const normalized = submittedQuery.trim().toLocaleLowerCase("ru");
    if (!normalized) return prototypePlaces;
    return prototypePlaces.filter((place) =>
      [place.name, place.address, place.type].some((value) =>
        value.toLocaleLowerCase("ru").includes(normalized),
      ),
    );
  }, [submittedQuery]);

  return (
    <div className="housing-modal-backdrop" role="presentation">
      <section className="housing-modal housing-search-modal" role="dialog" aria-modal="true" aria-labelledby="housing-search-title">
        <header className="housing-modal-header">
          <div>
            <span className="housing-eyebrow">GOOGLE PLACES</span>
            <h2 id="housing-search-title">Найти жильё</h2>
            <p>Отели, апартаменты и другие варианты в городе поездки</p>
          </div>
          <button className="housing-close" type="button" onClick={onClose} aria-label="Закрыть">×</button>
        </header>

        <div className="housing-search-fields">
          <label>
            <span>Город поездки</span>
            <input value={city} onChange={(event) => setCity(event.target.value)} />
          </label>
          <label>
            <span>Даты и гости</span>
            <input value={dates} onChange={(event) => setDates(event.target.value)} />
          </label>
          <label className="housing-search-query">
            <span>Что ищем</span>
            <div className="housing-query-control">
              <span aria-hidden="true">⌕</span>
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") setSubmittedQuery(query);
                }}
                placeholder="Название, отель или тип жилья"
              />
              <button type="button" onClick={() => setSubmittedQuery(query)}>Найти</button>
            </div>
          </label>
        </div>

        <div className="housing-search-source">
          <span>✦</span>
          <span>Результаты Google Places · рейтинг и фотографии из Google Maps</span>
        </div>

        <div className="housing-search-section-heading">
          <div>
            <h3>{submittedQuery ? "Результаты поиска" : "Рекомендуем"}</h3>
            <p>{filteredPlaces.length} варианта · {city}</p>
          </div>
          <span className="housing-sort-label">Сначала по рейтингу ▾</span>
        </div>

        <div className="housing-results-list">
          {filteredPlaces.map((place) => (
            <button
              className="housing-result-row"
              type="button"
              key={place.id}
              onClick={() => onOpenDetails({ ...place, city, dates: dates.split("·")[0].trim() })}
            >
              <PrototypePlaceImage place={place} className="housing-result-image" />
              <span className="housing-result-copy">
                <strong>{place.name}</strong>
                <PlaceMeta place={place} />
                <small>{place.address}</small>
                <small className="housing-place-type">{place.type}</small>
              </span>
              <span className="housing-result-arrow" aria-hidden="true">›</span>
            </button>
          ))}
          {!filteredPlaces.length && (
            <div className="housing-empty-search">Ничего не найдено. Попробуйте название или тип жилья.</div>
          )}
        </div>
      </section>
    </div>
  );
}

function HousingDetailsModal({
  place,
  saved,
  onClose,
  onAdd,
  onSave,
}: {
  place: PrototypeAccommodation;
  saved: boolean;
  onClose: () => void;
  onAdd: (bookingUrl: string) => void;
  onSave: (bookingUrl: string) => void;
}) {
  const [photoIndex, setPhotoIndex] = useState(0);
  const [bookingUrl, setBookingUrl] = useState(place.bookingUrl);
  const bookingTarget = place.bookingUrl || place.website || bookingSearchUrl(place.name, place.city);
  return (
    <div className="housing-modal-backdrop" role="presentation">
      <section className="housing-modal housing-details-modal" role="dialog" aria-modal="true" aria-labelledby="housing-details-title">
        <div className="housing-details-photo">
          <PrototypePlaceImage place={place} index={photoIndex} />
          {place.photos.length > 0 && <>
            <button type="button" className="housing-photo-nav housing-photo-prev" onClick={() => setPhotoIndex((photoIndex - 1 + place.photos.length) % place.photos.length)} aria-label="Предыдущее фото">‹</button>
            <button type="button" className="housing-photo-nav housing-photo-next" onClick={() => setPhotoIndex((photoIndex + 1) % place.photos.length)} aria-label="Следующее фото">›</button>
            <span className="housing-photo-counter">{photoIndex + 1} / {place.photos.length}</span>
          </>}
          <button className="housing-photo-close" type="button" onClick={onClose} aria-label="Закрыть">×</button>
        </div>

        <div className="housing-details-body">
          <div className="housing-details-title-row">
            <div>
              <span className="housing-eyebrow">{place.type}</span>
              <h2 id="housing-details-title">{place.name}</h2>
              <PlaceMeta place={place} />
            </div>
            <span className="housing-source-badge">{place.placeId ? "Google" : "Вручную"}</span>
          </div>

          <div className="housing-details-grid">
            <div><span>⌖</span><p><b>Адрес</b>{place.address}</p></div>
            <div><span>⌖</span><p><b>Координаты</b>{place.coordinates || "Не указаны"}</p></div>
            <div><span>☎</span><p><b>Телефон</b>{place.phone || "Не указан"}</p></div>
            <div><span>↗</span><p><b>Сайт объекта</b>{place.website ? "Официальный сайт" : "Не указан"}</p></div>
          </div>

          <div className="housing-details-links">
            {place.mapsUrl && <a href={place.mapsUrl} target="_blank" rel="noreferrer">Открыть Google Maps ↗</a>}
            <a href={bookingTarget} target="_blank" rel="noreferrer">Посмотреть цены ↗</a>
          </div>

          <label className="housing-booking-field">
            <span>Ссылка на бронирование <em>необязательно</em></span>
            <input
              value={bookingUrl}
              onChange={(event) => setBookingUrl(event.target.value)}
              placeholder="https://booking.com/... или сайт апартаментов"
            />
          </label>

          <div className="housing-details-actions">
            <button className="housing-primary-button" type="button" onClick={() => (saved ? onSave(bookingUrl) : onAdd(bookingUrl))}>
              {saved ? "Сохранить изменения" : "Добавить в поездку"}
            </button>
            {!saved && <button className="housing-secondary-button" type="button" onClick={() => window.open(bookingTarget, "_blank", "noopener,noreferrer")}>Посмотреть / Забронировать</button>}
          </div>
          <p className="housing-details-note">Цены и доступность будут подключены позже через Booking Demand API или другой hotel API.</p>
        </div>
      </section>
    </div>
  );
}

function HousingAddedModal({
  place,
  onViewTrip,
  onFindMore,
}: {
  place: PrototypeAccommodation;
  onViewTrip: () => void;
  onFindMore: () => void;
}) {
  return (
    <div className="housing-modal-backdrop" role="presentation">
      <section className="housing-modal housing-added-modal" role="dialog" aria-modal="true" aria-labelledby="housing-added-title">
        <div className="housing-success-mark">✓</div>
        <span className="housing-eyebrow">ЖИЛЬЁ СОХРАНЕНО</span>
        <h2 id="housing-added-title">Добавлено в поездку</h2>
        <strong>{place.name}</strong>
        <p>{place.dates} · {nightsLabel(place.dates)}</p>
        <button className="housing-primary-button" type="button" onClick={onViewTrip}>Посмотреть в поездке</button>
        <button className="housing-secondary-button" type="button" onClick={onFindMore}>Найти ещё жильё</button>
        <div className="housing-tip"><span>✦</span><p><b>Совет</b> Цены могут меняться. Проверьте актуальные предложения на сайте партнёра.</p></div>
      </section>
    </div>
  );
}

export function AccommodationPrototype() {
  const [stays, setStays] = useState<PrototypeAccommodation[]>([]);
  const [modal, setModal] = useState<ModalMode>(null);
  const [selected, setSelected] = useState<PrototypeAccommodation | null>(null);
  const [notice, setNotice] = useState("");
  const [filter, setFilter] = useState("Все");
  const [cardPhotoIndices, setCardPhotoIndices] = useState<Record<string, number>>({});
  const hasSavedStay = selected ? stays.some((stay) => stay.id === selected.id) : false;
  const visibleStays = filter === "Все" ? stays : stays.filter((stay) => stay.status === filter);
  const statusLabels = ["хочу", "бронь", "оплачено", "пожили"];

  const openAdd = () => {
    setSelected(null);
    setModal("chooser");
    setNotice("");
  };
  const openSearch = () => {
    setSelected(null);
    setModal("search");
    setNotice("");
  };
  const openDetails = (place: PrototypeAccommodation) => {
    setSelected(place);
    setModal("details");
  };
  const closeModal = () => {
    setModal(null);
    setSelected(null);
  };
  const addStay = (bookingUrl: string) => {
    if (!selected) return;
    setStays((current) => [
      ...current,
      { ...selected, id: `${selected.id}:${Date.now()}`, bookingUrl: bookingUrl.trim(), status: "хочу" },
    ]);
    setModal("added");
  };
  const saveManualStay = (stay: PrototypeAccommodation) => {
    setStays((current) => [...current, stay]);
    setSelected(stay);
    setModal("added");
  };
  const saveStay = (bookingUrl: string) => {
    if (!selected) return;
    setStays((current) => current.map((stay) => stay.id === selected.id ? { ...stay, bookingUrl: bookingUrl.trim() } : stay));
    setNotice("Ссылка на бронирование сохранена");
    closeModal();
  };
  const deleteStay = (id: string) => {
    setStays((current) => current.filter((stay) => stay.id !== id));
    setNotice("Жильё удалено из поездки");
  };
  const updateStatus = (id: string, status: string) => {
    setStays((current) => current.map((stay) => stay.id === id ? { ...stay, status } : stay));
  };

  return (
    <main className="housing-preview-page">
      <header className="housing-preview-topbar">
        <div className="housing-preview-brand"><span>R</span><b>Ramingo</b><small>Браузерный предпросмотр</small></div>
        <div className="housing-preview-trip-context"><span>Путешествие</span><b>Рождественская Италия</b><small>19–22 декабря · Рим</small></div>
      </header>

      <section className="housing-preview-workspace">
        <div className="accommodation-page">
          <header className="accommodation-heading">
            <h2>Жильё</h2>
            <button className="accent" type="button" onClick={openAdd}>＋ Добавить жильё</button>
          </header>

          <div className="accommodation-tabs">
            <button className="active" type="button">Список жилья</button>
            <button type="button" onClick={() => setNotice("Здесь появится список сроков бесплатной отмены")}>Отмена</button>
          </div>
          <div className="accommodation-filters" aria-label="Фильтр жилья">
            {["Все", "хочу", "бронь", "оплачено"].map((item) => (
              <button className={filter === item ? "active" : ""} type="button" key={item} onClick={() => setFilter(item)}>
                {item === "Все" ? `Все · ${stays.length}` : item === "бронь" ? "Забронировано" : item[0].toUpperCase() + item.slice(1)}
              </button>
            ))}
          </div>
          {notice && <div className="housing-preview-notice">{notice}</div>}

          <div className="accommodation-grid">
            {visibleStays.map((stay, index) => {
              const photos = stay.photos || [];
              const photoIndex = (cardPhotoIndices[stay.id] || 0) % Math.max(photos.length, 1);
              const changePhoto = (offset: number) => {
                if (photos.length < 2) return;
                setCardPhotoIndices((current) => ({
                  ...current,
                  [stay.id]: (photoIndex + offset + photos.length) % photos.length,
                }));
              };
              return (
                <article className={`accommodation-card c${index % 6}`} key={stay.id}>
                  <div className="accommodation-photo" onClick={() => openDetails(stay)}>
                    <PrototypePlaceImage place={stay} index={photoIndex} className="accommodation-photo-image" />
                    <span className={`stay-badge ${stay.status}`}>{stay.status}</span>
                    <button className="accommodation-edit" type="button" aria-label="Редактировать жильё" onClick={(event) => { event.stopPropagation(); openDetails(stay); }}>⋯</button>
                    <button className="accommodation-photo-previous" type="button" aria-label="Предыдущее фото" disabled={photos.length < 2} onClick={(event) => { event.stopPropagation(); changePhoto(-1); }}>‹</button>
                    <button className="accommodation-photo-next" type="button" aria-label="Следующее фото" disabled={photos.length < 2} onClick={(event) => { event.stopPropagation(); changePhoto(1); }}>›</button>
                    {photos.length > 1 && <i>{photos.map((_, photo) => photo === photoIndex ? "●" : "○").join(" ")}</i>}
                  </div>
                  <div className="accommodation-body">
                    <p>⌖ {stay.city}</p>
                    <h3>{stay.name}</h3>
                    <div className="stay-price"><span>{stay.dates}</span><b>{stay.priceLevel || "—"}</b></div>
                    <div className="stay-statuses">
                      {statusLabels.map((item) => (
                        <button className={stay.status === item ? `active ${item}` : ""} type="button" onClick={() => updateStatus(stay.id, item)} key={item}>{item}</button>
                      ))}
                    </div>
                    <small>{stay.address}</small>
                    <footer>
                      <a href={stay.bookingUrl || stay.website || bookingSearchUrl(stay.name, stay.city)} target="_blank" rel="noreferrer">{stay.bookingUrl ? "Ссылка на Букинг →" : "Посмотреть цены →"}</a>
                      <button type="button" onClick={() => deleteStay(stay.id)}>удалить</button>
                    </footer>
                  </div>
                </article>
              );
            })}
          </div>
          {!visibleStays.length && <p className="accommodation-empty">{stays.length ? "В этом статусе пока нет жилья." : "Жильё пока не добавлено."}</p>}
        </div>
      </section>

      {modal === "chooser" && <HousingAddChoiceModal onClose={closeModal} onManual={() => setModal("manual")} onSearch={() => setModal("search")} />}
      {modal === "manual" && <HousingManualModal onClose={closeModal} onSave={saveManualStay} />}
      {modal === "search" && <HousingSearchModal onClose={closeModal} onOpenDetails={openDetails} />}
      {modal === "details" && selected && <HousingDetailsModal place={selected} saved={hasSavedStay} onClose={closeModal} onAdd={addStay} onSave={saveStay} />}
      {modal === "added" && selected && <HousingAddedModal place={selected} onViewTrip={closeModal} onFindMore={openSearch} />}
    </main>
  );
}
