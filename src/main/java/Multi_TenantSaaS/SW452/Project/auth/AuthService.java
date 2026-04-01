package Multi_TenantSaaS.SW452.Project.auth;

import Multi_TenantSaaS.SW452.Project.auth.dto.AuthMeResponse;
import Multi_TenantSaaS.SW452.Project.auth.dto.LoginRequest;
import Multi_TenantSaaS.SW452.Project.auth.dto.LoginResponse;
import Multi_TenantSaaS.SW452.Project.security.JwtService;
import Multi_TenantSaaS.SW452.Project.user.User;
import Multi_TenantSaaS.SW452.Project.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);
        return new LoginResponse(token);
    }

    public AuthMeResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return AuthMeResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .tenantId(user.getTenantId())
                .build();
    }
}