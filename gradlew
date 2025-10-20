#!/usr/bin/env sh
# Minimal gradle wrapper shim for Unix-like systems
GRADLE_VERSION=8.4.1
WRAPPER_DIR="$(cd "$(dirname "$0")" && pwd)/.gradle-wrapper"
INSTALL_DIR="$WRAPPER_DIR/gradle-$GRADLE_VERSION"
mkdir -p "$WRAPPER_DIR"
if [ ! -x "$INSTALL_DIR/bin/gradle" ]; then
  echo "Downloading Gradle $GRADLE_VERSION to $WRAPPER_DIR..."
  curl -f -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$WRAPPER_DIR/gradle.zip" || exit 2
  unzip -o "$WRAPPER_DIR/gradle.zip" -d "$WRAPPER_DIR" || exit 3
fi
exec "$INSTALL_DIR/bin/gradle" "$@"
