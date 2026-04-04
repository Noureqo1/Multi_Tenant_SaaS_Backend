package Multi_TenantSaaS.SW452.Project.repository;

import Multi_TenantSaaS.SW452.Project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
