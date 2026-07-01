#!/usr/bin/env bash
set -euo pipefail

metadata_url="${FAND_METADATA_URL:-https://repo.fandmc.cn/repository/maven-public/io/fand/fand-api/maven-metadata.xml}"
base="${BASE_VERSION:-}"
input="${RELEASE_VERSION_INPUT:-}"

if [[ -z "$base" && -f gradle.properties ]]; then
  base="$(grep '^releaseVersion=' gradle.properties | cut -d= -f2 || true)"
fi

if [[ -z "$base" && -f build.gradle.kts ]]; then
  base="$(sed -n 's/.*providers\.gradleProperty("releaseVersion").*orElse("\([^"]*\)").*/\1/p' build.gradle.kts | head -n1)"
fi

if [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
  input="${GITHUB_REF_NAME#v}"
fi

if [[ -n "$input" ]]; then
  base="${input%%+build.*}"
fi

base="${base%%+build.*}"
base="${base%-SNAPSHOT}"

if [[ -z "$base" ]]; then
  echo "release base version must be provided" >&2
  exit 1
fi

if ! metadata="$(curl -fsSL -H 'Cache-Control: no-cache' "$metadata_url")"; then
  echo "failed to read Maven metadata from $metadata_url" >&2
  exit 1
fi

max=0
while IFS= read -r version; do
  case "$version" in
    "$base"+build.*)
      number="${version#"$base"+build.}"
      if [[ "$number" =~ ^[0-9]+$ ]] && (( number > max )); then
        max="$number"
      fi
      ;;
  esac
done < <(printf '%s\n' "$metadata" | sed -n 's#.*<version>\(.*\)</version>.*#\1#p')

version="${base}+build.$((max + 1))"
if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+\+build\.[0-9]+$ ]]; then
  echo "derived release version must match x.x.x+build.x, got '$version'" >&2
  exit 1
fi

echo "$version"
