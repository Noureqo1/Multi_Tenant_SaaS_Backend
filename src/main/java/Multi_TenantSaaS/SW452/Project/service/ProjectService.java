package Multi_TenantSaaS.SW452.Project.service;

import Multi_TenantSaaS.SW452.Project.domain.Project;
import Multi_TenantSaaS.SW452.Project.domain.Task;
import Multi_TenantSaaS.SW452.Project.dto.*;
import Multi_TenantSaaS.SW452.Project.repository.ProjectRepository;
import Multi_TenantSaaS.SW452.Project.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectService(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest req) {
        log.info("Creating project with name={}", req.name());
        Project project = new Project();
        project.setName(req.name());
        project.setDescription(req.description());
        
        Project saved = projectRepository.save(project);
        return mapToProjectResponse(saved);
    }

    @Transactional
    public ProjectResponse createProjectWithInitialTask(CreateProjectWithTaskRequest req) {
        log.info("Creating project with initial task: projectName={}, taskTitle={}", req.projectName(), req.taskTitle());

        Project project = new Project();
        project.setName(req.projectName());
        project.setDescription(req.projectDescription());

        Project savedProject = projectRepository.save(project);

        Task task = new Task();
        task.setTitle(req.taskTitle());
        task.setStatus(req.taskStatus());
        savedProject.addTask(task);

        taskRepository.save(task);

        return mapToProjectResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        log.info("Fetching all projects");
        return projectRepository.findAll()
                .stream()
                .map(this::mapToProjectResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        log.info("Fetching project with id={}", id);
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        return mapToProjectResponse(project);
    }

    @Transactional
    public TaskResponse addTaskToProject(Long projectId, CreateTaskRequest req) {
        log.info("Adding task title={} to projectId={}", req.title(), projectId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        Task task = new Task();
        task.setTitle(req.title());
        task.setStatus(req.status());
        
        project.addTask(task);
        Task savedTask = taskRepository.save(task);
        
        return mapToTaskResponse(savedTask);
    }

    private ProjectResponse mapToProjectResponse(Project project) {
        List<TaskResponse> taskResponses = project.getTasks().stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
        return new ProjectResponse(project.getId(), project.getName(), project.getDescription(), taskResponses);
    }

    private TaskResponse mapToTaskResponse(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getStatus());
    }
}
