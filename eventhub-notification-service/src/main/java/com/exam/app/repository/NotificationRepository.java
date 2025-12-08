package com.exam.app.repository;

import com.exam.app.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findAllByDeletedIsFalse();

    List<Notification> findByRecipientIdAndDeletedIsFalse(UUID recipientId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdOn < :cutoffDate")
    int deleteByCreatedOnBefore(LocalDateTime cutoffDate);

}

