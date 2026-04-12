package Multi_TenantSaaS.SW452.Project.controller;

import Multi_TenantSaaS.SW452.Project.dto.CreateProjectRequest;
import Multi_TenantSaaS.SW452.Project.dto.ProjectResponse;
import Multi_TenantSaaS.SW452.Project.dto.CreateTaskRequest;
import Multi_TenantSaaS.SW452.Project.dto.TaskResponse;
import Multi_TenantSaaS.SW452.Project.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {
    
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse createProject(@RequestBody CreateProjectRequest req) {
        return projectService.createProject(req);
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
    public TaskResponse addTaskToProject(@PathVariable Long id, @RequestBody CreateTaskRequest req) {
        return projectService.addTaskToProject(id, req);
    }
}
