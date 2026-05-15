package Multi_TenantSaaS.SW452.Project;

import Multi_TenantSaaS.SW452.Project.config.SecurityConfig;
import Multi_TenantSaaS.SW452.Project.controller.ProjectController;
import Multi_TenantSaaS.SW452.Project.dto.CreateProjectRequest;
import Multi_TenantSaaS.SW452.Project.dto.ProjectResponse;
import Multi_TenantSaaS.SW452.Project.multitenancy.TenantContextFilter;
import Multi_TenantSaaS.SW452.Project.security.JwtAuthenticationFilter;
import Multi_TenantSaaS.SW452.Project.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
        controllers = ProjectController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthenticationFilter.class, TenantContextFilter.class}
        )
)
@Import(ProjectControllerSecurityTest.MethodSecurityConfig.class)
class ProjectControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }

    @Test
    @WithMockUser(roles = "TENANT_ADMIN")
    void shouldAllowCreateProjectForTenantAdmin() throws Exception {
        String body = """
                {
                  "name": "Admin Project",
                  "description": null
                }
                """;

        when(projectService.createProject(any(CreateProjectRequest.class)))
                .thenReturn(new ProjectResponse(1L, "Admin Project", null, List.of()));

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(projectService).createProject(any(CreateProjectRequest.class));
    }

    @Test
    @WithMockUser(roles = "TENANT_USER")
    void shouldReturn403ForTenantUser() throws Exception {
        String body = """
                {
                  "name": "Blocked",
                  "description": null
                }
                """;

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        verify(projectService, never()).createProject(any());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        String body = """
                {
                  "name": "Anonymous",
                  "description": null
                }
                """;

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        verify(projectService, never()).createProject(any());
    }
}
