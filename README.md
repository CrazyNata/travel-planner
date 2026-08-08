# Одиссея

Самостоятельный интерактивный прототип многопользовательского планировщика путешествий.

## Запуск

```bash
npm install
npm run dev
```

Production-сборка:

```bash
npm run build
```

## Авторизация

Приложение использует Supabase Auth. Для локального запуска создайте `.env.local`:

```bash
VITE_SUPABASE_URL=https://<project-ref>.supabase.co
VITE_SUPABASE_PUBLISHABLE_KEY=sb_publishable_<key>
```

## Хранение данных

Supabase является единственным постоянным хранилищем данных приложения. Браузерные хранилища не используются для данных, черновиков или состояния интерфейса. Исключение: токен Supabase Auth может храниться для функции «Запомнить меня». Полные правила находятся в `AGENTS.md`.

## Публикация на GitHub Pages

Workflow `.github/workflows/deploy-pages.yml` публикует сайт после каждого push в `main`.

Перед первым деплоем добавьте в GitHub Repository variables:

```text
VITE_SUPABASE_URL
VITE_SUPABASE_PUBLISHABLE_KEY
VITE_MAPBOX_ACCESS_TOKEN
VITE_LEGAL_ENTITY_NAME
VITE_LEGAL_CONTACT_EMAIL
VITE_LEGAL_EFFECTIVE_DATE
```

В настройках репозитория откройте `Settings` → `Pages` и выберите `GitHub Actions` как источник публикации.

## Android-релиз

Native Android-проект находится в `mobile/`. Инструкция по production-конфигурации, подписанному AAB, GitHub Actions и чек-листу Play Console: [`mobile/RELEASE.md`](mobile/RELEASE.md).
