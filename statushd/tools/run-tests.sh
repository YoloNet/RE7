#!/bin/bash
# Ujian JVM untuk logik tulen StatusHD (FfmpegCommandBuilder).
# Menggunakan pengkompil Kotlin terbenam dalam Gradle 9.8.2 (2.4.10)
# — tiada Gradle daemon, tiada Android SDK diperlukan.
set -e
cd "$(dirname "$0")/.."
export JAVA_HOME="$HOME/jdk21"
JAVA="$JAVA_HOME/bin/java"
KOTLINC_JAR="$HOME/gradle-dist/gradle-9.8.2/lib/kotlin-compiler-embeddable-2.4.10.jar"
STDLIB_JAR="$HOME/gradle-dist/gradle-9.8.2/lib/kotlin-stdlib-2.4.10.jar"
TROUBLESHOOT_JAR="$HOME/gradle-dist/gradle-9.8.2/lib/kotlin-script-runtime-2.4.10.jar"
# Pengkompil Kotlin perlukan stdlib + reflect + coroutines di classpath-nya;
# cara paling mudah: seluruh lib/ Gradle (semua jar Kotlin ada di situ).
GRADLE_LIB="$HOME/gradle-dist/gradle-9.8.2/lib"
OUT="/tmp/statushd-test"
rm -rf "$OUT" && mkdir -p "$OUT"
echo "Compiling FfmpegCommandBuilder + test..."
"$JAVA" -cp "$GRADLE_LIB/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  app/src/main/java/com/statushd/app/media/FfmpegCommandBuilder.kt \
  tools/test/FfmpegCommandBuilderTest.kt \
  -cp "$STDLIB_JAR" -d "$OUT/test.jar" -no-stdlib 2>&1 | grep -v "^w:" || true
echo "Running tests..."
"$JAVA" -cp "$OUT/test.jar:$STDLIB_JAR:$TROUBLESHOOT_JAR" com.statushd.app.media.FfmpegCommandBuilderTestKt
