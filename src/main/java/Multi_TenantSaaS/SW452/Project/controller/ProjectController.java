package Multi_TenantSaaS.SW452.Project.controller;

import Multi_TenantSaaS.SW452.Project.dto.CreateProjectRequest;
import Multi_TenantSaaS.SW452.Project.dto.CreateProjectWithTaskRequest;
import Multi_TenantSaaS.SW452.Project.dto.GenerateReportResponse;
import Multi_TenantSaaS.SW452.Project.dto.ProjectResponse;
import Multi_TenantSaaS.SW452.Project.dto.CreateTaskRequest;
import Multi_TenantSaaS.SW452.Project.dto.TaskResponse;
import Multi_TenantSaaS.SW452.Project.service.JobService;
import Multi_TenantSaaS.SW452.Project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    
    private final ProjectService projectService;
    private final JobService jobService;

    public ProjectController(ProjectService projectService, JobService jobService) {
        this.projectService = projectService;
        this.jobService = jobService;
    }

    @PreAuthorize("hasRole('TENANT_ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@Valid @RequestBody CreateProjectRequest req) {
        return projectService.createProject(req);
    }

    @PostMapping("/with-task")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProjectWithInitialTask(@Valid @RequestBody CreateProjectWithTaskRequest req) {
        return projectService.createProjectWithInitialTask(req);
    }

    @GetMapping
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PostMapping("/{id}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse addTaskToProject(@PathVariable Long id, @Valid @RequestBody CreateTaskRequest req) {
        return projectService.addTaskToProject(id, req);
    }

    @PostMapping("/{id}/generate-report")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerateReportResponse generateReport(@PathVariable Long id) {
        return jobService.createAndEnqueueReportJob(id);
    }
}

