# Phase 1 Design Note

## 1. Architecture

The backend follows a standard Spring Boot layered structure:

- `controller` exposes HTTP endpoints and handles request/response mapping.
- `service` contains business rules and transaction boundaries.
- `repository` provides persistence via Spring Data JPA.
- `domain` contains JPA entities.
- `dto` contains request/response models used at the API boundary.
- `security` handles JWT authentication and request filtering.
- `multitenancy` resolves the current tenant and routes Hibernate to the correct schema.

This separation keeps HTTP concerns, business rules, and persistence concerns isolated. The result is easier testing, smaller classes, and clearer ownership of each layer.

## 2. JWT Login and Current User Endpoint

Authentication is based on JWTs.

1. `POST /auth/login` authenticates the user credentials through Spring Security.
2. A successful login returns a signed JWT that includes the user email, role, and tenant ID.
3. `GET /auth/me` reads the authenticated principal and returns the current user profile.

The JWT is the source of identity for API calls after login. The token is also the main place where tenant information is carried across requests.

## 3. Tenant Context Approach

Multi-tenancy is implemented at the schema level.

- `JwtAuthenticationFilter` resolves the authenticated user from the `Authorization: Bearer ...` header.
- `TenantContextFilter` extracts the tenant ID from the JWT claim, with `X-Tenant-ID` as a fallback.
- `TenantContext` stores the current tenant in a `ThreadLocal` for the lifetime of the request.
- `Tenant_Resolver_Impl` supplies the current tenant to Hibernate.
- `Tenant_Connection_Provider` switches the PostgreSQL `search_path` to the tenant schema and resets it after the request.

This design keeps tenant routing out of controller code and makes the tenant choice available to all repository operations during the request.

## 4. Validation and Error Responses

DTO validation is enforced at the controller boundary with `jakarta.validation` annotations and `@Valid`.

- Login requests require a valid email and password.
- Project and task write requests require non-blank required fields.

Validation and runtime failures are normalized in one place with a `@RestControllerAdvice` handler. The API returns a consistent error body with:

- HTTP status
- reason phrase
- human-readable message
- request path
- field-level errors when validation fails

This gives API consumers predictable error responses instead of framework-specific stack traces.

## 5. Transaction Boundary and Rollback

Write operations live in the service layer and are wrapped with `@Transactional`.

The main rollback demonstration is `POST /projects/with-task`, which creates a project and its initial task in one transaction. If the second write fails, the whole transaction rolls back and the project insert is not committed.

The rollback test proves the boundary by forcing the task repository write to fail and then asserting that no project remains persisted after the exception.

This is the intended pattern for future write use-cases: keep the transaction in the service layer, do all related writes inside it, and rely on rollback for partial failure handling.