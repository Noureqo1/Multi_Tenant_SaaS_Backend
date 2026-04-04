package Multi_TenantSaaS.SW452.Project.dto;

import java.util.List;

public record ProjectResponse(Long id, String name, String description, List<TaskResponse> tasks) {}
