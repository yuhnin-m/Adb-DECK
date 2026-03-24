#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage:
  generate-update-yml.sh --tag <tag> --assets-dir <dir> [--output-dir <dir>]

Description:
  Generates Electron/Nucleus-compatible update metadata files:
  - latest-mac.yml
  - latest.yml (Windows)
  - latest-linux.yml

Expected assets in --assets-dir:
  - ADBDeck-<tag>-macos-arm64.dmg
  - ADBDeck-<tag>-macos-arm64.zip
  - ADBDeck-<tag>-macos-x64.dmg
  - ADBDeck-<tag>-macos-x64.zip
  - ADBDeck-<tag>-windows-x64.msi
  - *.deb and/or *.rpm (Linux packages)
USAGE
}

TAG=""
ASSETS_DIR=""
OUTPUT_DIR=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag)
      TAG="${2:-}"
      shift 2
      ;;
    --assets-dir)
      ASSETS_DIR="${2:-}"
      shift 2
      ;;
    --output-dir)
      OUTPUT_DIR="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$TAG" || -z "$ASSETS_DIR" ]]; then
  usage >&2
  exit 1
fi

if [[ -z "$OUTPUT_DIR" ]]; then
  OUTPUT_DIR="$ASSETS_DIR"
fi

if [[ ! -d "$ASSETS_DIR" ]]; then
  echo "Assets directory not found: $ASSETS_DIR" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

VERSION="${TAG#v}"
VERSION="${VERSION#.}"
RELEASE_DATE="$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")"

file_size() {
  local file="$1"
  if stat -c%s "$file" >/dev/null 2>&1; then
    stat -c%s "$file"
  else
    stat -f%z "$file"
  fi
}

sha512_base64() {
  local file="$1"
  openssl dgst -sha512 -binary "$file" | openssl base64 -A
}

emit_latest_yml() {
  local output_file="$1"
  shift
  local files=("$@")

  if [[ ${#files[@]} -eq 0 ]]; then
    echo "No files provided for $output_file" >&2
    exit 1
  fi

  {
    echo "version: ${VERSION}"
    echo "files:"
    for file in "${files[@]}"; do
      local basename sha size
      basename="$(basename "$file")"
      sha="$(sha512_base64 "$file")"
      size="$(file_size "$file")"
      echo "  - url: ${basename}"
      echo "    sha512: ${sha}"
      echo "    size: ${size}"
    done
    echo "path: $(basename "${files[0]}")"
    echo "sha512: $(sha512_base64 "${files[0]}")"
    echo "releaseDate: '${RELEASE_DATE}'"
  } > "$output_file"
}

MAC_FILES=(
  "${ASSETS_DIR}/ADBDeck-${TAG}-macos-arm64.dmg"
  "${ASSETS_DIR}/ADBDeck-${TAG}-macos-arm64.zip"
  "${ASSETS_DIR}/ADBDeck-${TAG}-macos-x64.dmg"
  "${ASSETS_DIR}/ADBDeck-${TAG}-macos-x64.zip"
)

WINDOWS_FILES=(
  "${ASSETS_DIR}/ADBDeck-${TAG}-windows-x64.msi"
)

for required in "${MAC_FILES[@]}" "${WINDOWS_FILES[@]}"; do
  if [[ ! -f "$required" ]]; then
    echo "Required file is missing: $required" >&2
    exit 1
  fi
done

LINUX_FILES=()
while IFS= read -r linux_file; do
  LINUX_FILES+=("$linux_file")
done < <(find "$ASSETS_DIR" -maxdepth 1 -type f \( -name '*.deb' -o -name '*.rpm' \) | sort)

if [[ ${#LINUX_FILES[@]} -eq 0 ]]; then
  echo "No Linux package files (*.deb, *.rpm) found in: $ASSETS_DIR" >&2
  exit 1
fi

emit_latest_yml "${OUTPUT_DIR}/latest-mac.yml" "${MAC_FILES[@]}"
emit_latest_yml "${OUTPUT_DIR}/latest.yml" "${WINDOWS_FILES[@]}"
emit_latest_yml "${OUTPUT_DIR}/latest-linux.yml" "${LINUX_FILES[@]}"

echo "Generated update metadata files in: ${OUTPUT_DIR}"
