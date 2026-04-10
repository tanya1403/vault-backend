#!/bin/sh
# Simplified gradlew - delegates to system gradle or downloads wrapper
set -e
APP_HOME=$(cd "$(dirname "$0")" && pwd)

# If gradle-wrapper.jar exists, use it
if [ -f "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" ]; then
    exec java -jar "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" "$@"
fi

# Fall back to system gradle
exec gradle "$@"
