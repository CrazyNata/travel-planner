import { createClient } from "npm:@supabase/supabase-js@2";

const allowedOrigins = ["https://crazynata.github.io", "http://localhost:5173"];

Deno.serve(async (request) => {
  const origin = request.headers.get("Origin") ?? "";
  const corsHeaders = {
    "Access-Control-Allow-Origin": allowedOrigins.includes(origin) ? origin : allowedOrigins[0],
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Content-Type": "application/json",
  };

  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405, headers: corsHeaders });

  const token = request.headers.get("Authorization")?.replace("Bearer ", "");
  if (!token) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401, headers: corsHeaders });

  const url = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const client = createClient(url, anonKey);
  const { data: { user }, error: userError } = await client.auth.getUser(token);
  if (userError || !user) return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401, headers: corsHeaders });

  const { email, name, redirectTo } = await request.json();
  if (typeof email !== "string" || !/^\S+@\S+\.\S+$/.test(email)) {
    return new Response(JSON.stringify({ error: "Введите корректный e-mail" }), { status: 400, headers: corsHeaders });
  }
  if (typeof redirectTo !== "string" || !allowedOrigins.some((allowed) => redirectTo.startsWith(allowed))) {
    return new Response(JSON.stringify({ error: "Invalid redirect URL" }), { status: 400, headers: corsHeaders });
  }

  const admin = createClient(url, serviceRoleKey);
  const { error } = await admin.auth.admin.inviteUserByEmail(email, {
    data: { full_name: typeof name === "string" ? name.trim() : "", invited_by: user.id },
    redirectTo,
  });
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 400, headers: corsHeaders });

  return new Response(JSON.stringify({ ok: true }), { headers: corsHeaders });
});
