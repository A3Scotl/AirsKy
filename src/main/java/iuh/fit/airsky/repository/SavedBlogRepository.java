package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Blog;
import iuh.fit.airsky.model.SavedBlog;
import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavedBlogRepository extends JpaRepository<SavedBlog, Long> {
    Optional<SavedBlog> findByUserAndBlog(User user, Blog blog);
    Page<SavedBlog> findByUser(User user, Pageable pageable);
    boolean existsByUserAndBlog(User user, Blog blog);
    void deleteByUserAndBlog(User user, Blog blog);
}

