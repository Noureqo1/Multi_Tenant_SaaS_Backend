package Multi_TenantSaaS.SW452.Project.multitenancy;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;


@Component
@Slf4j
public class Tenant_Connection_Provider implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    public Tenant_Connection_Provider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        log.debug("Getting connection for tenant: {}", tenantIdentifier);
        
        Connection connection = getAnyConnection();
        
        try {
            String schemaName = sanitizeSchemaName(tenantIdentifier);

            // Ensure schema exists so new tenants can be used immediately.
            connection.createStatement().execute(
                "CREATE SCHEMA IF NOT EXISTS " + schemaName
            );

            // Set the PostgreSQL search_path to the tenant's schema
            // This ensures all unqualified table references resolve to this schema
            connection.createStatement().execute(
                "SET search_path TO " + schemaName
            );
            log.debug("Schema switched to: {}", schemaName);
        } catch (SQLException e) {
            log.error("Failed to switch schema to: {}", tenantIdentifier, e);
            throw new SQLException("Could not switch to tenant schema: " + tenantIdentifier, e);
        }
        
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Reset to default schema before returning connection to pool
            connection.createStatement().execute(
                "SET search_path TO " + TenantContext.DEFAULT_TENANT
            );
            log.debug("Schema reset to default for connection release");
        } catch (SQLException e) {
            log.warn("Failed to reset schema on connection release", e);
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // Return false to keep connections until transaction completes
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    /**
     * Sanitizes the schema name to prevent SQL injection.
     * PostgreSQL schema names must be valid identifiers.
     */
    private String sanitizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            return TenantContext.DEFAULT_TENANT;
        }
        // Only allow alphanumeric characters and underscores
        String sanitized = schemaName.replaceAll("[^a-zA-Z0-9_]", "");
        return sanitized.isBlank() ? TenantContext.DEFAULT_TENANT : sanitized;
    }
}
