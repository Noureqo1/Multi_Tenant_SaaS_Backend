# Multi Tenant SaaS Backend - Make targets
# Usage:
#   make help
#   make run
#   make test

ifeq ($(OS),Windows_NT)
SHELL := cmd.exe
.SHELLFLAGS := /C
GRADLEW := gradlew.bat
OPEN_CMD := start
MAIN_DB_ENV := set DB_URL=jdbc:postgresql://localhost:5432/workhubdb&& set DB_USERNAME=postgres&& set DB_PASSWORD=postgres&&
TEST_DB_ENV := set TEST_DB_URL=jdbc:postgresql://localhost:5432/workhubdb_test&& set TEST_DB_USERNAME=postgres&& set TEST_DB_PASSWORD=postgres&&
else
GRADLEW := ./gradlew
OPEN_CMD := xdg-open
MAIN_DB_ENV := DB_URL=jdbc:postgresql://localhost:5432/workhubdb DB_USERNAME=postgres DB_PASSWORD=postgres
TEST_DB_ENV := TEST_DB_URL=jdbc:postgresql://localhost:5432/workhubdb_test TEST_DB_USERNAME=postgres TEST_DB_PASSWORD=postgres
endif

PSQL ?= psql
DB_HOST ?= localhost
DB_PORT ?= 5432
DB_USER ?= postgres
DB_NAME ?= workhubdb
TEST_DB_NAME ?= workhubdb_test

.PHONY: help clean build build-fast test test-local run run-local run-debug init-db init-test-db seed-db reset-db test-report

help:
	@echo Available targets:
	@echo   make clean           - Clean build artifacts
	@echo   make build           - Build project with tests
	@echo   make build-fast      - Build project without tests
	@echo   make test            - Run tests using configured TEST_DB_* vars
	@echo   make test-local      - Run tests with local default Postgres values
	@echo   make run             - Run app using configured DB_* vars
	@echo   make run-local       - Run app with local default Postgres values
	@echo   make run-debug       - Run app with debug logging and stacktraces
	@echo   make init-db         - Create and initialize runtime database
	@echo   make init-test-db    - Create test database
	@echo   make seed-db         - Run SQL seed scripts on runtime database
	@echo   make reset-db        - Drop and recreate runtime/test databases
	@echo   make test-report     - Open test report in browser
	@echo.
	@echo Optional vars:
	@echo   DB_USER, DB_HOST, DB_PORT, DB_NAME, TEST_DB_NAME, PSQL

clean:
	$(GRADLEW) clean

build:
	$(GRADLEW) build

build-fast:
	$(GRADLEW) build -x test

test:
	$(GRADLEW) test

test-local:
	$(TEST_DB_ENV) $(GRADLEW) test

run:
	$(GRADLEW) bootRun

run-local:
	$(MAIN_DB_ENV) $(GRADLEW) bootRun

run-debug:
	$(GRADLEW) bootRun --stacktrace --info

init-db:
	-$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d postgres -c "CREATE DATABASE $(DB_NAME);"
	$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) -f src/main/resources/db/schema-setup.sql
	$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) -f src/main/resources/db/setup-database.sql

init-test-db:
	-$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d postgres -c "CREATE DATABASE $(TEST_DB_NAME);"

seed-db:
	$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) -f src/main/resources/db/schema-setup.sql
	$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) -f src/main/resources/db/setup-database.sql

reset-db:
	-$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d postgres -c "DROP DATABASE IF EXISTS $(DB_NAME);"
	-$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d postgres -c "DROP DATABASE IF EXISTS $(TEST_DB_NAME);"
	$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d postgres -c "CREATE DATABASE $(DB_NAME);"
	$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d postgres -c "CREATE DATABASE $(TEST_DB_NAME);"
	$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) -f src/main/resources/db/schema-setup.sql
	$(PSQL) -h $(DB_HOST) -p $(DB_PORT) -U $(DB_USER) -d $(DB_NAME) -f src/main/resources/db/setup-database.sql

test-report:
	$(OPEN_CMD) build/reports/tests/test/index.html
