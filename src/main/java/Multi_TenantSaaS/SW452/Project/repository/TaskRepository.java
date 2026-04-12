package Multi_TenantSaaS.SW452.Project.repository;

import Multi_TenantSaaS.SW452.Project.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
