@echo off
REM SmartInventory Backend Server Startup Script for Windows
REM This script starts the Spring Boot backend server on port 5001

echo ==========================================
echo Starting SmartInventory Backend Server
echo ==========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed!
    echo Please install Maven first:
    echo   Download from https://maven.apache.org/
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed!
    echo Please install Java JDK 17 or higher
    pause
    exit /b 1
)

echo Maven found
echo Java found
echo.

REM Navigate to project directory
cd /d "%~dp0"

echo Starting Spring Boot application...
echo Server will be available at: http://localhost:5001
echo API Base URL: http://localhost:5001/api
echo.
echo Press Ctrl+C to stop the server
echo ==========================================
echo.

REM Start the Spring Boot application
mvn spring-boot:run

pause
