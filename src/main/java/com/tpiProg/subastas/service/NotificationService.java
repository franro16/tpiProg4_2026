package com.tpiProg.subastas.service;

import com.tpiProg.subastas.domain.entity.User;
import com.tpiProg.subastas.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    // Este método lo usarán internamente otros servicios, no se expone al Controller
    void createNotification(User user, String message);
    
    // Estos son para el usuario final
    List<NotificationResponse> getMyNotifications(String username);
    void markAsRead(Long notificationId, String username);
}