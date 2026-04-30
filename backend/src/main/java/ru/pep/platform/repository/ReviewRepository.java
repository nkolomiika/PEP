package ru.pep.platform.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.pep.platform.domain.AppUser;
import ru.pep.platform.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByReportAuthor(AppUser author);
}
