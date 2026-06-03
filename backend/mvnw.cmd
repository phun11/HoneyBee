@echo off
REM ============================================================
REM Portable Maven runner for HoneyBee Trace
REM ------------------------------------------------------------
REM Dung khi may chua cai Maven global. Script se tai Maven ve
REM %USERPROFILE%\.honeybee-tools va chay Maven tu do.
REM Can co JDK 17 va Internet trong lan dau chay.
REM ============================================================
setlocal
set MAVEN_VERSION=3.9.9
set TOOL_DIR=%USERPROFILE%\.honeybee-tools
set MVN_HOME=%TOOL_DIR%\apache-maven-%MAVEN_VERSION%
set MVN_EXE=%MVN_HOME%\bin\mvn.cmd
if not exist "%MVN_EXE%" (
  echo [HoneyBee] Maven chua co, dang tai Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "New-Item -ItemType Directory -Force '%TOOL_DIR%' | Out-Null; Invoke-WebRequest 'https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile '%TOOL_DIR%\maven.zip'; Expand-Archive -Force '%TOOL_DIR%\maven.zip' '%TOOL_DIR%'"
)
"%MVN_EXE%" %*
endlocal
