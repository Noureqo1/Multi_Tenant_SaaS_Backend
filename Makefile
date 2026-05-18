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
MAIN_DB_ENV := set DB_URL=jdbc:postgresql://localhost:5433/workhubdb&& set DB_USERNAME=postgres&& set DB_PASSWORD=postgres&&
TEST_DB_ENV := set TEST_DB_URL=jdbc:postgresql://localhost:5433/workhubdb_test&& set TEST_DB_USERNAME=postgres&& set TEST_DB_PASSWORD=admin&&
else
GRADLEW := ./gradlew
OPEN_CMD := xdg-open
MAIN_DB_ENV := DB_URL=jdbc:postgresql://localhost:5433/workhubdb DB_USERNAME=postgres DB_PASSWORD=postgres
TEST_DB_ENV := TEST_DB_URL=jdbc:postgresql://localhost:5433/workhubdb_test TEST_DB_USERNAME=postgres TEST_DB_PASSWORD=admin
endif

PSQL ?= psql
DB_HOST ?= localhost
DB_PORT ?= 5433
DB_USER ?= postgres
DB_NAME ?= workhubdb
TEST_DB_NAME ?= workhubdb_test

.PHONY: help clean build build-fast test test-local run run-local run-debug init-db init-test-db seed-db reset-db test-report docker-up docker-down docker-restart docker-logs k8s-deploy k8s-delete k8s-status k8s-canary-deploy k8s-canary-delete test-integration test-k6 rabbitmq-status

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
	@echo Docker targets:
	@echo   make docker-up       - Start the full docker-compose stack in background
	@echo   make docker-down     - Stop and tear down all docker-compose containers
	@echo   make docker-restart  - Restart the docker-compose services
	@echo   make docker-logs     - Tail the live logs of the docker-compose stack
	@echo.
	@echo Kubernetes (K8s) targets:
	@echo   make k8s-deploy      - Deploy standard services, deployment, configmap, secrets
	@echo   make k8s-delete      - Delete standard deployments
	@echo   make k8s-status      - View pods, services, deployments in workhub namespace
	@echo   make k8s-canary-deploy - Deploy Canary routing, services, and rate-limited Ingress
	@echo   make k8s-canary-delete - Delete Canary deployment configurations
	@echo.
	@echo Testing & Observability targets:
	@echo   make test-integration - Run Python end-to-end integration and async outbox flow demo
	@echo   make test-k6         - Run k6 performance load tests against defined SLO thresholds
	@echo   make rabbitmq-status - Print live RabbitMQ queues and routing status from active broker
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

# --- Docker Targets ---
docker-up:
	docker compose up -d

docker-down:
	docker compose down

docker-restart:
	docker compose down && docker compose up -d

docker-logs:
	docker compose logs -f

# --- Kubernetes Targets ---
k8s-deploy:
	kubectl apply -f k8s/

k8s-delete:
	kubectl delete -f k8s/

k8s-status:
	kubectl get all -n workhub

k8s-canary-deploy:
	kubectl apply -f k8s/canary/

k8s-canary-delete:
	kubectl delete -f k8s/canary/

# --- Testing & Messaging Targets ---
test-integration:
	python scripts/demo.py

test-k6:
	k6 run k6/load-test.js

rabbitmq-status:
	docker exec workhub-rabbitmq rabbitmqctl list_queues
