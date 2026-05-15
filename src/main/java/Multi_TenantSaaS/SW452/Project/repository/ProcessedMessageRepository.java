package Multi_TenantSaaS.SW452.Project.repository;

import Multi_TenantSaaS.SW452.Project.domain.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, UUID> {
}
