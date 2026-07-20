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

## Публикация на GitHub Pages

Workflow `.github/workflows/deploy-pages.yml` публикует сайт после каждого push в `main`.

Перед первым деплоем добавьте в GitHub Repository variables:

```text
VITE_SUPABASE_URL
VITE_SUPABASE_PUBLISHABLE_KEY
```

В настройках репозитория откройте `Settings` → `Pages` и выберите `GitHub Actions` как источник публикации.
