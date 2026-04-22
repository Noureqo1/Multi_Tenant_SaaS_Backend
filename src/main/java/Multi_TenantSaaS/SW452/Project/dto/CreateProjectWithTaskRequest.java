package Multi_TenantSaaS.SW452.Project.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateProjectWithTaskRequest(
        @NotBlank(message = "Project name is required") String projectName,
        String projectDescription,
        @NotBlank(message = "Task title is required") String taskTitle,
        @NotBlank(message = "Task status is required") String taskStatus
) {
}