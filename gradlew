#!/bin/sh
#
# Gradle wrapper launcher (POSIX)
# Downloads the Gradle distribution specified in gradle/wrapper/gradle-wrapper.properties
# and executes it with the provided arguments.
#
set -e
DIR=$(cd "$(dirname "$0")" && pwd)
APP_HOME="$DIR"
WRAPPER_DIR="$APP_HOME/gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"
WRAPPER_PROPS="$WRAPPER_DIR/gradle-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Gradle wrapper jar is missing at $WRAPPER_JAR." >&2
    echo "Run 'gradle wrapper' once with a local Gradle install, or download the jar from" >&2
    echo "https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar" >&2
    exit 1
fi

exec "${JAVA_HOME:-/usr}/bin/java" \
    -Dorg.gradle.appname="$(basename "$0")" \
    -classpath "$WRAPPER_JAR" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
