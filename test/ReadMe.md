# WorkHub Multi-Tenant API Testing Suite

Welcome to the testing documentation for the **WorkHub Multi-Tenant SaaS Backend**. This directory contains the resources and execution instructions required to perform end-to-end API testing and verify tenant isolation, database routing, asynchronous report generation, and session security.

---

## 📂 Suite Structure

The test directory is organized as follows:
* **[postman/](file:///d:/EAD/Project/test/postman)**: Contains the Postman collections and local environments.
  * 📄 `WorkHub-MultiTenant.postman_collection.json` — The main suite of HTTP requests.
  * 📄 `WorkHub-Local.postman_environment.json` — Environment config for local testing.
* **[screenshots/](file:///d:/EAD/Project/test/screenshots)**: Step-by-step visual proofs of API executions and tests.

---

## 🚀 How to Run the Tests

To run these integration tests locally, follow these steps:

### 1. Start the Environment
Ensure PostgreSQL, RabbitMQ, and the Spring Boot application are up and running:
```powershell
# Start local containers
docker compose up -d

# Start the application
.\gradlew.bat bootRun
```

### 2. Import into Postman
1. Open Postman.
2. Click **Import** in the top-left corner.
3. Drag and drop both:
   * `postman/WorkHub-MultiTenant.postman_collection.json`
   * `postman/WorkHub-Local.postman_environment.json`
4. Select the **WorkHub Local** environment from the environment dropdown in the top right.

### 3. Run the Suite
You can execute the requests sequentially or run the collection automatically:
* **Manual execution**: Go through the collection from top to bottom.
* **Collection Runner**: Open the Collection Runner in Postman, select the imported collection, and click **Run WorkHub Multi-Tenant API**.

---

## 📸 Visual API Verification & Tour

Below is a detailed guide showing each of the tests represented by the execution screenshots inside `test/screenshots/`. These screenshots serve as evidence of the functional capabilities of our multi-tenant SaaS API.

---

### 🔑 1. User Authentication (Login)
* **Filename:** `post login.jpeg` & `Auth login.jpeg`
* **Request:** `POST /auth/login`
* **Purpose:** Authenticates seeded tenants (e.g., admin or standard users from ACME, Globex, Initech) and retrieves a secure JSON Web Token (JWT).
* **Details:** The credentials are validated against the PostgreSQL database. Upon successful login, the server returns a signed JWT containing roles, email, and the crucial `tenantId` claim. A Postman test script runs immediately, saving this JWT as the `activeToken` variable in the environment.

![Authentication Login](screenshots/post%20login.jpeg)
*(Verification of user login and JWT token issuance)*

---

### 🛡️ 2. Session Identity Verification
* **Filename:** `auth me.jpeg`
* **Request:** `GET /auth/me`
* **Purpose:** Validates the validity of the current JWT session and displays user details.
* **Details:** This request includes `Authorization: Bearer {{activeToken}}` in the headers. The server parses the JWT, extracts the security context, and returns the current user profile including their associated tenant ID (verifying their tenancy at runtime).

![Auth Me Identity](screenshots/auth%20me.jpeg)
*(Active session lookup showing authenticated user profile and resolved tenant)*

---

### 🌐 3. Multi-Tenant Provisioning
* **Filename:** `create tenent.png`
* **Request:** `POST /tenants` (or Admin bootstrap endpoints)
* **Purpose:** Dynamically registers a new tenant under a separate schema.
* **Details:** When a new tenant is created, the system initializes a isolated database schema (e.g., `tenant_3`) and runs migration scripts to prepare the schema structure before any users write data to it.

![Create Tenant](screenshots/create%20tenent.png)
*(Dynamic creation of a new isolated tenant schema)*

---

### 🏗️ 4. Project Creation
* **Filename:** `post project.jpeg` & `tennt create project.png`
* **Request:** `POST /projects` or `POST /projects/with-task`
* **Headers:** `Authorization: Bearer {{activeToken}}`, `X-Tenant-ID: {{activeTenantId}}`
* **Purpose:** Creates a project within the resolved tenant's isolated boundaries.
* **Details:** When this request is received, the `TenantContextFilter` extracts the tenant context. The `Tenant_Connection_Provider` changes the PostgreSQL connection's `search_path` to the tenant's schema before writing, ensuring that the new project is persisted strictly inside that tenant's schema.

![Create Project](screenshots/post%20project.jpeg)
*(Creating a project with automatic schema routing)*

---

### 🔒 5. Verified Tenant Data Isolation
* **Filename:** `tenent get project.png`
* **Request:** `GET /projects`
* **Purpose:** Demonstrates data isolation by verifying that a tenant can only view their own projects.
* **Details:** When calling `GET /projects` as Tenant A, the system queries only Tenant A's schema. The response is limited strictly to Tenant A's projects, confirming that Tenant B's data is completely invisible and out of reach, preventing cross-tenant data leaks.

![Tenant Get Projects](screenshots/tenent%20get%20project.png)
*(Successful isolation verification: only tenant-owned projects are returned)*

---

### 📤 6. Triggering Asynchronous Reports
* **Filename:** `post generate report.jpeg`
* **Request:** `POST /reports/generate` (or `POST /jobs`)
* **Purpose:** Submits an asynchronous request to generate a system report.
* **Details:** The write operation uses the Transactional Outbox pattern. An outbox event is saved in the tenant database schema in the same transaction as the request. An asynchronous process then forwards this event to RabbitMQ, which triggers the job worker.

![Post Generate Report](screenshots/post%20generate%20report.jpeg)
*(Submitting a background job triggering the outbox pattern)*

---

### 📊 7. Monitoring Background Jobs
* **Filename:** `get jops.jpeg`
* **Request:** `GET /jobs`
* **Purpose:** Checks the execution status of asynchronous reporting jobs.
* **Details:** Returns a list of current jobs and their execution states (e.g., `PENDING`, `RUNNING`, `COMPLETED`), showing real-time feedback of the processes handled asynchronously by RabbitMQ.

![Get Background Jobs](screenshots/get%20jops.jpeg)
*(Job status board detailing background reporting task progression)*

---

### 🛡️ 8. JWT Token Structure & Validation
* **Filename:** `post jwt token.jpeg`
* **Request:** Token inspections / parsing
* **Purpose:** Inspects claims, signatures, and headers of the emitted JWT tokens.
* **Details:** Confirms that the JWT payload properly encapsulates the critical tenant identifier claim along with user email and roles, establishing cryptographically verified context propagation.

![JWT Claims Token](screenshots/post%20jwt%20token.jpeg)
*(Cryptographic payload check of emitted JWT token)*
