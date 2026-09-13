#!/usr/bin/env bash
set -euo pipefail

env_file="${RAMINGO_PAYMENT_REMINDERS_ENV_FILE:-/etc/ramingo/payment-reminders.env}"
if [[ ! -r "$env_file" ]]; then
  echo "Payment reminder environment file is missing: $env_file" >&2
  exit 1
fi

# shellcheck disable=SC1090
source "$env_file"
: "${SUPABASE_URL:?SUPABASE_URL is required}"
: "${PAYMENT_REMINDERS_SECRET:?PAYMENT_REMINDERS_SECRET is required}"

# This POST is intentionally not retried: a lost response after Resend accepts
# the message could otherwise produce a duplicate reminder.
exec /usr/bin/curl \
  --fail \
  --silent \
  --show-error \
  --connect-timeout 15 \
  --max-time 120 \
  --request POST \
  "${SUPABASE_URL%/}/functions/v1/payment-reminders" \
  --header "Content-Type: application/json" \
  --header "x-payment-reminders-secret: ${PAYMENT_REMINDERS_SECRET}" \
  --data '{}'
