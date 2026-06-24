package com.tpiProg.subastas.domain.enums;

// Este archivo define los tipos de avisos que le llegan a los usuarios.
public enum NotificationType {
    SYSTEM,           // Mensaje general del sistema
    AUCTION_WON,      // ¡Ganaste la subasta!
    OUTBID,           // Alguien ofreció más plata que vos
    DISPUTE_OPENED,   // Se abrió un reclamo en tu subasta
    DISPUTE_RESOLVED  // Un administrador resolvió el reclamo
}