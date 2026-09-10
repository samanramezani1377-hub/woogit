#!/usr/bin/env bash
set -euo pipefail

BASE="${1:-HEAD^}"
HEAD="${2:-HEAD}"
mapfile -t changed < <(git diff --name-only "$BASE" "$HEAD")

code_changed=0
map_changed=0
for path in "${changed[@]}"; do
  case "$path" in
    docs/agent/*) map_changed=1 ;;
    app/**|data/**|domain/**|presentation/**|core/**|build.gradle.kts|settings.gradle.kts|gradle.properties|gradle/**|*.gradle|*.gradle.kts)
      code_changed=1 ;;
  esac
done

if (( code_changed == 1 && map_changed == 0 )); then
  echo "AGENT MAP GUARD FAILED: App code/build contract changed without a docs/agent/** update."
  echo "Update the relevant architecture/API/JSON/session/error/source map in the same change."
  printf '%s\n' "${changed[@]}"
  exit 1
fi

echo "AGENT MAP GUARD PASSED: code changes are accompanied by an agent-map update, or this is a map-only/non-code change."
