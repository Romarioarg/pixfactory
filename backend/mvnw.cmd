@echo off
setlocal
set "JAVA_HOME=%JAVA_HOME%"
if "%JAVA_HOME%"=="" if exist "C:\Program Files\Java\jdk-24" set "JAVA_HOME=C:\Program Files\Java\jdk-24"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0mvnw.ps1" %*
exit /b %ERRORLEVEL%
