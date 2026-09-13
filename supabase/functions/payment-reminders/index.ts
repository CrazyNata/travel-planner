import { createClient } from "npm:@supabase/supabase-js@2";

type JsonObject = Record<string, unknown>;

type ReminderKind = "three_days" | "today";

type PaymentReminder = {
  kind: ReminderKind;
  tripId: string;
  tripTitle: string;
  accommodationId: string;
  accommodationName: string;
  city: string;
  dates: string;
  paymentDeadline: string;
  price: string;
  bookingUrl: string;
};

type TripRow = {
  id: string;
  owner_id: string | null;
  payload: unknown;
};

const PROJECT_SITE_URL = "https://ramingo.online";
const DEFAULT_TIME_ZONE = "Europe/Prague";
const REMINDER_HEADER = "x-payment-reminders-secret";

const isJsonObject = (value: unknown): value is JsonObject =>
  typeof value === "object" && value !== null && !Array.isArray(value);

const stringValue = (value: unknown) =>
  typeof value === "string" ? value.trim() : "";

const firstString = (...values: unknown[]) => {
  for (const value of values) {
    const text = stringValue(value);
    if (text) return text;
  }
  return "";
};

const firstNonEmptyArray = (...values: unknown[]) => {
  const firstArray = values.find((value): value is unknown[] => Array.isArray(value));
  return values.find(
    (value): value is unknown[] => Array.isArray(value) && value.length > 0,
  ) || firstArray || [];
};

const escapeHtml = (value: string) =>
  value.replace(/[&<>'"]/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "'": "&#39;",
    '"': "&quot;",
  })[character]!);

function jsonResponse(body: JsonObject, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Cache-Control": "no-store",
      "Content-Type": "application/json; charset=utf-8",
    },
  });
}

function secretsMatch(actual: string, expected: string) {
  if (!actual || actual.length !== expected.length) return false;
  let difference = 0;
  for (let index = 0; index < expected.length; index += 1) {
    difference |= actual.charCodeAt(index) ^ expected.charCodeAt(index);
  }
  return difference === 0;
}

function localDateString(now: Date, timeZone: string) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    day: "2-digit",
    month: "2-digit",
    timeZone,
    year: "numeric",
  }).formatToParts(now);
  const values = Object.fromEntries(
    parts
      .filter((part) => part.type !== "literal")
      .map((part) => [part.type, part.value]),
  );
  return `${values.year}-${values.month}-${values.day}`;
}

function dateOnly(value: unknown) {
  const raw = stringValue(value);
  const match = raw.match(/^(\d{4}-\d{2}-\d{2})/);
  if (!match) return "";
  const normalized = match[1];
  const parsed = new Date(`${normalized}T00:00:00Z`);
  if (Number.isNaN(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== normalized) {
    return "";
  }
  return normalized;
}

function daysUntil(today: string, deadline: string) {
  const todayTime = Date.parse(`${today}T00:00:00Z`);
  const deadlineTime = Date.parse(`${deadline}T00:00:00Z`);
  if (!Number.isFinite(todayTime) || !Number.isFinite(deadlineTime)) return null;
  return Math.round((deadlineTime - todayTime) / 86_400_000);
}

function formatPaymentDate(value: string) {
  const parsed = new Date(`${value}T12:00:00Z`);
  if (Number.isNaN(parsed.getTime())) return value;
  return new Intl.DateTimeFormat("ru-RU", {
    day: "numeric",
    month: "long",
    timeZone: "UTC",
    year: "numeric",
  }).format(parsed);
}

function accommodationIsPaid(stay: JsonObject) {
  return stringValue(stay.status).toLocaleLowerCase("ru-RU") === "оплачено";
}

function accommodationsFromPayload(payload: JsonObject) {
  const nestedData = isJsonObject(payload.data) ? payload.data : {};
  const nestedTrip = isJsonObject(nestedData.trip) ? nestedData.trip : {};
  return firstNonEmptyArray(
    payload.accommodations,
    nestedTrip.accommodations,
    nestedData.accommodations,
  ).filter(isJsonObject);
}

function tripTitleFromPayload(payload: JsonObject) {
  const nestedData = isJsonObject(payload.data) ? payload.data : {};
  const nestedTrip = isJsonObject(nestedData.trip) ? nestedData.trip : {};
  return firstString(payload.title, nestedTrip.title) || "Путешествие";
}

function reminderFromStay(
  trip: TripRow,
  payload: JsonObject,
  stay: JsonObject,
  index: number,
  today: string,
): PaymentReminder | null {
  if (accommodationIsPaid(stay)) return null;
  const paymentDeadline = dateOnly(stay.paymentDeadline);
  if (!paymentDeadline) return null;
  const difference = daysUntil(today, paymentDeadline);
  const kind = difference === 3
    ? "three_days"
    : difference === 0
    ? "today"
    : null;
  if (!kind) return null;

  return {
    kind,
    tripId: trip.id,
    tripTitle: tripTitleFromPayload(payload),
    accommodationId: firstString(stay.id) || `${trip.id}-accommodation-${index + 1}`,
    accommodationName: firstString(stay.name) || "Жильё",
    city: firstString(stay.city),
    dates: firstString(stay.dates),
    paymentDeadline,
    price: firstString(stay.price),
    bookingUrl: firstString(stay.bookingUrl),
  };
}

function safeBookingUrl(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:" ? url.toString() : "";
  } catch {
    return "";
  }
}

function tripAccommodationUrl(tripId: string) {
  return `${PROJECT_SITE_URL}/#/trips/${encodeURIComponent(tripId)}/accommodation`;
}

function reminderLabel(kind: ReminderKind) {
  return kind === "today" ? "Оплатить сегодня" : "Оплатить через 3 дня";
}

function reminderDescription(kind: ReminderKind, deadline: string) {
  return kind === "today"
    ? "Срок оплаты наступил сегодня."
    : `Срок оплаты — ${formatPaymentDate(deadline)}.`;
}

function renderEmail(reminders: PaymentReminder[]) {
  const hasToday = reminders.some((reminder) => reminder.kind === "today");
  const subject = hasToday
    ? "Ramingo: сегодня нужно оплатить жильё"
    : "Ramingo: жильё нужно оплатить через 3 дня";
  const rows = reminders.map((reminder) => {
    const city = reminder.city ? `${escapeHtml(reminder.city)} · ` : "";
    const price = reminder.price ? `<strong>${escapeHtml(reminder.price)}</strong>` : "";
    const bookingUrl = safeBookingUrl(reminder.bookingUrl);
    const links = [
      `<a href="${escapeHtml(tripAccommodationUrl(reminder.tripId))}" style="color:#5145cd;text-decoration:none;font-weight:700">Открыть жильё в Ramingo →</a>`,
      bookingUrl
        ? `<a href="${escapeHtml(bookingUrl)}" style="color:#777b8d;text-decoration:none">Ссылка на бронирование</a>`
        : "",
    ].filter(Boolean).join(" · ");
    return `<tr>
      <td style="padding:18px 0;border-bottom:1px solid #ebeaf2">
        <div style="color:#5145cd;font-size:12px;font-weight:800;letter-spacing:.02em">${escapeHtml(reminderLabel(reminder.kind))}</div>
        <div style="margin-top:7px;color:#171827;font-size:18px;line-height:1.25;font-weight:800">${escapeHtml(reminder.accommodationName)}</div>
        <div style="margin-top:5px;color:#777b8d;font-size:13px;line-height:1.5">${city}${escapeHtml(reminder.tripTitle)}</div>
        <div style="margin-top:10px;color:#333749;font-size:14px;line-height:1.5">${escapeHtml(reminderDescription(reminder.kind, reminder.paymentDeadline))}${price ? ` · ${price}` : ""}</div>
        <div style="margin-top:12px;font-size:12px;line-height:1.5">${links}</div>
      </td>
    </tr>`;
  }).join("");
  const text = reminders.map((reminder) => {
    const price = reminder.price ? ` · ${reminder.price}` : "";
    const booking = safeBookingUrl(reminder.bookingUrl);
    return [
      reminderLabel(reminder.kind),
      `${reminder.accommodationName}${reminder.city ? `, ${reminder.city}` : ""}`,
      reminder.tripTitle,
      reminderDescription(reminder.kind, reminder.paymentDeadline) + price,
      tripAccommodationUrl(reminder.tripId),
      booking,
    ].filter(Boolean).join("\n");
  }).join("\n\n");

  const html = `<!doctype html>
<html lang="ru">
  <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>${escapeHtml(subject)}</title></head>
  <body style="margin:0;padding:0;background:#eef0f6;color:#171827;font-family:Inter,Arial,Helvetica,sans-serif">
    <div style="display:none;max-height:0;overflow:hidden;opacity:0">${escapeHtml(subject)}</div>
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="padding:24px 10px;background:#eef0f6">
      <tr><td align="center">
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:650px;border:1px solid #dedcf2;border-radius:24px;overflow:hidden;background:#fff">
          <tr><td style="padding:34px 38px 28px;background:#f7f5ff">
            <div style="color:#5145cd;font-size:12px;font-weight:800;letter-spacing:.12em;text-transform:uppercase">RAMINGO · НАПОМИНАНИЕ</div>
            <h1 style="margin:15px 0 0;color:#171827;font-size:30px;line-height:1.1;letter-spacing:-.04em">${escapeHtml(subject.replace("Ramingo: ", ""))}</h1>
            <p style="margin:13px 0 0;color:#777b8d;font-size:14px;line-height:1.6">Проверьте оплату жилья в ваших путешествиях.</p>
          </td></tr>
          <tr><td style="padding:10px 38px 28px;background:#fff">
            <table role="presentation" width="100%" cellspacing="0" cellpadding="0">${rows}</table>
          </td></tr>
          <tr><td style="border-top:1px solid #ebeaf2;padding:20px 38px;color:#9a9eaf;font-size:11px;line-height:1.6;text-align:center">Ramingo · Travel planner<br><a href="mailto:support@ramingo.online" style="color:#5145cd;text-decoration:none">support@ramingo.online</a></td></tr>
        </table>
      </td></tr>
    </table>
  </body>
</html>`;
  return { subject, html, text };
}

async function sendEmail(
  apiKey: string,
  recipient: string,
  reminders: PaymentReminder[],
) {
  const email = renderEmail(reminders);
  const response = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from: "Ramingo <no-reply@ramingo.online>",
      to: [recipient],
      subject: email.subject,
      html: email.html,
      text: email.text,
    }),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null) as { message?: string } | null;
    throw new Error(error?.message || `Resend returned HTTP ${response.status}`);
  }
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok");
  if (request.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  const expectedSecret = Deno.env.get("PAYMENT_REMINDERS_SECRET") || "";
  const providedSecret = request.headers.get(REMINDER_HEADER) || "";
  if (!secretsMatch(providedSecret, expectedSecret)) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }

  let input: JsonObject = {};
  try {
    const body = await request.json();
    if (isJsonObject(body)) input = body;
  } catch {
    // An empty body is valid for the scheduled runner.
  }
  const dryRun = input.dryRun === true;
  const timeZone = Deno.env.get("PAYMENT_REMINDERS_TIME_ZONE") || DEFAULT_TIME_ZONE;
  const today = localDateString(new Date(), timeZone);
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  const resendApiKey = Deno.env.get("RESEND_API_KEY");
  const configuredRecipient = stringValue(
    Deno.env.get("PAYMENT_REMINDERS_RECIPIENT"),
  ).toLowerCase();
  if (!supabaseUrl || !serviceRoleKey || (!resendApiKey && !dryRun)) {
    console.error("payment-reminders: missing configuration", {
      supabaseUrl: Boolean(supabaseUrl),
      serviceRoleKey: Boolean(serviceRoleKey),
      resendApiKey: Boolean(resendApiKey),
    });
    return jsonResponse({ error: "Сервис напоминаний не настроен" }, 503);
  }

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const { data, error } = await admin
    .from("trips")
    .select("id,owner_id,payload");
  if (error) {
    console.error("payment-reminders: trip lookup failed", error);
    return jsonResponse({ error: "Не удалось проверить путешествия" }, 500);
  }

  const remindersByOwner = new Map<string, PaymentReminder[]>();
  let paidSkipped = 0;
  let invalidSkipped = 0;
  for (const row of (data || []) as TripRow[]) {
    if (!row.owner_id || !isJsonObject(row.payload)) {
      invalidSkipped += 1;
      continue;
    }
    if (stringValue(row.payload.deletedAt)) continue;
    const tripReminders = accommodationsFromPayload(row.payload)
      .map((stay, index) => reminderFromStay(row, row.payload as JsonObject, stay, index, today));
    const paidCount = accommodationsFromPayload(row.payload).filter(accommodationIsPaid).length;
    paidSkipped += paidCount;
    for (const reminder of tripReminders) {
      if (!reminder) continue;
      const ownerReminders = remindersByOwner.get(row.owner_id) || [];
      ownerReminders.push(reminder);
      remindersByOwner.set(row.owner_id, ownerReminders);
    }
  }

  if (dryRun) {
    return jsonResponse({
      ok: true,
      dryRun: true,
      today,
      timeZone,
      recipients: remindersByOwner.size,
      reminders: [...remindersByOwner.values()].flat().map((reminder) => ({
        kind: reminder.kind,
        tripId: reminder.tripId,
        tripTitle: reminder.tripTitle,
        accommodationId: reminder.accommodationId,
        accommodationName: reminder.accommodationName,
        paymentDeadline: reminder.paymentDeadline,
      })),
      paidSkipped,
      invalidSkipped,
    });
  }

  let sent = 0;
  let skippedWithoutEmail = 0;
  const failures: string[] = [];
  const remindersByRecipient = new Map<string, PaymentReminder[]>();
  for (const [ownerId, reminders] of remindersByOwner) {
    let recipient = configuredRecipient;
    let userLookupFailed = false;
    if (!recipient) {
      const { data: user, error: userError } = await admin.auth.admin.getUserById(ownerId);
      recipient = stringValue(user?.user?.email).toLowerCase();
      userLookupFailed = Boolean(userError);
    }
    if (userLookupFailed || !recipient) {
      skippedWithoutEmail += 1;
      continue;
    }
    remindersByRecipient.set(recipient, [
      ...(remindersByRecipient.get(recipient) || []),
      ...reminders,
    ]);
  }

  for (const [recipient, reminders] of remindersByRecipient) {
    try {
      await sendEmail(resendApiKey!, recipient, reminders);
      sent += 1;
    } catch (sendError) {
      console.error("payment-reminders: email delivery failed", {
        recipient,
        error: sendError instanceof Error ? sendError.message : String(sendError),
      });
      failures.push(recipient);
    }
  }

  return jsonResponse({
    ok: failures.length === 0,
    today,
    timeZone,
    recipients: remindersByRecipient.size,
    sent,
    skippedWithoutEmail,
    paidSkipped,
    invalidSkipped,
    failures: failures.length,
  }, failures.length ? 502 : 200);
});
