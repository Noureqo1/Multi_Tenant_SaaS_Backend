package Multi_TenantSaaS.SW452.Project.auth.dto;

import Multi_TenantSaaS.SW452.Project.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthMeResponse {
    private Long id;
    private String email;
    private Role role;
    private Long tenantId;
}