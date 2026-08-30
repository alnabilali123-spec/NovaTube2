#!/bin/sh
# Convenience script: download the Gradle wrapper jar so the project builds out of the box.
# Run once after cloning if you don't already have a system Gradle install.
set -e
DIR=$(cd "$(dirname "$0")/.." && pwd)
DEST="$DIR/gradle/wrapper/gradle-wrapper.jar"
URL="https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$DEST" ]; then
  echo "gradle-wrapper.jar already present"
  exit 0
fi
mkdir -p "$(dirname "$DEST")"
echo "Downloading $URL -> $DEST"
curl -fSL -o "$DEST" "$URL"
echo "Done."
