package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Blog;
import iuh.fit.airsky.model.BlogLike;
import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogLikeRepository extends JpaRepository<BlogLike, Long> {
    Optional<BlogLike> findByBlogAndUser(Blog blog, User user);
    
    boolean existsByBlogAndUser(Blog blog, User user);
    
    long countByBlog(Blog blog);
    
    void deleteByBlogAndUser(Blog blog, User user);

    Page<BlogLike> findByUser(User user, Pageable pageable);
}
