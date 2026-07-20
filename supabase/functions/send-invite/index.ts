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

  const url = Deno.env.get("SUPABASE_URL")!;
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const admin = createClient(url, serviceRoleKey);

  const { email, name, redirectTo } = await request.json();
  if (typeof email !== "string" || !/^\S+@\S+\.\S+$/.test(email)) {
    return new Response(JSON.stringify({ error: "Введите корректный e-mail" }), { status: 400, headers: corsHeaders });
  }
  if (typeof redirectTo !== "string" || !allowedOrigins.some((allowed) => redirectTo.startsWith(allowed))) {
    return new Response(JSON.stringify({ error: "Invalid redirect URL" }), { status: 400, headers: corsHeaders });
  }

  const { error } = await admin.auth.admin.inviteUserByEmail(email, {
    data: { full_name: typeof name === "string" ? name.trim() : "" },
    redirectTo,
  });
  if (error) return new Response(JSON.stringify({ error: error.message }), { status: 400, headers: corsHeaders });

  return new Response(JSON.stringify({ ok: true }), { headers: corsHeaders });
});
