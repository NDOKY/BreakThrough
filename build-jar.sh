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

echo "Compiling..."
"${JAVAC}" -encoding UTF-8 *.java

echo "Creating BreakThrough.jar..."
"${JAR}" cfm BreakThrough.jar META-INF/MANIFEST.MF *.class

echo ""
echo "Done: BreakThrough.jar"
echo "Examples:"
echo "  java -jar BreakThrough.jar --help"
echo "  java -jar BreakThrough.jar --host 192.168.0.10 -p 8888 5 rouge"
echo "  java -jar BreakThrough.jar --gui-hote"
