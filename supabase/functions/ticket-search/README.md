# Ticket search

This function proxies the Tiqets Distributor API so the Tiqets API token never ships in the Android APK. It searches Tiqets products by city coordinates (preferred) or city name and returns the existing ticket-card shape used by the app.

Configure the project secret before using the function. Add the token in Supabase Dashboard → Edge Functions → Secrets, or with the CLI:

```sh
supabase secrets set TIQETS_API_TOKEN="..."
```

The production API is used by default. A different HTTPS base URL can be supplied for an approved Tiqets test environment:

```sh
supabase secrets set TIQETS_BASE_URL="https://api.tiqets.com/v2"
supabase functions deploy ticket-search --project-ref hxcavgtlucyoqudbrgse
```

The endpoint accepts a city, optional attraction query, optional latitude/longitude, a radius in kilometres, a language code (`ru`, `en`, `es` or `de`), and a result limit. It returns Tiqets product title, description, EUR price, rating, review count, photo and affiliate checkout URL. Search results are not persisted; user-selected tickets continue to be saved inside the existing trip payload.
