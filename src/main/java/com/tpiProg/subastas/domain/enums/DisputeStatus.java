package com.tpiProg.subastas.domain.enums;

// Este archivo define en qué estado está un reclamo/disputa.
public enum DisputeStatus {
    PENDING,         // El reclamo recién se crea, esperando que un Admin lo vea
    RESOLVED_BUYER,  // El Admin le dio la razón al comprador (ganador)
    RESOLVED_SELLER, // El Admin le dio la razón al vendedor
    REJECTED         // El Admin rechazó el reclamo
}