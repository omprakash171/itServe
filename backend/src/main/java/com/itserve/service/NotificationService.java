package com.itserve.service;

import com.itserve.dto.NotificationResponse;
import com.itserve.dto.PageResponse;
import com.itserve.entity.Notification;
import com.itserve.entity.NotificationType;
import com.itserve.entity.Ticket;
import com.itserve.entity.User;
import com.itserve.exception.ResourceNotFoundException;
import com.itserve.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notify(User recipient, Ticket ticket, NotificationType type, String message) {
        if (recipient == null) {
            return;
        }
        notificationRepository.save(new Notification(recipient, ticket, type, message));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(Long userId, int page, int size) {
        return PageResponse.from(notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, Math.min(size, 50)))
                .map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new com.itserve.exception.ForbiddenActionException("This notification belongs to another user");
        }
        notification.setRead(true);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadFor(userId);
    }
}
