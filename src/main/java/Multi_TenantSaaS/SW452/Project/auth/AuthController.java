package Multi_TenantSaaS.SW452.Project.auth;

import Multi_TenantSaaS.SW452.Project.auth.dto.AuthMeResponse;
import Multi_TenantSaaS.SW452.Project.auth.dto.LoginRequest;
import Multi_TenantSaaS.SW452.Project.auth.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthMeResponse me(Authentication authentication) {
        return authService.getCurrentUser(authentication.getName());
    }
}