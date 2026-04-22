package Multi_TenantSaaS.SW452.Project.service;

import Multi_TenantSaaS.SW452.Project.domain.Project;
import Multi_TenantSaaS.SW452.Project.dto.CreateProjectWithTaskRequest;
import Multi_TenantSaaS.SW452.Project.repository.ProjectRepository;
import Multi_TenantSaaS.SW452.Project.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Import(ProjectService.class)
class ProjectServiceRollbackTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectService projectService;

    @MockitoBean
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanProjects() {
        projectRepository.deleteAll();
    }

    @Test
    void createProjectWithInitialTask_rollsBackProjectWhenTaskSaveFails() {
        when(taskRepository.save(any())).thenThrow(new RuntimeException("boom"));

        CreateProjectWithTaskRequest request = new CreateProjectWithTaskRequest(
                "Rollback Project",
                "should not persist",
                "Rollback Task",
                "PENDING"
        );

        assertThatThrownBy(() -> projectService.createProjectWithInitialTask(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("boom");

        assertThat(projectRepository.count()).isZero();
    }
}