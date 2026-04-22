# Multi Tenant SaaS Backend (PostgreSQL)

This project is now configured to use PostgreSQL by default with schema-per-tenant multi-tenancy.

## 1. Prerequisites

- Java 17
- PostgreSQL 14+
- Gradle wrapper (already included)

## 2. Create Databases

Create two databases:

- `workhubdb` for application runtime
- `workhubdb_test` for tests

Example with `psql`:

```sql
CREATE DATABASE workhubdb;
CREATE DATABASE workhubdb_test;
```

## 3. Configure Environment Variables

PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/workhubdb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="admin"

$env:TEST_DB_URL="jdbc:postgresql://localhost:5432/workhubdb_test"
$env:TEST_DB_USERNAME="postgres"
$env:TEST_DB_PASSWORD="admin"
```

If not provided, the app uses these defaults:

- `DB_URL=jdbc:postgresql://localhost:5432/workhubdb`
- `DB_USERNAME=postgres`
- `DB_PASSWORD=admin`
- `TEST_DB_URL=jdbc:postgresql://localhost:5432/workhubdb_test`
- `TEST_DB_USERNAME=postgres`
- `TEST_DB_PASSWORD=admin`

## 4. Optional: Initialize Sample Schemas/Data

Run SQL scripts from [src/main/resources/db/schema-setup.sql](src/main/resources/db/schema-setup.sql) and [src/main/resources/db/setup-database.sql](src/main/resources/db/setup-database.sql).

Notes:

- The app can auto-create tenant schemas on demand when a tenant is resolved.
- Default tenant schema is `public`.

## 5. Run the Application

```powershell
.\gradlew.bat bootRun
```

App runs on port `8082`.

## 6. Run Tests

```powershell
.\gradlew.bat test
```

The `test` profile uses PostgreSQL (`workhubdb_test`) and `create-drop` DDL mode.

## 7. Phase 1 Deliverables

- API collection: [postman/WorkHub-MultiTenant.postman_collection.json](postman/WorkHub-MultiTenant.postman_collection.json)
- Design note: [DESIGN-NOTE.pdf](DESIGN-NOTE.pdf)
- Main phase-1 transactional write path: `POST /projects/with-task`

## 8. Multi-Tenant Behavior (Schema Per Tenant)

- Tenant is extracted from JWT claim (fallback: `X-Tenant-ID` header).
- Tenant `n` maps to schema `tenant_n`.
- Hibernate connection provider sets PostgreSQL `search_path` to tenant schema.
- On release, connection `search_path` is reset to `public`.

## 9. Useful Commands

```powershell
# Build only
.\gradlew.bat build -x test

# Run only tests
.\gradlew.bat test

# Show test report
start .\build\reports\tests\test\index.html
```

