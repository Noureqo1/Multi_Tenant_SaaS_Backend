package Multi_TenantSaaS.SW452.Project.dto;

import java.time.Instant;
import java.util.UUID;

public record JobStatusResponse(
        UUID jobId,
        Long projectId,
        String status,
        Instant createdAt,
        Instant completedAt
) {}
