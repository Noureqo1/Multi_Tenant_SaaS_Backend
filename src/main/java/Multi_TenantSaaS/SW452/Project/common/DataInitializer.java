package Multi_TenantSaaS.SW452.Project.common;

import Multi_TenantSaaS.SW452.Project.user.Role;
import Multi_TenantSaaS.SW452.Project.user.User;
import Multi_TenantSaaS.SW452.Project.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail("admin@tenant1.com").isEmpty()) {
            userRepository.save(User.builder()
                    .email("admin@tenant1.com")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.TENANT_ADMIN)
                    .tenantId(1L)
                    .build());
        }

        if (userRepository.findByEmail("user@tenant1.com").isEmpty()) {
            userRepository.save(User.builder()
                    .email("user@tenant1.com")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.TENANT_USER)
                    .tenantId(1L)
                    .build());
        }

        if (userRepository.findByEmail("admin@tenant2.com").isEmpty()) {
            userRepository.save(User.builder()
                    .email("admin@tenant2.com")
                    .password(passwordEncoder.encode("123456"))
                    .role(Role.TENANT_ADMIN)
                    .tenantId(2L)
                    .build());
        }
    }
}