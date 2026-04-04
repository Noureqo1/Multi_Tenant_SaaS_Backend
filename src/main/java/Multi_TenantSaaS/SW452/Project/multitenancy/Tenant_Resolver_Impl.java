package Multi_TenantSaaS.SW452.Project.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * CurrentTenantIdentifierResolverImpl - Provides the current tenant identifier to Hibernate.
 */
@Component
public class Tenant_Resolver_Impl implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getTenantIdOrDefault();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
