@echo off
chcp 65001 >nul
echo === TG WS Proxy Go Build ===
echo Project root: %~dp0
echo Output dir:   %~dp0app\src\main\jniLibs
echo.
echo Checking for Docker...

docker info >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker Desktop is not running or not installed.
    echo Please start Docker Desktop and try again.
    pause
    exit /b 1
)

echo Running build-go.sh via Git Bash...
bash "%~dp0build-go.sh"

if %ERRORLEVEL% neq 0 (
    echo Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo === Build finished ===
dir /s /b "%~dp0app\src\main\jniLibs"
pause
