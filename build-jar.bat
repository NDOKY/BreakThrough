@echo off
setlocal
cd /d "%~dp0"

rem JAVA_HOME must point to the JDK root, e.g. C:\...\jdk-21.0.x
rem (NOT the bin folder). If you mistakenly set JAVA_HOME to ...\bin, we fix it below.
set "JDK_BIN="
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JDK_BIN=%JAVA_HOME%\bin"
  ) else if exist "%JAVA_HOME%\javac.exe" (
    set "JDK_BIN=%JAVA_HOME%"
  )
)

set "JAVAC="
set "JAR_EXE="
if defined JDK_BIN (
  if exist "%JDK_BIN%\javac.exe" set "JAVAC=%JDK_BIN%\javac.exe"
  if exist "%JDK_BIN%\jar.exe" set "JAR_EXE=%JDK_BIN%\jar.exe"
)

if not defined JAVAC (
  for /f "delims=" %%i in ('where javac 2^>nul') do (
    set "JAVAC=%%i"
    set "JAR_EXE=%%~dpi\jar.exe"
    goto :javac_done
  )
  set "JAVAC=javac"
)
:javac_done

if defined JAR_EXE if exist "%JAR_EXE%" goto :jar_ok

if not exist "%JAR_EXE%" (
  for /f "delims=" %%i in ('where javac 2^>nul') do (
    set "JAR_EXE=%%~dpi\jar.exe"
    goto :jar_next_to_javac
  )
)
:jar_next_to_javac

if exist "%JAR_EXE%" goto :jar_ok

for /f "delims=" %%j in ('where jar 2^>nul') do (
  set "JAR_EXE=%%j"
  goto :jar_ok
)

echo.
echo ERROR: jar.exe introuvable.
echo.
if defined JAVA_HOME (
  echo JAVA_HOME=%JAVA_HOME%
  if defined JDK_BIN (
    echo Dossier bin utilise : %JDK_BIN%
  ) else (
    echo JAVA_HOME ne pointe pas vers un JDK valide ^(racine du JDK ou dossier bin^).
  )
) else (
  echo JAVA_HOME n'est pas defini dans cette fenetre.
)
echo javac utilise : "%JAVAC%"
echo.
echo Verifications :
echo   1. JAVA_HOME = racine du JDK ^(pas ...\bin^), exemple :
echo      C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot
echo   2. PATH contient : %%JAVA_HOME%%\bin   ^(ou le chemin complet vers bin^)
echo   3. Fermez TOUTES les fenetres PowerShell/CMD et Visual Studio Code, puis rouvrez :
echo      les variables systeme ne s'appliquent qu'aux nouveaux processus.
echo   4. Dans PowerShell :  .\build-jar.bat   ^(avec .\ au debut^)
echo.
echo Cause frequente : Oracle javapath a seulement javac sans jar ; utilisez le bin du JDK.
echo JDK complet : https://adoptium.net/
echo.
exit /b 1

:jar_ok
if not exist "META-INF\MANIFEST.MF" (
  echo ERROR: META-INF\MANIFEST.MF not found.
  exit /b 1
)

if not exist "target\classes" mkdir "target\classes"

echo Using: "%JAVAC%"
echo Using: "%JAR_EXE%"
echo.
echo Compiling...
"%JAVAC%" -d target\classes -encoding UTF-8 -Xlint:all -Werror *.java
if errorlevel 1 exit /b 1

echo Creating Breakthrough.jar...
"%JAR_EXE%" cfm Breakthrough.jar META-INF\MANIFEST.MF -C target\classes .
if errorlevel 1 (
  echo ERROR: la commande jar a echoue.
  exit /b 1
)

echo.
echo Done: Breakthrough.jar
echo Examples:
echo   java -jar Breakthrough.jar --help
echo   java -jar Breakthrough.jar --host 192.168.0.10 -p 8888 5 rouge
echo   java -jar Breakthrough.jar --nogui
endlocal
