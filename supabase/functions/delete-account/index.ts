import { createClient } from "npm:@supabase/supabase-js@2";

const allowedOrigins = [
  "https://travelplanner.muntim.ru",
  "https://crazynata.github.io",
  "http://localhost:5173",
];

function headersFor(origin: string) {
  return {
    "Access-Control-Allow-Origin": allowedOrigins.includes(origin)
      ? origin
      : allowedOrigins[0],
    "Access-Control-Allow-Headers":
      "authorization, x-client-info, apikey, content-type",
    "Content-Type": "application/json",
  };
}

function json(body: Record<string, unknown>, status: number, headers: Record<string, string>) {
  return new Response(JSON.stringify(body), { status, headers });
}

Deno.serve(async (request) => {
  const headers = headersFor(request.headers.get("Origin") ?? "");
  if (request.method === "OPTIONS") return new Response("ok", { headers });
  if (request.method !== "POST") {
    return json({ error: "Method not allowed" }, 405, headers);
  }

  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) {
    return json({ error: "Authentication required" }, 401, headers);
  }

  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  if (!serviceRoleKey || !supabaseUrl) {
    console.error("Supabase server credentials are not configured");
    return json({ error: "Account deletion is temporarily unavailable" }, 503, headers);
  }

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const token = authorization.slice("Bearer ".length);
  const { data: { user }, error: userError } = await admin.auth.getUser(token);
  if (userError || !user) {
    return json({ error: "Invalid or expired session" }, 401, headers);
  }

  try {
    const { data: ownedTrips, error: tripsLookupError } = await admin
      .from("trips")
      .select("id")
      .eq("owner_id", user.id);
    if (tripsLookupError) throw tripsLookupError;

    const ownedTripIds = (ownedTrips ?? []).map((trip) => trip.id as string);
    const objectLookups = await Promise.all([
      admin
        .schema("storage")
        .from("objects")
        .select("name")
        .eq("bucket_id", "trip-photos")
        .like("name", `${user.id}/profile/%`),
      ...ownedTripIds.map((tripId) =>
        admin
          .schema("storage")
          .from("objects")
          .select("name")
          .eq("bucket_id", "trip-photos")
          .like("name", `%/${tripId}/%`)
      ),
    ]);
    const failedLookup = objectLookups.find((lookup) => lookup.error);
    if (failedLookup?.error) throw failedLookup.error;

    const objectNames = objectLookups
      .flatMap((lookup) => lookup.data ?? [])
      .map((object) => object.name as string)
      .filter(Boolean);
    const uniqueObjectNames = [...new Set(objectNames)];

    // Revoke refresh tokens before deleting data. The current access token can
    // remain valid until its short JWT expiry, so sensitive database access
    // must continue to rely on the rows removed below.
    const revokeResponse = await fetch(`${supabaseUrl}/auth/v1/logout?scope=global`, {
      method: "POST",
      headers: {
        apikey: serviceRoleKey,
        Authorization: authorization,
      },
    });
    if (!revokeResponse.ok && revokeResponse.status !== 401) {
      throw new Error(`Could not revoke sessions (${revokeResponse.status})`);
    }

    // Remove only the profile photo and photos belonging to trips the user
    // owns. Uploads made by this user in somebody else's shared trip remain.
    for (let index = 0; index < uniqueObjectNames.length; index += 100) {
      const batch = uniqueObjectNames.slice(index, index + 100);
      const { error: storageError } = await admin.storage
        .from("trip-photos")
        .remove(batch);
      if (storageError) throw storageError;
    }

    const { error: relationalError } = await admin.rpc(
      "delete_account_relational_data",
      { p_user_id: user.id },
    );
    if (relationalError) throw relationalError;

    const { error: deleteUserError } = await admin.auth.admin.deleteUser(user.id);
    if (deleteUserError) throw deleteUserError;

    return json({ ok: true }, 200, headers);
  } catch (error) {
    console.error("Account deletion failed", error);
    return json({ error: "Could not delete the account" }, 500, headers);
  }
});
