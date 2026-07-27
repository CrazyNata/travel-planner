import { createClient } from "@supabase/supabase-js";

const url = import.meta.env.VITE_SUPABASE_URL;
const publishableKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;
let rememberSession = true;

if (!url || !publishableKey) {
  throw new Error("Supabase environment variables are not configured.");
}

export function setAuthSessionPersistence(remember: boolean) {
  rememberSession = remember;
}

const authStorage: Storage = {
  getItem(key) {
    return localStorage.getItem(key) ?? sessionStorage.getItem(key);
  },
  setItem(key, value) {
    const target = rememberSession ? localStorage : sessionStorage;
    const other = rememberSession ? sessionStorage : localStorage;
    other.removeItem(key);
    target.setItem(key, value);
  },
  removeItem(key) {
    localStorage.removeItem(key);
    sessionStorage.removeItem(key);
  },
  clear() {
    // Supabase removes its token with removeItem; never clear unrelated storage.
  },
  key(index) {
    return localStorage.key(index) ?? sessionStorage.key(index);
  },
  get length() {
    return localStorage.length + sessionStorage.length;
  },
};

export const supabase = createClient(url, publishableKey, {
  auth: { persistSession: true, autoRefreshToken: true, storage: authStorage },
});
