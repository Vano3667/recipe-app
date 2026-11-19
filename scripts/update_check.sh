#!/usr/bin/env bash
# Task 8 update check (CI emulator, google_apis image so `adb root` works).
#  1. installs OLD (lower versionCode), launches it so it creates and seeds its database;
#  2. records a fingerprint of the stored recipes (ids, names, createdAt: a wiped and re-seeded database
#     would get new createdAt values, so it cannot pass);
#  3. installs NEW over it with `adb install -r` (no uninstall, no data clearing);
#  4. launches NEW (runs any Room migration) and checks: versionCode increased, firstInstallTime unchanged,
#     identical recipe fingerprint.
# Both APKs must be signed with the same key, otherwise step 3 fails with INSTALL_FAILED_UPDATE_INCOMPATIBLE.
set -euo pipefail
OLD="$1"
NEW="$2"
PKG=com.example.homemenu
DATA="/data/data/$PKG/databases"
WORK=$(mktemp -d)

log() { echo "[update-check] $*"; }

adb wait-for-device
adb root >/dev/null 2>&1 || true
sleep 5
adb wait-for-device

launch() {
  adb shell am start -W -n "$PKG/.MainActivity" >/dev/null
  sleep 25
  adb shell am force-stop "$PKG"
  sleep 3
}

# dumpsys goes to a file first: `grep -m1` on a pipe would end it early and, with pipefail, abort the script.
pkg_field() {
  adb shell dumpsys package "$PKG" > "$WORK/dumpsys.txt"
  grep -m1 "$1" "$WORK/dumpsys.txt" | tr -d '\r' | sed 's/^ *//' || true
}

fingerprint() {
  local dir="$WORK/$1"
  mkdir -p "$dir"
  for f in home_menu.db home_menu.db-wal home_menu.db-shm; do
    adb pull "$DATA/$f" "$dir/$f" >/dev/null 2>&1 || true
  done
  python3 - "$dir/home_menu.db" <<'PY'
import hashlib, sqlite3, sys
c = sqlite3.connect(sys.argv[1])
rows = c.execute("SELECT id, name, createdAt FROM recipes ORDER BY id").fetchall()
h = hashlib.sha256(repr(rows).encode("utf-8")).hexdigest()
print(f"recipes={len(rows)} sha256={h}")
PY
}

log "install OLD"
adb install "$OLD"
launch
BEFORE=$(fingerprint before)
CODE_BEFORE=$(pkg_field versionCode=)
FIRST_BEFORE=$(pkg_field firstInstallTime=)
log "before: $BEFORE | $CODE_BEFORE | $FIRST_BEFORE"
case "$BEFORE" in recipes=0\ *) log "FAIL: the old app stored no recipes"; exit 1 ;; esac

log "install NEW over OLD (adb install -r)"
adb install -r "$NEW"
launch
AFTER=$(fingerprint after)
CODE_AFTER=$(pkg_field versionCode=)
FIRST_AFTER=$(pkg_field firstInstallTime=)
log "after:  $AFTER | $CODE_AFTER | $FIRST_AFTER"

fail=0
[ "$BEFORE" = "$AFTER" ] || { log "FAIL: stored recipes changed during the update"; fail=1; }
[ -n "$FIRST_BEFORE" ] && [ "$FIRST_BEFORE" = "$FIRST_AFTER" ] || { log "FAIL: firstInstallTime changed (app was reinstalled, not updated)"; fail=1; }
[ "$CODE_BEFORE" != "$CODE_AFTER" ] || { log "FAIL: versionCode did not change"; fail=1; }
if [ "$fail" = 0 ]; then log "PASSED: update installed over the existing app, data preserved"; fi
exit "$fail"
