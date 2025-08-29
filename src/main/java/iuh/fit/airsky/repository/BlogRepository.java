package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Blog;
import iuh.fit.airsky.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {
    
    // Simple queries without JOIN FETCH for pagination
    Page<Blog> findAll(Pageable pageable);
    
    Page<Blog> findByIsPublishedTrue(Pageable pageable);
    
    Page<Blog> findByAuthor(User author, Pageable pageable);
    
    Page<Blog> findByAuthorAndIsPublishedTrue(User author, Pageable pageable);
    
    @Query("SELECT b FROM Blog b JOIN b.categories c WHERE c.slug = :categorySlug AND b.isPublished = true")
    Page<Blog> findByCategorySlugAndIsPublishedTrue(@Param("categorySlug") String categorySlug, Pageable pageable);
    
    @Query("SELECT b FROM Blog b WHERE b.isPublished = true AND (LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR b.content LIKE CONCAT('%', :keyword, '%'))")
    Page<Blog> findByKeywordAndIsPublishedTrue(@Param("keyword") String keyword, Pageable pageable);
    
    // Single entity queries with JOIN FETCH
    @Query("SELECT DISTINCT b FROM Blog b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.categories LEFT JOIN FETCH b.categories WHERE b.slug = :slug")
    Optional<Blog> findBySlug(@Param("slug") String slug);
    
    @Query("SELECT DISTINCT b FROM Blog b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.categories LEFT JOIN FETCH b.categories WHERE b.slug = :slug AND b.isPublished = true")
    Optional<Blog> findBySlugAndIsPublishedTrue(@Param("slug") String slug);
    
    @Query("SELECT DISTINCT b FROM Blog b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.categories LEFT JOIN FETCH b.categories WHERE b.blogId = :id")
    Optional<Blog> findById(@Param("id") Long id);
    
    // Helper method to fetch entities with relationships by IDs
    @Query("SELECT DISTINCT b FROM Blog b LEFT JOIN FETCH b.author LEFT JOIN FETCH b.categories LEFT JOIN FETCH b.categories WHERE b.blogId IN :ids")
    List<Blog> findByIdInWithRelations(@Param("ids") List<Long> ids);
    
    boolean existsBySlug(String slug);
}
