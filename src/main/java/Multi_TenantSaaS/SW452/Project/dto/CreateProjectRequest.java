package Multi_TenantSaaS.SW452.Project.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectRequest(
	@NotBlank(message = "Project name is required") String name,
	String description
) {}
