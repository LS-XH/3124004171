@echo off
setlocal

rem Locate Myapp.jar beside this script so it can be called from any directory.
set "APP_DIR=%~dp0"
set "JAR_FILE=%APP_DIR%Myapp.jar"

if not exist "%JAR_FILE%" (
    echo [ERROR] Myapp.jar was not found: "%JAR_FILE%"
    echo Run .\build.ps1 in the project directory first.
    exit /b 1
)

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java was not found. Install JDK 17 or newer and add it to PATH.
    exit /b 1
)

rem Forward all BAT arguments, such as -n, -r, -e, -a and --help.
java -jar "%JAR_FILE%" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
