@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

if defined JAVA_HOME (
  set "JAVAC=%JAVA_HOME%\bin\javac.exe"
  set "JAR=%JAVA_HOME%\bin\jar.exe"
) else (
  set "JAVAC=javac"
  set "JAR=jar"
)

if not exist "META-INF\MANIFEST.MF" (
  echo ERROR: META-INF\MANIFEST.MF not found.
  exit /b 1
)

if not exist "target\classes" mkdir "target\classes"

echo Compiling...
"%JAVAC%" -d target\classes -encoding UTF-8 -Xlint:all -Werror *.java
if errorlevel 1 exit /b 1

echo Creating BreakThrough.jar...
"%JAR%" cfm BreakThrough.jar META-INF\MANIFEST.MF -C target\classes .
if errorlevel 1 (
  echo ERROR: jar failed. Install a JDK and add JAVA_HOME or put JDK bin on PATH.
  exit /b 1
)

echo.
echo Done: BreakThrough.jar
echo Examples:
echo   java -jar BreakThrough.jar --help
echo   java -jar BreakThrough.jar --host 192.168.0.10 -p 8888 5 rouge
echo   java -jar BreakThrough.jar --gui-hote
endlocal
