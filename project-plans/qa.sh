#!/usr/bin/env bash
# Drive the phone through a sequence of steps in one invocation, so a single
# checklist item costs one call instead of three.
#
#   ./qa.sh launch , sleep 3 , tap 538 1001 , text "hi" , back , shot login
#
# Steps are separated by a bare ",". Screenshots go to $QA_OUT and their paths
# are printed, so the next step is to read the file that was just named.
#
# The device address is discovered over mDNS: wireless debugging hands out a new
# port every time it is toggled, so hardcoding one guarantees a stale address.
# Override with QA_DEVICE=ip:port if discovery is unavailable.
set -uo pipefail

ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
QA_OUT="${QA_OUT:-$HOME/ramingo-qa-shots}"
PKG="${QA_PKG:-com.odyssey.travelplanner.debug}"
ACTIVITY="$PKG/com.odyssey.travelplanner.MainActivity"
mkdir -p "$QA_OUT"

discover() {
    "$ADB" mdns services 2>/dev/null \
        | awk '/_adb-tls-connect\._tcp/ {print $3; exit}'
}

if [ -z "${QA_DEVICE:-}" ]; then
    QA_DEVICE="$(discover)"
    [ -z "$QA_DEVICE" ] && { echo "no device advertised over mDNS; set QA_DEVICE=ip:port" >&2; exit 1; }
fi
"$ADB" connect "$QA_DEVICE" >/dev/null 2>&1

run() { "$ADB" -s "$QA_DEVICE" "$@"; }

step() {
    local cmd="$1"; shift
    case "$cmd" in
        tap)    run shell input tap "$1" "$2" ;;
        swipe)  run shell input swipe "$1" "$2" "$3" "$4" "${5:-300}" ;;
        text)   run shell input text "$1" ;;
        key)    run shell input keyevent "$@" ;;
        back)   run shell input keyevent 4 ;;
        home)   run shell input keyevent 3 ;;
        sleep)  sleep "$1" ;;
        shot)   run exec-out screencap -p > "$QA_OUT/$1.png"; echo "SHOT $QA_OUT/$1.png" ;;
        launch) run shell am start -n "$ACTIVITY" >/dev/null ;;
        stop)   run shell am force-stop "$PKG" ;;
        install) run install -r "$1" | tail -1 ;;
        clear)  run logcat -c ;;
        crash)  echo "CRASH_LINES $(run logcat -d -b crash 2>/dev/null | grep -c "$PKG")" ;;
        alive)  echo "PID $(run shell pidof "$PKG" | tr -d '\r' | grep . || echo none)" ;;
        *)      echo "unknown step: $cmd" >&2; exit 1 ;;
    esac
}

args=()
for a in "$@"; do
    if [ "$a" = "," ]; then
        [ ${#args[@]} -gt 0 ] && step "${args[@]}"
        args=()
    else
        args+=("$a")
    fi
done
[ ${#args[@]} -gt 0 ] && step "${args[@]}"
exit 0
