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
