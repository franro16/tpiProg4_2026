package com.tpiProg.subastas.repository;

import com.tpiProg.subastas.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Busca todas las notificaciones de un usuario ordenadas por fecha (las más nuevas primero)
    List<Notification> findByUserIdOrderByCreationDateDesc(Long userId);
    
    // Busca solo las notificaciones que el usuario todavía no leyó
    List<Notification> findByUserIdAndIsReadFalseOrderByCreationDateDesc(Long userId);
}