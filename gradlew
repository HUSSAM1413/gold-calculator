#!/usr/bin/env sh
set -e

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="8.9"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
DIST_DIR="$APP_HOME/.gradle-wrapper-cache/gradle-${GRADLE_VERSION}"
ZIP_FILE="$APP_HOME/.gradle-wrapper-cache/gradle-${GRADLE_VERSION}-bin.zip"

if [ ! -x "$DIST_DIR/bin/gradle" ]; then
  mkdir -p "$APP_HOME/.gradle-wrapper-cache"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$ZIP_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$ZIP_FILE"
  else
    echo "Error: curl or wget is required to download Gradle" >&2
    exit 1
  fi
  unzip -qo "$ZIP_FILE" -d "$APP_HOME/.gradle-wrapper-cache"
  chmod +x "$DIST_DIR/bin/gradle"
fi

exec "$DIST_DIR/bin/gradle" "$@"
