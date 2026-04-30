package ru.pep.platform.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.Course;

public interface CourseRepository extends JpaRepository<Course, UUID> {
}
