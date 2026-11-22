#!/usr/bin/env bash
# Build hackathon-template, then start the CheckMate GUI under CheckMateDriver.java. See SKILL.md.
#
#   drive.sh "idle" "size 1400 950" "ss shot.png"   # batch: one driver command per argument
#   drive.sh < commands.txt                          # REPL: one command per line on stdin
#
# Works from any cwd; relative screenshot paths resolve against bim-portal/Java_client.
set -euo pipefail

skill="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"            # <repo>/.claude/skills/run-checkmate
unit="$(cd "$skill/../../../bim-portal/Java_client" && pwd)"   # <repo>/bim-portal/Java_client
cd "$unit"

java_home="${JAVA_HOME:-}"
# Both checks matter: some non-25 JDKs also bundle JavaFX (e.g. Zulu 21 FX). They pass the JavaFX
# check and only fail later in Gradle with "invalid source release: 25".
if [ -z "$java_home" ] || [ ! -f "$java_home/lib/javafx.properties" ] ||
  ! grep -q '^JAVA_VERSION="25' "$java_home/release" 2>/dev/null; then
  echo "drive.sh: JAVA_HOME='$java_home' is not a JDK 25 with bundled JavaFX (e.g. Zulu 25 FX)." >&2
  exit 2
fi

# Compile the module and write the classpath @argfile (build/checkmate-driver.args).
./gradlew -q --console=plain -I "$skill/driver-classpath.init.gradle" :hackathon-template:writeDriverArgs >&2

exec "$java_home/bin/java" \
  @hackathon-template/build/checkmate-driver.args \
  -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
  "$skill/CheckMateDriver.java" "$@"
