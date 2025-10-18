package iuh.fit.airsky.repository;

import iuh.fit.airsky.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.deleted = false")
    Page<Notification> findAll(Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.notificationId = :id AND n.deleted = false")
    Optional<Notification> findById(Long id);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.deleted = false ORDER BY n.createdAt DESC")
    Page<Notification> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.deleted = false")
    List<Notification> findUnreadByUserId(Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false AND n.deleted = false")
    Long countUnreadByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.deleted = true, n.deletedAt = :now, n.active = false WHERE n.notificationId = :id")
    void softDeleteById(Long id, LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.notificationId IN :ids")
    void markAsReadByUserIdAndIds(Long userId, List<Long> ids);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllAsReadByUserId(Long userId);
}