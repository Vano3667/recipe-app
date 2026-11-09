#!/usr/bin/env bash
# Downloads OFL-licensed fonts (Nunito, Lora) into app/src/main/res/font.
# Required before the first build (R.font.* is referenced from Type.kt).
set -euo pipefail
DIR="app/src/main/res/font"
mkdir -p "$DIR"
BASE="https://github.com/google/fonts/raw/main/ofl"
fetch() {
  if [ -s "$DIR/$2" ]; then echo "ok (cached) $2"; return; fi
  curl -fsSL --retry 3 -o "$DIR/$2" "$BASE/$1" || { echo "ERROR: could not fetch $1"; exit 1; }
  echo "ok $2"
}
fetch "nunito/Nunito%5Bwght%5D.ttf" nunito.ttf
fetch "lora/Lora%5Bwght%5D.ttf" lora.ttf
