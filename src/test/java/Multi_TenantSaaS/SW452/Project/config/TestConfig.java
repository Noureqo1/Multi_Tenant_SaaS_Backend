package Multi_TenantSaaS.SW452.Project.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

/**
 * Test configuration that disables multi-tenancy for basic unit tests.
 * This allows tests to run with H2 in-memory database without tenant setup.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean testEntityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("Multi_TenantSaaS.SW452.Project");
        
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        ((HibernateJpaVendorAdapter) vendorAdapter).setShowSql(false);
        ((HibernateJpaVendorAdapter) vendorAdapter).setGenerateDdl(true);
        
        em.setJpaVendorAdapter(vendorAdapter);
        
        return em;
    }
}