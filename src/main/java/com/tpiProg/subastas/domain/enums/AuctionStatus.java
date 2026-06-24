package com.tpiProg.subastas.domain.enums;

// Este archivo define todos los estados por los que pasa una subasta.
public enum AuctionStatus {
    BORRADOR,   // El vendedor la está armando pero aún no es pública
    PUBLICADA,  // Está visible, pero la fecha de inicio aún no llegó
    ACTIVA,     // La gente ya puede realizar pujas
    FINALIZADA, // Terminó el tiempo y nadie hizo pujas
    CANCELADA,  // Se dio de baja antes de terminar
    ADJUDICADA, // Terminó el tiempo y hay un ganador
    EN_DISPUTA  // Hubo un problema y hay un reclamo abierto
}