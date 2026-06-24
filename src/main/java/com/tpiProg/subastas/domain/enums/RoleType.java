package com.tpiProg.subastas.domain.enums;

// Este archivo define los roles que puede tener una persona en el sistema.
public enum RoleType {
    USER,   // Usuario normal (puede comprar/pujar)
    SELLER, // Vendedor (puede publicar productos)
    ADMIN   // Administrador (controla todo)
}