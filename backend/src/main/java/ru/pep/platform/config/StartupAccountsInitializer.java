package ru.pep.platform.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Role;
import ru.pep.platform.repository.AppUserRepository;

@Configuration
public class StartupAccountsInitializer {

    @Bean
    CommandLineRunner seedStartupAccounts(AppUserRepository users, PasswordEncoder passwordEncoder) {
        return args -> {
            createUser(users, passwordEncoder, "admin@local.host", "admin", "Администратор", Role.ADMIN);
            createUser(users, passwordEncoder, "teacher@local.host", "teacher", "Куратор", Role.CURATOR);
            createUser(users, passwordEncoder, "student@local.host", "student", "Студент", Role.STUDENT);
        };
    }

    private void createUser(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            String email,
            String rawPassword,
            String displayName,
            Role role) {
        if (!users.existsByEmail(email)) {
            users.save(new AppUser(email, passwordEncoder.encode(rawPassword), displayName, role));
        }
    }
}
