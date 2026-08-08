# Data Storage Rules

- Supabase is the only persistent store for application data.
- Do not use `localStorage`, `sessionStorage`, IndexedDB, cookies, or browser caches for application data, drafts, or UI state.
- Browser storage is permitted only for Supabase Auth tokens. When users select "Remember me", use persistent auth storage; otherwise keep the auth session only for the current browser session.
- Keep transient UI state only in React state. Persist user-visible data through Supabase tables or Storage before treating a change as saved.

# Existing Trip Data

- Never delete, clear, overwrite, or replace user-entered data in an existing populated trip unless the user explicitly identifies that trip and explicitly requests deletion of those specific data.
- When adapting defaults or templates, apply changes only to newly created trips. Preserve all content in existing trips.

# Mobile UI Verification Protocol

- The implementation scope is the native Android app in `mobile/`, not a responsive/mobile web version. The HTML prototype is used only as the visual design reference for the Android screens.
- The Claude prototype (`/home/natasha/Downloads/Odyssey App.html`) is the authoritative visual reference for the Android app. Do not add, remove, or reinterpret UI elements based on assumptions or personal design decisions.
- Distinguish the browser viewport used to display the prototype from the Android device viewport. Before comparing screens, fix the browser viewport, verify the mockup's actual dimensions, set the outer page scroll to `0`, and explicitly set/record the inner prototype-phone scroll position.
- Never treat an element that is absent from a cropped or incorrectly scrolled screenshot as absent from the design. Validate the complete target screen at the same scroll position first; hidden DOM text is not visual evidence.
- For each screen, capture the prototype and the Android device in the same state and compare the screenshots before making changes. After any change, capture both again and ask the user to verify that screen before proceeding to another screen.
- When the user asks a question or requests an explanation, answer it without changing code unless the user explicitly asks for implementation.

# Git Workflow

- Use `main` as the working branch for this project.
- Do not create feature, agent, or other additional branches for routine work. Make requested changes directly on `main`, then commit and push them there when the user asks.

# Infrastructure and Email Map

This section is the operational source of truth for the current domain, hosting, authentication-email, and human-support-email setup. It must be updated when the provider or responsibility changes. Never put credentials or secret values here.

- **Domain and DNS — GoDaddy:** `ramingo.online` was purchased at GoDaddy. GoDaddy remains the authoritative DNS provider. Do not move nameservers to another provider without explicit user approval and a reviewed record inventory.
- **Web hosting — OVHcloud VPS:** The VPS runs Ubuntu 24.04 and Caddy for the web app. The VPS is not a mailbox. The OVH account currently has no MX Plan, Zimbra Mail, or Email Pro service; do not assume that a VPS subscription includes email hosting.
- **Transactional/auth email — Resend:** Resend is on its free tier and `ramingo.online` is verified. Supabase Auth uses Resend SMTP (`smtp.resend.com`) with sender `no-reply@ramingo.online`. Keep Resend’s `send` MX/SPF records and `resend._domainkey` DKIM record intact. Resend Receiving is not the human inbox solution; it is webhook/API-based.
- **Application backend — Supabase:** Supabase remains the only persistent application data store. Project ref: `hxcavgtlucyoqudbrgse`. Custom SMTP is configured through Resend. The primary Site URL and new Android redirects use `ramingo.online`; legacy `travelplanner.muntim.ru` redirects/host support are retained for installed clients and compatibility.
- **Human support inbox — ImprovMX + Gmail:** ImprovMX is on the free plan. `support@ramingo.online` forwards to `natasha7261@gmail.com`; GoDaddy root MX is `mx1.improvmx.com` (priority 10) and `mx2.improvmx.com` (priority 20), with SPF `v=spf1 include:spf.improvmx.com ~all`. ImprovMX also currently has its auto-created catch-all `*@ramingo.online` forwarding to the same Gmail; disable it from Aliases if unwanted. Free ImprovMX SMTP being `0` is expected; Gmail **Send mail as** is verified and sends through Resend SMTP (`smtp.resend.com:587`, TLS).
- **Google Workspace:** Not purchased and not required. Do not introduce it unless the user explicitly changes the budget/requirements.
- **Secrets:** Never commit or paste SMTP passwords, Resend API keys, Supabase PAT/service-role keys, or private SSH keys. Temporary management tokens must be revoked after use.

## Email change safety

- There must be one deliberate inbound MX provider for the root domain. Do not combine ImprovMX, Resend Receiving, or OVH mail MX records at the same root.
- A mail client (Gmail/Thunderbird) does not create a mailbox; it connects to an inbound provider. A personal Gmail account is sufficient for **Send mail as** when an SMTP provider and verification address exist; Google Workspace is not required.
- Before changing root MX records, preserve the web A/CNAME records and all Resend sending records, then verify the destination address and test both inbound forwarding and outbound Gmail sending. The current deliberate root MX provider is ImprovMX.
- Mail setup has been smoke-tested: public MX/SPF resolve; a message sent to `support@ramingo.online` returns through ImprovMX to Gmail; a message sent from Gmail as `support@ramingo.online` is delivered through `send.ramingo.online` with the `ramingo.online` signature and TLS.
