package Multi_TenantSaaS.SW452.Project.multitenancy;

import Multi_TenantSaaS.SW452.Project.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TenantContextFilter - Extracts tenant ID from requests and populates TenantContext.
 *
 * Production behavior:
 * - Tenant is extracted only from JWT claims.
 *
 * Test behavior:
 * - X-Tenant-ID header fallback can be enabled for testing cross-tenant scenarios.
 *
 * The filter ensures the TenantContext is ALWAYS cleared in a finally block
 * to prevent memory leaks and thread contamination in connection pools.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after JwtAuthenticationFilter
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Value("${app.tenancy.allow-header-fallback:false}")
    private boolean allowHeaderFallback;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String tenantId = extractTenantId(request);

            if (tenantId != null && !tenantId.isBlank()) {
                String schemaName = formatTenantSchemaName(tenantId);
                TenantContext.setTenantId(schemaName);
                log.debug("Tenant context set to: {}", schemaName);
            } else {
                log.debug("No tenant ID found, using default schema");
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            log.debug("Tenant context cleared");
        }
    }

    /**
     * Extracts tenant ID from the request.
     * Priority:
     * 1) JWT token claim
     * 2) X-Tenant-ID header (ONLY if enabled, e.g. in test profile)
     */
    private String extractTenantId(HttpServletRequest request) {
        String tenantId = extractTenantFromJwt(request);

        if ((tenantId == null || tenantId.isBlank()) && allowHeaderFallback) {
            tenantId = request.getHeader(TENANT_HEADER);
            if (tenantId != null && !tenantId.isBlank()) {
                log.debug("Tenant ID extracted from header (test fallback): {}", tenantId);
            }
        }

        return tenantId;
    }

    /**
     * Extracts tenant ID from the JWT token's claims.
     */
    private String extractTenantFromJwt(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }

        try {
            String jwt = authHeader.substring(BEARER_PREFIX.length());
            Long tenantId = jwtService.extractTenantId(jwt);

            if (tenantId != null) {
                log.debug("Tenant ID extracted from JWT: {}", tenantId);
                return tenantId.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to extract tenant ID from JWT: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Formats the tenant ID into a valid PostgreSQL schema name.
     * Schema names follow the pattern: tenant_{id}
     */
    private String formatTenantSchemaName(String tenantId) {
        String sanitized = tenantId.replaceAll("[^a-zA-Z0-9_]", "");
        return "tenant_" + sanitized;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/login") ||
                path.startsWith("/auth/register") ||
                path.startsWith("/actuator");
    }
}