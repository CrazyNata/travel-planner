# Ramingo

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

## Инфраструктура и почта

Актуальная карта сервисов (сверено 2026-08-08). Секреты, пароли, API-ключи и приватные SSH-ключи в репозитории не хранятся.

| Сервис | Что там находится | Текущий статус и где управлять |
| --- | --- | --- |
| **GoDaddy** | Домен `ramingo.online` и authoritative DNS | Домен куплен здесь; DNS остаётся в GoDaddy. Здесь находятся A/CNAME/TXT/MX-записи. |
| **OVHcloud** | VPS для сайта | Ubuntu 24.04 + Caddy, сайт проксируется на приложение. В аккаунте проверено: отдельного MX Plan, Zimbra Mail и Email Pro нет. VPS не является почтовым ящиком. |
| **Resend** | Отправка писем | Бесплатный аккаунт; домен `ramingo.online` подтверждён. Supabase Auth отправляет письма через `smtp.resend.com`; отправитель — `no-reply@ramingo.online`. Resend Receiving/webhook для входящей переписки не используется. |
| **Supabase** | Auth, база и Edge Functions | Проект `hxcavgtlucyoqudbrgse`. Custom SMTP уже настроен через Resend; Site URL и Android redirect’ы переведены на `ramingo.online`, старый `travelplanner.muntim.ru` сохранён для совместимости. |
| **ImprovMX** | Входящая пересылка почты | Бесплатный аккаунт. Алиас `support@ramingo.online` пересылается на `natasha7261@gmail.com`; в GoDaddy добавлены MX `mx1.improvmx.com`/`mx2.improvmx.com` и SPF `include:spf.improvmx.com`. Также оставлен созданный ImprovMX catch-all `*@ramingo.online` на тот же Gmail; если он начнёт собирать спам, отключить его в Aliases. SMTP-лимит бесплатного плана равен 0 — это нормально: исходящие письма идут через Resend. |
| **Gmail** | Личный inbox и ручная переписка | Google Workspace не используется и не оплачивается. `support@ramingo.online` подтверждён в **Send mail as** и отправляет через Resend SMTP (`smtp.resend.com:587`, TLS). |
| **GitHub** | Репозиторий и CI/CD | Исходный код и GitHub Actions; это не почтовый провайдер. |

### Что нельзя случайно сломать

- Не удалять Resend-записи на `send.ramingo.online` и `resend._domainkey.ramingo.online`.
- Корневые MX-записи (`@`) сейчас принадлежат ImprovMX (`mx1`/`mx2`); не добавлять туда другие почтовые MX-записи.
- A-запись сайта, `www` и Caddy находятся на VPS/GoDaddy; почтовые MX-записи не меняют веб-хостинг.
- Никогда не коммитить SMTP-пароли, Resend API keys, Supabase PAT/service-role keys или приватный SSH-ключ.

Проверка почты: DNS MX/SPF виден у авторитетного DNS и публичных резолверов; тестовое письмо на `support@ramingo.online` дошло обратно в Gmail через ImprovMX; тестовое письмо из Gmail с From `support@ramingo.online` доставлено и показывает `send.ramingo.online`, подпись `ramingo.online` и TLS.

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
