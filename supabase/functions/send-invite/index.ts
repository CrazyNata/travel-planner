import { createClient } from "npm:@supabase/supabase-js@2";

const allowedOrigins = [
  "https://ramingo.online",
  "https://travelplanner.muntim.ru",
  "https://crazynata.github.io",
  "http://localhost:5173",
];

Deno.serve(async (request) => {
  const origin = request.headers.get("Origin") ?? "";
  const corsHeaders = {
    "Access-Control-Allow-Origin": allowedOrigins.includes(origin)
      ? origin
      : allowedOrigins[0],
    "Access-Control-Allow-Headers":
      "authorization, x-client-info, apikey, content-type",
    "Content-Type": "application/json",
  };
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") {
    return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405, headers: corsHeaders });
  }

  const { email, name, role, redirectTo, tripId } = await request.json();
  if (typeof email !== "string" || !/^\S+@\S+\.\S+$/.test(email)) {
    return new Response(JSON.stringify({ error: "Введите корректный e-mail" }), { status: 400, headers: corsHeaders });
  }
  if (typeof redirectTo !== "string" || !allowedOrigins.some((allowed) => redirectTo.startsWith(allowed))) {
    return new Response(JSON.stringify({ error: "Недопустимая ссылка приглашения" }), { status: 400, headers: corsHeaders });
  }
  if (typeof tripId !== "string" || !tripId) {
    return new Response(JSON.stringify({ error: "Не указана поездка" }), { status: 400, headers: corsHeaders });
  }

  const admin = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
  );
  const token = request.headers.get("Authorization")?.replace("Bearer ", "");
  const { data: { user: inviter } } = token
    ? await admin.auth.getUser(token)
    : { data: { user: null } };
  const { data: trip } = await admin
    .from("trips")
    .select("owner_id")
    .eq("id", tripId)
    .maybeSingle();
  if (!inviter || trip?.owner_id !== inviter.id) {
    return new Response(JSON.stringify({ error: "Недостаточно прав для приглашения" }), { status: 403, headers: corsHeaders });
  }

  const inviteeName = typeof name === "string" ? name.trim() : "";
  const inviteeRole = role === "Читатель" ? "Читатель" : "Редактор";
  const { data: users, error: usersError } = await admin.auth.admin.listUsers({
    page: 1,
    perPage: 1000,
  });
  if (usersError) {
    return new Response(JSON.stringify({ error: "Не удалось проверить пользователя" }), { status: 500, headers: corsHeaders });
  }
  const existingUser = users.users.find(
    (user) => user.email?.toLowerCase() === email.toLowerCase(),
  );
  let inviteeId = existingUser?.id;
  if (existingUser) {
    const { error } = await admin.auth.signInWithOtp({
      email,
      options: { emailRedirectTo: redirectTo },
    });
    if (error) {
      return new Response(JSON.stringify({ error: error.message }), { status: 400, headers: corsHeaders });
    }
  } else {
    const { data, error } = await admin.auth.admin.inviteUserByEmail(email, {
      data: { full_name: inviteeName },
      redirectTo,
    });
    if (error || !data.user) {
      return new Response(JSON.stringify({ error: error?.message || "Не удалось отправить приглашение" }), { status: 400, headers: corsHeaders });
    }
    inviteeId = data.user.id;
  }
  const { error: collaboratorError } = await admin
    .from("trip_collaborators")
    .upsert({ trip_id: tripId, user_id: inviteeId, role: inviteeRole });
  if (collaboratorError) {
    return new Response(JSON.stringify({ error: "Не удалось выдать доступ к поездке" }), { status: 500, headers: corsHeaders });
  }
  return new Response(JSON.stringify({ ok: true }), { headers: corsHeaders });
});
