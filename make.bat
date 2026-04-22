@echo off
REM ============================================================================
REM WorkHub Multi-Tenant SaaS Backend - Windows Batch Scripts
REM ============================================================================
REM Alternative to Makefile for Windows users without GNU Make
REM ============================================================================

setlocal enabledelayedexpansion

if "%~1"=="" goto :help

REM Check which command was requested
if "%~1"=="help" goto :help
if "%~1"=="clean" goto :clean
if "%~1"=="build" goto :build
if "%~1"=="build-fast" goto :build-fast
if "%~1"=="test" goto :test
if "%~1"=="run" goto :run
if "%~1"=="run-fast" goto :run-fast
if "%~1"=="run-dev" goto :run-dev
if "%~1"=="setup-db" goto :setup-db
if "%~1"=="reset-db" goto :reset-db
if "%~1"=="test-api" goto :test-api
if "%~1"=="info" goto :info
if "%~1"=="stop" goto :stop

echo Unknown command: %~1
goto :help

:help
echo.
echo WorkHub Multi-Tenant SaaS Backend - Available Commands:
echo ========================================================
echo.
echo Build Commands:
echo   make clean          Clean build artifacts
echo   make build          Build application with tests
echo   make build-fast     Build without tests
echo   make test           Run all tests
echo.
echo Run Commands:
echo   make run            Start application (PostgreSQL)
echo   make run-fast       Start without running tests first
echo   make run-dev        Start with PostgreSQL local defaults
echo.
echo Database Commands:
echo   make setup-db       Set up PostgreSQL database
echo   make reset-db       Reset database completely
echo.
echo Utility Commands:
echo   make test-api       Test API endpoints
echo   make info           Show project information
echo   make stop           Stop running Java processes
echo.
echo Quick Start:
echo   1. make setup-db     # Set up database
echo   2. make run         # Start application
echo   3. make test-api    # Test the API
echo.
goto :end

:clean
echo Cleaning build artifacts...
call gradlew.bat clean
goto :end

:build
call :set-default-db-env
echo Building application with tests...
call gradlew.bat build
if errorlevel 1 (
    echo.
    echo Build failed. If this is a PostgreSQL auth issue, verify TEST_DB_* values:
    echo   set TEST_DB_URL=jdbc:postgresql://localhost:5432/workhubdb_test
    echo   set TEST_DB_USERNAME=postgres
    echo   set TEST_DB_PASSWORD=your_password
)
goto :end

:build-fast
echo Fast build ^(skipping tests^)...
call gradlew.bat build -x test
goto :end

:test
call :set-default-db-env
echo Running tests...
call gradlew.bat test
if errorlevel 1 (
    echo.
    echo Tests failed. If this is a PostgreSQL auth issue, verify TEST_DB_* values:
    echo   set TEST_DB_URL=jdbc:postgresql://localhost:5432/workhubdb_test
    echo   set TEST_DB_USERNAME=postgres
    echo   set TEST_DB_PASSWORD=your_password
)
goto :end

:run
call :set-default-db-env
echo Starting WorkHub application...
echo Application will be available at: http://localhost:8082
call gradlew.bat bootRun
goto :end

:run-fast
call :set-default-db-env
echo Fast start ^(skipping tests^)...
call gradlew.bat bootRun -x test
goto :end

:run-dev
call :set-default-db-env
echo Running with local PostgreSQL defaults...
call gradlew.bat bootRun
goto :end

:setup-db
echo Setting up PostgreSQL database...
where psql >nul 2>nul
if errorlevel 1 (
    echo ❌ PostgreSQL not found. Use 'make install-postgres' or 'make run-dev' instead
    goto :end
)
echo Creating database 'workhubdb'...
psql -h localhost -U postgres -c "CREATE DATABASE workhubdb;" 2>nul
echo Running setup script...
psql -h localhost -U postgres -d workhubdb -f setup-database.sql
echo ✅ Database setup complete!
goto :end

:reset-db
echo Resetting database...
where psql >nul 2>nul
if errorlevel 1 (
    echo ❌ PostgreSQL not found. Use 'make install-postgres' or 'make run-dev' instead
    goto :end
)
psql -h localhost -U postgres -c "DROP DATABASE IF EXISTS workhubdb;"
psql -h localhost -U postgres -c "CREATE DATABASE workhubdb;"
psql -h localhost -U postgres -d workhubdb -f setup-database.sql
echo ✅ Database reset complete!
goto :end

:check-postgres
echo Checking PostgreSQL installation...
where psql >nul 2>nul
if errorlevel 1 (
    echo ❌ PostgreSQL not found
    echo Install PostgreSQL: https://www.postgresql.org/download/
) else (
    echo ✅ PostgreSQL found
    psql --version
)
goto :end

:install-postgres
echo Installing PostgreSQL...
where choco >nul 2>nul
if errorlevel 1 (
    echo ❌ Chocolatey not found. Install from https://chocolatey.org/
    goto :end
)
echo Installing PostgreSQL via Chocolatey...
choco install postgresql --yes
echo ✅ PostgreSQL installation complete!
echo ⚠️  You may need to restart your terminal/IDE
goto :end

:test-api
echo Testing API endpoints...
echo Testing login endpoint...
curl -X POST http://localhost:8081/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@acme.com\",\"password\":\"password123\"}"
if errorlevel 1 echo ❌ Make sure the application is running (make run)
goto :end

:info
echo Project Information:
echo ======================
echo Name: Multi_Tenant_SaaS_Backend
echo JAR:  build\libs\Multi_Tenant_SaaS_Backend-0.0.1-SNAPSHOT.jar
java -version 2>&1 | findstr "version"
call gradlew.bat --version | findstr "Gradle"
goto :end

:stop
echo Stopping Java processes...
taskkill /F /IM java.exe 2>nul
if errorlevel 1 echo No Java processes found
goto :end

:set-default-db-env
if not defined DB_URL set "DB_URL=jdbc:postgresql://localhost:5432/workhubdb"
if not defined DB_USERNAME set "DB_USERNAME=postgres"
if not defined DB_PASSWORD set "DB_PASSWORD=admin"
if not defined TEST_DB_URL set "TEST_DB_URL=jdbc:postgresql://localhost:5432/workhubdb_test"
if not defined TEST_DB_USERNAME set "TEST_DB_USERNAME=postgres"
if not defined TEST_DB_PASSWORD set "TEST_DB_PASSWORD=admin"
goto :eof

:end