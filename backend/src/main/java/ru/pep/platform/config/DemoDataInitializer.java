package ru.pep.platform.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Course;
import ru.pep.platform.domain.CourseStatus;
import ru.pep.platform.domain.LearningModule;
import ru.pep.platform.domain.ModuleStatus;
import ru.pep.platform.domain.Role;
import ru.pep.platform.repository.AppUserRepository;
import ru.pep.platform.repository.CourseRepository;

@Configuration
public class DemoDataInitializer {

    @Bean
    CommandLineRunner seedDemoData(AppUserRepository users, CourseRepository courses, PasswordEncoder passwordEncoder) {
        return args -> {
            createUser(users, passwordEncoder, "admin@pep.local", "admin", "Администратор", Role.ADMIN);
            createUser(users, passwordEncoder, "curator@pep.local", "curator", "Куратор", Role.CURATOR);
            createUser(users, passwordEncoder, "student1@pep.local", "student", "Студент 1", Role.STUDENT);
            createUser(users, passwordEncoder, "student2@pep.local", "student", "Студент 2", Role.STUDENT);

            if (courses.count() == 0) {
                Course course = new Course(
                        "OWASP Top 10",
                        "Практический курс по базовым классам web-уязвимостей.",
                        CourseStatus.PUBLISHED);
                course.addModule(new LearningModule("A03. Injection", "SQL Injection", ModuleStatus.ACTIVE));
                courses.save(course);
            }
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
