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
        log.info("Notificación creada para el usuario {}: {}", user.getUsername(), message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", 0L));

        // Usamos el método que Victoria dejó en el repositorio para traerlas ordenadas
        return notificationRepository.findByUserIdOrderByCreationDateDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación", notificationId));

        if (!notification.getUser().getUsername().equals(username)) {
            throw new UnauthorizedException("No tienes permisos para modificar esta notificación.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // Como Victoria no creó el Mapper, lo resolvemos internamente acá para no sumar archivos extra
    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getCreationDate(),
                notification.isRead()
        );
    }
}