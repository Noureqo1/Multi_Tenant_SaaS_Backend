package Multi_TenantSaaS.SW452.Project.repository;

import Multi_TenantSaaS.SW452.Project.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
