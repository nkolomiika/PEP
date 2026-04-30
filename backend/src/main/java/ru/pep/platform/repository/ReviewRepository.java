package ru.pep.platform.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
}
