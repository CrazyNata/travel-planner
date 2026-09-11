import { useEffect, useRef, useState, type TouchEvent } from "react";

export type WebOnboardingMode = "first-run" | "replay";
export type WebOnboardingExitAction = "skip" | "create" | "explore";

type OnboardingPage = {
  eyebrow: string;
  title: string;
  body: string;
  action: string;
  actionDetails: string;
};

const onboardingPages: OnboardingPage[] = [
  {
    eyebrow: "01 · НАЧАЛО",
    title: "Создайте поездку за минуту",
    body: "На главной собрана основная информация о поездках, а новая поездка начинается с одной кнопки.",
    action: "Нажмите «Новое путешествие»",
    actionDetails: "Кнопка находится слева в меню. Затем заполните название, направление и даты поездки.",
  },
  {
    eyebrow: "02 · ПЛАН",
    title: "Соберите маршрут по дням",
    body: "Редактируйте переезды, добавляйте места и храните жильё в связанных разделах поездки.",
    action: "Откройте «Маршрут»",
    actionDetails: "Выберите день, добавьте переезд или место и переходите к следующему дню по боковой шкале.",
  },
  {
    eyebrow: "03 · НАСТРОЙКИ",
    title: "Ramingo подстроится под вас",
    body: "Язык, тема и повторное обучение всегда доступны в профиле. Готовы начать?",
    action: "Нажмите ⚙ в профиле → «Показать обучение»",
    actionDetails: "Профиль находится внизу левого меню. Здесь можно вернуться к подсказкам в любой момент.",
  },
];

function RamingoBrand({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`web-onboarding-brand${compact ? " compact" : ""}`}>
      <span>R</span>
      <b>Ramingo</b>
    </div>
  );
}

function PreviewHome() {
  return (
    <div className="web-onboarding-preview-content">
      <small className="web-onboarding-preview-eyebrow">МОИ ПУТЕШЕСТВИЯ</small>
      <h3>Мои путешествия</h3>
      <div className="web-onboarding-preview-pills">
        <span className="active">Все · 1</span>
        <span>Предстоящие</span>
        <span>Черновики</span>
      </div>
      <article className="web-onboarding-trip-preview">
        <div className="web-onboarding-trip-status">
          <i /> Предстоящее
        </div>
        <strong>Зимняя Италия</strong>
        <small>19 дек — 3 янв · 10 дней</small>
        <div className="web-onboarding-progress"><i /></div>
        <span>Рим → Флоренция → Пиза</span>
      </article>
      <div className="web-onboarding-home-focus">
        <span>1</span>
        <div><b>Начните здесь</b><small>Нажмите «Новое путешествие» в левом меню</small></div>
      </div>
      <div className="web-onboarding-home-map"><PreviewMap /></div>
      <div className="web-onboarding-weather-row">
        <span>☀ Рим&nbsp; 12°</span>
        <span>☁ Флоренция&nbsp; 10°</span>
        <span>↗ 3 города</span>
      </div>
      <nav className="web-onboarding-preview-nav" aria-label="Разделы приложения">
        <span className="active">Путешествия</span>
        <span>Каталог</span>
        <span>Профиль</span>
      </nav>
    </div>
  );
}

function PreviewMap() {
  return (
    <div className="web-onboarding-map-preview">
      <svg viewBox="0 0 560 170" role="img" aria-label="Маршрут на карте">
        <path d="M45 122C125 16 190 147 285 54S410 40 510 122" />
        {["45,122", "285,54", "510,122"].map((point) => {
          const [cx, cy] = point.split(",");
          return <circle cx={cx} cy={cy} r="9" key={point} />;
        })}
      </svg>
      <span>3 города · 9 дней</span>
    </div>
  );
}

function PreviewRoute() {
  return (
    <div className="web-onboarding-preview-content">
      <div className="web-onboarding-workspace-heading">
        <div>
          <small className="web-onboarding-preview-eyebrow">ЗИМНЯЯ ИТАЛИЯ</small>
          <h3>План поездки</h3>
        </div>
        <span>✎</span>
      </div>
      <nav className="web-onboarding-workspace-tabs" aria-label="Разделы поездки">
        <span>Главная</span>
        <span className="active">Маршрут</span>
        <span>Достопримечательности</span>
        <span>Жильё</span>
      </nav>
      <div className="web-onboarding-route-board">
        <aside className="web-onboarding-day-rail">
          <span className="active">День 1<small>Рим</small></span>
          <span>День 2<small>Флоренция</small></span>
          <span>День 3<small>Пиза</small></span>
        </aside>
        <div className="web-onboarding-route-content">
          <div className="web-onboarding-mini-map"><PreviewMap /></div>
          <div className="web-onboarding-route-row">
            <div><small>ПЕРЕЕЗД · ДЕНЬ 1</small><strong>Рим → Флоренция</strong></div>
            <span>⧉</span>
            <span>✎</span>
          </div>
        </div>
      </div>
      <div className="web-onboarding-linked-sections">
        <article className="highlighted">
          <span>◈</span>
          <div><small>Достопримечательности</small><b>По дням · карта · порядок</b></div>
        </article>
        <article>
          <span>⌂</span>
          <div><small>Жильё</small><b>Даты · цена · ссылка</b></div>
        </article>
      </div>
      <p className="web-onboarding-preview-note">Разделы поездки связаны между собой</p>
    </div>
  );
}

function PreviewSettings() {
  return (
    <div className="web-onboarding-preview-content web-onboarding-settings-preview">
      <div className="web-onboarding-workspace-heading">
        <div>
          <small className="web-onboarding-preview-eyebrow">ПРОФИЛЬ / НАСТРОЙКИ</small>
          <h3>Личный кабинет</h3>
        </div>
        <span className="web-onboarding-settings-avatar">НП</span>
      </div>
      <div className="web-onboarding-settings-stats">
        <span><b>2</b><small>поездки</small></span>
        <span><b>5</b><small>городов</small></span>
        <span><b>—</b><small>км</small></span>
      </div>
      <div className="web-onboarding-settings-list">
        <div className="web-onboarding-settings-row">
          <span className="web-onboarding-settings-icon">文A</span>
          <div><b>Языки</b><small>Русский</small></div>
          <strong>RU&nbsp; ›</strong>
        </div>
        <div className="web-onboarding-settings-row">
          <span className="web-onboarding-settings-icon">☾</span>
          <div><b>Тёмная тема</b><small>Сохраняет выбор</small></div>
          <i className="web-onboarding-toggle"><b /></i>
        </div>
        <div className="web-onboarding-settings-row highlighted">
          <span className="web-onboarding-settings-icon">✦</span>
          <div><b>Показать обучение</b><small>Вернуться к tutorial</small></div>
          <strong>›</strong>
        </div>
      </div>
      <p className="web-onboarding-preview-note">Настройки и tutorial всегда под рукой</p>
    </div>
  );
}

function OnboardingPreview({ page }: { page: number }) {
  return (
    <div className="web-onboarding-preview-shell">
      <header>
        <RamingoBrand compact />
        <span className="web-onboarding-preview-user">НП</span>
      </header>
      {page === 0 && <PreviewHome />}
      {page === 1 && <PreviewRoute />}
      {page === 2 && <PreviewSettings />}
    </div>
  );
}

export function WebOnboarding({
  mode,
  darkTheme = false,
  onExit,
}: {
  mode: WebOnboardingMode;
  darkTheme?: boolean;
  onExit: (action: WebOnboardingExitAction) => void;
}) {
  const [page, setPage] = useState(0);
  const [finishing, setFinishing] = useState(false);
  const touchStart = useRef<{ x: number; y: number } | null>(null);
  const copy = onboardingPages[page];

  useEffect(() => {
    document.body.classList.add("ramingo-onboarding-open");
    return () => document.body.classList.remove("ramingo-onboarding-open");
  }, []);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (finishing) return;
      if (event.key === "Escape") {
        event.preventDefault();
        setFinishing(true);
        onExit("skip");
      } else if (event.key === "ArrowLeft" && page > 0) {
        event.preventDefault();
        setPage((current) => Math.max(0, current - 1));
      } else if (event.key === "ArrowRight") {
        event.preventDefault();
        if (page === onboardingPages.length - 1) {
          setFinishing(true);
          onExit("explore");
        } else {
          setPage((current) => Math.min(onboardingPages.length - 1, current + 1));
        }
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [finishing, onExit, page]);

  const exit = (action: WebOnboardingExitAction) => {
    if (finishing) return;
    setFinishing(true);
    onExit(action);
  };

  const next = () => {
    if (page === onboardingPages.length - 1) {
      exit("explore");
      return;
    }
    setPage((current) => Math.min(onboardingPages.length - 1, current + 1));
  };

  const handleTouchStart = (event: TouchEvent<HTMLElement>) => {
    const touch = event.changedTouches[0];
    if (!touch || finishing) return;
    touchStart.current = { x: touch.clientX, y: touch.clientY };
  };

  const handleTouchEnd = (event: TouchEvent<HTMLElement>) => {
    const start = touchStart.current;
    touchStart.current = null;
    const touch = event.changedTouches[0];
    if (!start || !touch || finishing) return;
    const deltaX = touch.clientX - start.x;
    const deltaY = touch.clientY - start.y;
    if (Math.abs(deltaX) < 48 || Math.abs(deltaX) <= Math.abs(deltaY)) return;
    if (deltaX < 0) {
      next();
    } else if (page > 0) {
      setPage((current) => Math.max(0, current - 1));
    }
  };

  return (
    <div className={`web-onboarding-backdrop${darkTheme ? " dark" : ""}`}>
      <section
        className="web-onboarding-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="web-onboarding-title"
        aria-describedby="web-onboarding-body"
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
        onTouchCancel={() => {
          touchStart.current = null;
        }}
      >
        <header className="web-onboarding-header">
          <RamingoBrand />
          <div className="web-onboarding-meta">
            <span>ПУТЕШЕСТВУЙТЕ ВМЕСТЕ</span>
            <b>{String(page + 1).padStart(2, "0")} / 03</b>
          </div>
          {page < onboardingPages.length - 1 ? (
            <button
              type="button"
              className="web-onboarding-skip"
              onClick={() => exit("skip")}
              disabled={finishing}
            >
              Пропустить
            </button>
          ) : (
            <span className="web-onboarding-header-spacer" aria-hidden="true" />
          )}
        </header>

        <div className="web-onboarding-main">
          <div className="web-onboarding-copy">
            <p className="web-onboarding-eyebrow">{copy.eyebrow}</p>
            <h1 id="web-onboarding-title">{copy.title}</h1>
            <p id="web-onboarding-body">{copy.body}</p>
            <div className="web-onboarding-action-card">
              <span className="web-onboarding-action-index">{String(page + 1).padStart(2, "0")}</span>
              <div>
                <small>Что нажать</small>
                <strong>{copy.action}</strong>
                <p>{copy.actionDetails}</p>
              </div>
            </div>
            <div className="web-onboarding-help">
              <span>✦</span>
              <small>{mode === "replay" ? "Можно вернуться к этим подсказкам в любой момент" : "После этого переходите к следующему шагу"}</small>
            </div>
          </div>
          <OnboardingPreview page={page} />
        </div>

        <footer className="web-onboarding-footer">
          <div className="web-onboarding-dots" aria-label={`Шаг ${page + 1} из ${onboardingPages.length}`}>
            {onboardingPages.map((item, index) => (
              <button
                type="button"
                className={index === page ? "active" : ""}
                onClick={() => setPage(index)}
                aria-label={`Перейти к шагу ${index + 1}`}
                aria-current={index === page ? "step" : undefined}
                disabled={finishing}
                key={item.eyebrow}
              />
            ))}
          </div>
          {page === onboardingPages.length - 1 && mode === "first-run" ? (
            <div className="web-onboarding-final-actions">
              <button type="button" className="web-onboarding-primary" onClick={() => exit("create")} disabled={finishing}>
                Создать первую поездку
              </button>
              <button type="button" className="web-onboarding-secondary" onClick={() => exit("explore")} disabled={finishing}>
                Сначала осмотреться
              </button>
            </div>
          ) : (
            <button type="button" className="web-onboarding-primary" onClick={next} disabled={finishing}>
              {page === onboardingPages.length - 1 ? "Вернуться в приложение" : "Далее"}
            </button>
          )}
          {page > 0 ? (
            <button type="button" className="web-onboarding-back" onClick={() => setPage((current) => Math.max(0, current - 1))} disabled={finishing}>
              ← Назад
            </button>
          ) : (
            <span className="web-onboarding-shortcut">← → листать</span>
          )}
        </footer>
      </section>
    </div>
  );
}

