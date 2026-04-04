package Multi_TenantSaaS.SW452.Project.repository;

import Multi_TenantSaaS.SW452.Project.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
