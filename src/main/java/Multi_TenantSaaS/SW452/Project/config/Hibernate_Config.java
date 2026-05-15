package Multi_TenantSaaS.SW452.Project.config;

import Multi_TenantSaaS.SW452.Project.multitenancy.Tenant_Connection_Provider;
import Multi_TenantSaaS.SW452.Project.multitenancy.Tenant_Resolver_Impl;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class Hibernate_Config {

    private final DataSource dataSource;
    private final Tenant_Resolver_Impl tenantIdentifierResolver;
    private final Tenant_Connection_Provider multiTenantConnectionProvider;

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("Multi_TenantSaaS.SW452.Project");
        em.setJpaVendorAdapter(jpaVendorAdapter());
        em.setJpaPropertyMap(hibernateProperties());
        return em;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(true);
        adapter.setGenerateDdl(true);
        return adapter;
    }

    private Map<String, Object> hibernateProperties() {
        Map<String, Object> properties = new HashMap<>();

        // Hibernate multi-tenancy configuration
        properties.put("hibernate.multiTenancy", "SCHEMA");
        properties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
        properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);

        // Additional Hibernate properties
        properties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        properties.put(AvailableSettings.HBM2DDL_AUTO, "update");
        properties.put(AvailableSettings.SHOW_SQL, true);
        properties.put(AvailableSettings.FORMAT_SQL, true);
        properties.put(AvailableSettings.DEFAULT_SCHEMA, "public");

        return properties;
    }
}