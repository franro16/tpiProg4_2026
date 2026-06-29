package com.tpiProg.subastas.service.impl;

import com.tpiProg.subastas.domain.entity.Notification;
import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.dto.response.NotificationResponse;
import com.tpiProg.subastas.exception.ResourceNotFoundException;
import com.tpiProg.subastas.exception.UnauthorizedException;
import com.tpiProg.subastas.repository.NotificationRepository;
import com.tpiProg.subastas.repository.UserRepository;
import com.tpiProg.subastas.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void createNotification(User user, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .creationDate(OffsetDateTime.now(ZoneOffset.UTC))
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Notificacion creada para usuario {}: {}", user.getEmail(), message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String userEmail) {
        // authentication.getName() devuelve email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado", 0L));

        return notificationRepository.findByUserIdOrderByCreationDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, String userEmail) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificationId));

        // Comparar por email
        if (!notification.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("No tenés permisos para modificar esta notificacion.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getCreationDate(),
                notification.isRead()
        );
    }
}