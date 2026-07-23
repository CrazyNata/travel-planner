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

  const { email, name, role, redirectTo, trip } = await request.json();
  if (typeof email !== "string" || !/^\S+@\S+\.\S+$/.test(email)) {
    return new Response(JSON.stringify({ error: "Введите корректный e-mail" }), { status: 400, headers: corsHeaders });
  }
  if (typeof redirectTo !== "string" || !allowedOrigins.some((allowed) => redirectTo.startsWith(allowed))) {
    return new Response(JSON.stringify({ error: "Invalid redirect URL" }), { status: 400, headers: corsHeaders });
  }

  const inviteeName = typeof name === "string" ? name.trim() : "";
  const inviteeRole = typeof role === "string" ? role.trim() : "Редактор";
  const { data: invite, error: inviteError } = await admin.auth.admin.generateLink({
    type: "invite",
    email,
    options: { data: { full_name: inviteeName }, redirectTo },
  });
  if (inviteError || !invite.properties.action_link) return new Response(JSON.stringify({ error: inviteError?.message || "Не удалось создать ссылку приглашения" }), { status: 400, headers: corsHeaders });

  const resendApiKey = Deno.env.get("RESEND_API_KEY");
  const from = Deno.env.get("RESEND_FROM_EMAIL");
  if (!resendApiKey || !from) return new Response(JSON.stringify({ error: "Сервис приглашений не настроен" }), { status: 503, headers: corsHeaders });

  const displayName = inviteeName || email.split("@")[0];
  const escapeHtml = (value: string) => value.replace(/[&<>'"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" })[character]!);
  const tripData = trip && typeof trip === "object" ? trip as Record<string, unknown> : {};
  const tripTitle = typeof tripData.title === "string" ? tripData.title : "путешествие";
  const tripDates = typeof tripData.dates === "string" ? tripData.dates : "Даты уточняются";
  const tripCities = typeof tripData.cities === "string" ? tripData.cities : "Маршрут уточняется";
  const inviterName = typeof tripData.inviterName === "string" ? tripData.inviterName : "Участник путешествия";
  const participants = Array.isArray(tripData.participants) ? tripData.participants.filter((participant): participant is string => typeof participant === "string") : [];
  const initials = inviterName.split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase();
  const roleDescription = inviteeRole === "Читатель" ? "можно просматривать маршрут" : "можно менять маршрут";
  const html = `<!doctype html><html lang="ru"><body style="margin:0;background:#f3f4f6;font-family:Arial,sans-serif;color:#172033"><table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="padding:22px 12px"><tr><td align="center"><table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#fff;border:1px solid #e2e4e9;border-radius:20px;overflow:hidden"><tr><td style="padding:22px 34px"><table role="presentation" cellspacing="0" cellpadding="0"><tr><td style="width:34px;height:34px;border-radius:9px;background:#4c46d6;color:#fff;font-size:17px;font-weight:700;text-align:center">О</td><td style="padding-left:11px;color:#171d2b;font-size:18px;font-weight:700">Одиссея</td></tr></table></td></tr><tr><td style="padding:38px 34px 30px;background:linear-gradient(120deg,#312b89,#625be3);color:#fff"><p style="margin:0 0 15px;font-size:12px;font-weight:700;letter-spacing:.08em">ПРИГЛАШЕНИЕ В ПОЕЗДКУ</p><h1 style="margin:0 0 12px;font-size:29px;line-height:1.18">Планируем «${escapeHtml(tripTitle)}» вместе</h1><p style="margin:0;color:#f2f1ff;font-size:14px">${escapeHtml(tripDates)} · ${escapeHtml(tripCities)}</p></td></tr><tr><td style="padding:32px 34px 18px"><table role="presentation" cellspacing="0" cellpadding="0"><tr><td style="width:46px;height:46px;border-radius:50%;background:#eeedff;color:#4c46d6;font-size:14px;font-weight:700;text-align:center">${escapeHtml(initials)}</td><td style="padding-left:13px"><b style="display:block;font-size:15px">${escapeHtml(inviterName)}</b><span style="display:block;margin-top:3px;color:#9aa2b3;font-size:13px">приглашает вас как <b style="color:#4c46d6">${escapeHtml(inviteeRole.toLowerCase())}</b></span></td></tr></table><p style="margin:25px 0 0;font-size:15px;line-height:1.65">Привет, ${escapeHtml(displayName)}! Я собираю поездку и хочу спланировать её вместе с вами. Присоединяйтесь - сможете добавлять места в маршрут, бронировать жильё и вести общий бюджет.</p></td></tr><tr><td style="padding:8px 34px 26px"><table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #e6e8ed;border-radius:14px;background:#f8f9fb"><tr><td style="padding:14px 20px;border-bottom:1px solid #e6e8ed;color:#7c8493;font-size:13px">Даты</td><td align="right" style="padding:14px 20px;border-bottom:1px solid #e6e8ed;font-size:13px;font-weight:700">${escapeHtml(tripDates)}</td></tr><tr><td style="padding:14px 20px;border-bottom:1px solid #e6e8ed;color:#7c8493;font-size:13px">Участники</td><td align="right" style="padding:14px 20px;border-bottom:1px solid #e6e8ed;font-size:13px;font-weight:700">${escapeHtml(participants.join(", ") || "Участники")}${participants.length ? " + вы" : ""}</td></tr><tr><td style="padding:14px 20px;color:#7c8493;font-size:13px">Ваша роль</td><td align="right" style="padding:14px 20px;color:#4c46d6;font-size:13px;font-weight:700">${escapeHtml(inviteeRole)} - ${escapeHtml(roleDescription)}</td></tr></table></td></tr><tr><td style="padding:0 34px 16px" align="center"><a href="${invite.properties.action_link}" style="display:inline-block;border-radius:12px;background:#4c46d6;padding:14px 36px;color:#fff;font-size:15px;font-weight:700;text-decoration:none">Принять приглашение</a><p style="margin:16px 0 0;color:#9aa2b3;font-size:12px">Или откройте ссылку:<br><a href="${invite.properties.action_link}" style="color:#4c46d6;text-decoration:none">${escapeHtml(invite.properties.action_link)}</a></p></td></tr><tr><td style="border-top:1px solid #e8e9ed;padding:23px 34px;color:#a4abba;font-size:12px;line-height:1.55;text-align:center">Письмо отправлено сервисом планирования путешествий «Одиссея».<br>Если вы не ожидали это приглашение - просто проигнорируйте письмо.</td></tr></table></td></tr></table></body></html>`;
  const emailResponse = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: { Authorization: `Bearer ${resendApiKey}`, "Content-Type": "application/json" },
    body: JSON.stringify({ from, to: [email], subject: "Вас пригласили в путешествие", html }),
  });
  if (!emailResponse.ok) {
    const error = await emailResponse.json().catch(() => null) as { message?: string } | null;
    return new Response(JSON.stringify({ error: error?.message || "Не удалось отправить письмо" }), { status: 502, headers: corsHeaders });
  }

  return new Response(JSON.stringify({ ok: true }), { headers: corsHeaders });
});
