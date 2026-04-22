package Multi_TenantSaaS.SW452.Project.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(
	@NotBlank(message = "Task title is required") String title,
	@NotBlank(message = "Task status is required") String status
) {}
