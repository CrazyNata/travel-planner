import { createClient } from "npm:@supabase/supabase-js@2";

const allowedOrigins = [
  "https://ramingo.online",
  "https://travelplanner.muntim.ru",
  "https://crazynata.github.io",
  "http://localhost:5173",
];

type JsonObject = Record<string, unknown>;

const isJsonObject = (value: unknown): value is JsonObject =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const stringValue = (value: unknown) =>
  typeof value === "string" ? value.trim() : "";

const firstString = (...values: unknown[]) => {
  for (const value of values) {
    const result = stringValue(value);
    if (result) return result;
  }
  return "";
};

const heroMarkBase64 = "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";

const oneLine = (value: string) => value.replace(/[\r\n]+/g, " ").trim();

const escapeHtml = (value: string) =>
  value.replace(/[&<>'"]/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "'": "&#39;",
    '"': "&quot;",
  })[character]!);

const initialsFor = (value: string) => {
  const initials = value
    .split(/\s+/)
    .filter(Boolean)
    .map((part) => part[0] || "")
    .join("")
    .slice(0, 2)
    .toUpperCase();
  return initials || value.slice(0, 2).toUpperCase() || "R";
};

const isAllowedRedirect = (value: string) => {
  try {
    return allowedOrigins.includes(new URL(value).origin);
  } catch {
    return false;
  }
};

Deno.serve(async (request) => {
  const origin = request.headers.get("Origin") ?? "";
  const corsHeaders = {
    "Access-Control-Allow-Origin": allowedOrigins.includes(origin)
      ? origin
      : allowedOrigins[0],
    "Access-Control-Allow-Headers":
      "authorization, x-client-info, apikey, content-type",
    "Content-Type": "application/json; charset=utf-8",
  };
  const jsonResponse = (body: JsonObject, status = 200) =>
    new Response(JSON.stringify(body), { status, headers: corsHeaders });

  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return jsonResponse({ error: "Некорректное тело запроса" }, 400);
  }
  const input = isJsonObject(body) ? body : {};
  const email = stringValue(input.email).toLowerCase();
  const inviteeName = stringValue(input.name);
  const inviteeRole = input.role === "Читатель" ? "Читатель" : "Редактор";
  const redirectTo = stringValue(input.redirectTo);
  const tripId = stringValue(input.tripId);

  if (!/^\S+@\S+\.\S+$/.test(email)) {
    return jsonResponse({ error: "Введите корректный e-mail" }, 400);
  }
  if (!isAllowedRedirect(redirectTo)) {
    return jsonResponse({ error: "Недопустимая ссылка приглашения" }, 400);
  }
  if (!tripId) {
    return jsonResponse({ error: "Не указана поездка" }, 400);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const resendApiKey = Deno.env.get("RESEND_API_KEY");
  const from = "Ramingo <no-reply@ramingo.online>";
  if (!supabaseUrl || !serviceRoleKey || !resendApiKey) {
    console.error("send-invite: missing configuration", {
      supabaseUrl: Boolean(supabaseUrl),
      serviceRoleKey: Boolean(serviceRoleKey),
      resendApiKey: Boolean(resendApiKey),
    });
    return jsonResponse({ error: "Сервис приглашений не настроен" }, 503);
  }

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
      detectSessionInUrl: false,
    },
  });
  const token = request.headers.get("Authorization")?.match(/^Bearer\s+(.+)$/i)?.[1];
  const userResult = token ? await admin.auth.getUser(token) : null;
  const inviter = userResult?.data.user ?? null;
  const { data: trip, error: tripError } = await admin
    .from("trips")
    .select("owner_id,payload")
    .eq("id", tripId)
    .maybeSingle();
  if (tripError) {
    return jsonResponse({ error: "Не удалось проверить поездку" }, 500);
  }
  if (!inviter || !trip || trip.owner_id !== inviter.id) {
    return jsonResponse({ error: "Недостаточно прав для приглашения" }, 403);
  }

  const tripPayload = isJsonObject(trip.payload) ? trip.payload : {};
  const nestedData = isJsonObject(tripPayload.data) ? tripPayload.data : {};
  const nestedTrip = isJsonObject(nestedData.trip) ? nestedData.trip : {};
  const memberValues = Array.isArray(tripPayload.members)
    ? tripPayload.members
    : Array.isArray(nestedTrip.members)
    ? nestedTrip.members
    : [];
  const members = memberValues.filter(isJsonObject);
  const ownerMember = members.find(
    (member) => stringValue(member.role) === "Владелец",
  );
  const inviterMetadata = isJsonObject(inviter.user_metadata)
    ? inviter.user_metadata
    : {};
  const inviterEmail = stringValue(inviter.email);
  const inviterName = firstString(
    inviterMetadata.full_name,
    inviterMetadata.name,
    ownerMember?.name,
    inviterEmail.split("@")[0],
  ) || "Участник путешествия";
  const tripTitle = firstString(tripPayload.title, nestedTrip.title) ||
    "Путешествие";
  const tripDates = firstString(tripPayload.dates, nestedTrip.dates) ||
    "Даты уточняются";
  const tripHeroDates = tripDates
    .replace(/\s*·\s*\d+\s+дн(?:ей|я)?\s*$/i, "")
    .trim();
  const tripHeroSummary = tripHeroDates;

  const { data: users, error: usersError } = await admin.auth.admin.listUsers({
    page: 1,
    perPage: 1000,
  });
  if (usersError) {
    return jsonResponse({ error: "Не удалось проверить пользователя" }, 500);
  }
  const existingUser = users.users.find(
    (user) => user.email?.toLowerCase() === email,
  );
  let inviteeId = existingUser?.id;
  let actionLink = "";
  if (existingUser) {
    const { data, error } = await admin.auth.admin.generateLink({
      type: "magiclink",
      email,
      options: { redirectTo },
    });
    if (error) {
      return jsonResponse({ error: error.message }, 400);
    }
    actionLink = data?.properties?.action_link || "";
  } else {
    const { data, error } = await admin.auth.admin.generateLink({
      type: "invite",
      email,
      options: {
        data: {
          full_name: inviteeName,
          invite_pending: true,
          invite_trip_id: tripId,
          invite_role: inviteeRole,
        },
        redirectTo,
      },
    });
    if (error || !data?.user?.id) {
      return jsonResponse({
        error: error?.message || "Не удалось создать приглашение",
      }, 400);
    }
    inviteeId = data.user.id;
    actionLink = data.properties?.action_link || "";
  }
  if (!inviteeId || !actionLink) {
    return jsonResponse({ error: "Не удалось создать ссылку приглашения" }, 400);
  }

  // Persist access and the member row before sending the message. The RPC is
  // idempotent for an email/user pair and locks the trip row, so repeated
  // invitations cannot create duplicate members or partial permissions.
  const { error: invitationError } = await admin.rpc(
    "upsert_trip_invitation",
    {
      p_trip_id: tripId,
      p_actor_id: inviter.id,
      p_user_id: inviteeId,
      p_email: email,
      p_name: inviteeName,
      p_role: inviteeRole,
    },
  );
  if (invitationError) {
    console.error("send-invite: could not persist invitation", invitationError);
    return jsonResponse({ error: "Не удалось выдать доступ к поездке" }, 500);
  }

  const subject = oneLine(
    `Вас пригласили в поездку «${tripTitle}»`,
  ).slice(0, 180);
  const inviterAddedVerb = /(?:а|я)$/i.test(inviterName) ? "добавила" : "добавил";
  const safeActionLink = escapeHtml(actionLink);
  const actionLabel = existingUser
    ? "Войти и присоединиться"
    : "Создать аккаунт и присоединиться";
  const actionHint = existingUser
    ? "Откройте ссылку — Ramingo выполнит вход и откроет поездку."
    : "Откройте ссылку, придумайте пароль и завершите регистрацию в Ramingo.";
  // Supplied reference layout: compass mark, centered hero, sender card, facts and CTA.
  const html = `<!doctype html>
<html lang="ru">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${escapeHtml(subject)}</title>
  </head>
  <body style="margin:0;padding:0;background:#eef0f6;color:#171827;font-family:Inter,Arial,Helvetica,sans-serif">
    <div style="display:none;max-height:0;overflow:hidden;opacity:0">${escapeHtml(subject)}</div>
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="padding:6px 10px 24px;background:#eef0f6">
      <tr>
        <td align="center">
          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:680px;border:1px solid #dedcf2;border-radius:30px;overflow:hidden;background:#ffffff">
            <tr>
              <td style="padding:0;background:#f7f5ff;text-align:center">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border-collapse:collapse;background:#f7f5ff">
                  <tr>
                    <td align="center" valign="top" style="padding:62px 42px 42px;background:#f7f5ff;text-align:center;vertical-align:top">
                      <img src="cid:ramingo-mark" width="84" height="84" alt="" style="display:block;width:84px;height:84px;margin:0 auto;border:0">
                      <div style="margin-top:15px;color:#171827;font-size:29px;line-height:1;font-weight:800;letter-spacing:-.04em">Ramingo</div>
                      <div style="margin-top:7px;color:#9295a7;font-size:10px;line-height:1;font-weight:800;letter-spacing:.11em;text-transform:uppercase">Travel planner</div>
                      <div style="margin-top:39px;color:#4f43bf;font-size:11px;line-height:1.3;font-weight:800;letter-spacing:.16em;text-transform:uppercase">ВАШ НОВЫЙ МАРШРУТ</div>
                      <h1 style="max-width:470px;margin:18px auto 0;color:#171827;font-size:34px;line-height:1.08;font-weight:800;letter-spacing:-.055em">${escapeHtml(inviterName)} ${inviterAddedVerb} вас в поездку</h1>
                      <p style="max-width:430px;margin:15px auto 0;color:#777b8d;font-size:14px;line-height:1.55">«${escapeHtml(tripTitle)}» · ${escapeHtml(tripHeroSummary)}</p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td style="padding:32px 42px 38px;background:#ffffff">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="border:1px solid #e6e3f5;border-radius:17px;background:#fbfaff">
                  <tr>
                    <td style="padding:16px;vertical-align:middle">
                      <table role="presentation" cellspacing="0" cellpadding="0" style="border-collapse:collapse">
                        <tr>
                          <td width="42" height="42" style="width:42px;height:42px;padding:0;border-radius:12px;background:#eeecff;text-align:center;vertical-align:middle">
                            <img src="cid:ramingo-mark" width="30" height="30" alt="" style="display:block;width:30px;height:30px;margin:0 auto;border:0">
                          </td>
                          <td style="padding:0 0 0 12px;vertical-align:middle">
                            <strong style="display:block;color:#171827;font-size:16px;line-height:1.35">${escapeHtml(inviterName)}</strong>
                            <span style="display:block;margin-top:3px;color:#777c8e;font-size:13px;line-height:1.45">приглашает вас присоединиться к поездке</span>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
                <p style="max-width:510px;margin:27px auto 24px;color:#333749;font-size:16px;line-height:1.7;text-align:center">${escapeHtml(actionHint)}</p>
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0">
                  <tr>
                    <td width="32%" valign="top" style="padding:16px 12px;border:1px solid #e8e6f3;border-radius:14px;background:#ffffff">
                      <span style="display:block;color:#85899a;font-size:11px;line-height:1.3">Поездка</span>
                      <strong style="display:block;margin-top:6px;color:#282b3b;font-size:13px;line-height:1.35">${escapeHtml(tripTitle)}</strong>
                    </td>
                    <td width="8" style="width:8px;font-size:0;line-height:0">&nbsp;</td>
                    <td width="32%" valign="top" style="padding:16px 12px;border:1px solid #e8e6f3;border-radius:14px;background:#ffffff">
                      <span style="display:block;color:#85899a;font-size:11px;line-height:1.3">Даты</span>
                      <strong style="display:block;margin-top:6px;color:#282b3b;font-size:13px;line-height:1.35">${escapeHtml(tripHeroDates)}</strong>
                    </td>
                    <td width="8" style="width:8px;font-size:0;line-height:0">&nbsp;</td>
                    <td width="32%" valign="top" style="padding:16px 12px;border:1px solid #e8e6f3;border-radius:14px;background:#ffffff">
                      <span style="display:block;color:#85899a;font-size:11px;line-height:1.3">Роль</span>
                      <strong style="display:block;margin-top:6px;color:#4f43bf;font-size:13px;line-height:1.35">${escapeHtml(inviteeRole)}</strong>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            <tr>
              <td align="center" style="padding:2px 42px 38px;background:#ffffff;text-align:center">
                <a href="${safeActionLink}" style="display:inline-block;border-radius:13px;padding:15px 28px;color:#ffffff;background:#6c5ce7;box-shadow:0 9px 20px #6c5ce744;font-size:14px;font-weight:800;text-decoration:none">${escapeHtml(actionLabel)}</a>
                <p style="max-width:470px;margin:15px auto 0;color:#969aaa;font-size:11px;line-height:1.55">Ссылка безопасна и действует только для этого приглашения.</p>
              </td>
            </tr>
            <tr>
              <td style="border-top:1px solid #e8e8f0;padding:20px 32px;color:#9a9eaf;font-size:11px;line-height:1.6;text-align:center">Ramingo · Travel planner<br><a href="mailto:support@ramingo.online" style="color:#4f43bf;text-decoration:none">support@ramingo.online</a></td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>`;

  const emailResponse = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${resendApiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from,
      to: [email],
      ...(inviterEmail ? { reply_to: inviterEmail } : {}),
      subject,
      html,
      attachments: [{
        content: heroMarkBase64,
        filename: "ramingo-mark.png",
        content_id: "ramingo-mark",
        content_type: "image/png",
      }],
    }),
  });
  if (!emailResponse.ok) {
    const error = await emailResponse.json().catch(() => null) as
      { message?: string } | null;
    return jsonResponse({
      error: error?.message || "Не удалось отправить письмо",
    }, 502);
  }

  return jsonResponse({ ok: true });
});
