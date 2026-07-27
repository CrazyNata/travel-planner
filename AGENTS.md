# Data Storage Rules

- Supabase is the only persistent store for application data.
- Do not use `localStorage`, `sessionStorage`, IndexedDB, cookies, or browser caches for application data, drafts, or UI state.
- Browser storage is permitted only for Supabase Auth tokens. When users select "Remember me", use persistent auth storage; otherwise keep the auth session only for the current browser session.
- Keep transient UI state only in React state. Persist user-visible data through Supabase tables or Storage before treating a change as saved.
