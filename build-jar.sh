#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"

if [[ -n "${JAVA_HOME}" ]]; then
  JAVAC="${JAVA_HOME}/bin/javac"
  JAR="${JAVA_HOME}/bin/jar"
else
  JAVAC="javac"
  JAR="jar"
fi

if [[ ! -f META-INF/MANIFEST.MF ]]; then
  echo "ERROR: META-INF/MANIFEST.MF not found." >&2
  exit 1
fi

mkdir -p target/classes

echo "Compiling..."
"${JAVAC}" -d target/classes -encoding UTF-8 -Xlint:all -Werror *.java

echo "Creating BreakThrough.jar..."
"${JAR}" cfm BreakThrough.jar META-INF/MANIFEST.MF -C target/classes .

echo ""
echo "Done: BreakThrough.jar"
echo "Examples:"
echo "  java -jar BreakThrough.jar --help"
echo "  java -jar BreakThrough.jar --host 192.168.0.10 -p 8888 5 rouge"
echo "  java -jar BreakThrough.jar --nogui"
