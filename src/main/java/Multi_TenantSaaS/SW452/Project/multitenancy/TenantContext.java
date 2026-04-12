package Multi_TenantSaaS.SW452.Project.multitenancy;

/**
 * TenantContext - Thread-safe utility for managing tenant ID per request.
 * 
 * Uses ThreadLocal to store the current tenant identifier, ensuring each HTTP request
 * thread has its own isolated tenant context. This is critical for schema-per-tenant
 * multi-tenancy where Hibernate needs to know which schema to route queries to.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * Default schema used when no tenant is specified (e.g., for public/shared data
     * or during authentication before tenant is identified).
     */
    public static final String DEFAULT_TENANT = "public";

    private TenantContext() {
        // Utility class - prevent instantiation
    }

    /**
     * Sets the tenant ID for the current thread.
     * 
     * @param tenantId The tenant identifier (maps to database schema name)
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Gets the tenant ID for the current thread.
     * 
     * @return The current tenant ID, or null if not set
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Gets the tenant ID for the current thread, with fallback to default.
     * 
     * @return The current tenant ID, or DEFAULT_TENANT if not set
     */
    public static String getTenantIdOrDefault() {
        String tenantId = CURRENT_TENANT.get();
        return (tenantId != null && !tenantId.isBlank()) ? tenantId : DEFAULT_TENANT;
    }

    /**
     * Clears the tenant context for the current thread.
     * 
     * IMPORTANT: Must be called in a finally block after request processing
     * to prevent memory leaks and thread contamination in thread pools.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
