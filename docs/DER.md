# DER - Sistema de Subastas Online

```mermaid
erDiagram

    APP_USERS {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR email UK
        VARCHAR password_hash
        BOOLEAN is_blocked
        INT failed_attempts
        TIMESTAMPTZ locked_until
    }

    ROLES {
        BIGINT id PK
        VARCHAR name UK
    }

    USER_ROLES {
        BIGINT user_id PK, FK
        BIGINT role_id PK, FK
    }

    CATEGORIES {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR description
    }

    PRODUCTS {
        BIGINT id PK
        BIGINT seller_id FK
        BIGINT category_id FK
        VARCHAR name
        VARCHAR description
    }

    AUCTIONS {
        BIGINT id PK
        BIGINT product_id FK
        BIGINT winner_id FK
        NUMERIC base_price
        NUMERIC minimum_increment
        NUMERIC current_price
        TIMESTAMPTZ start_date
        TIMESTAMPTZ end_date
        TIMESTAMPTZ adjudication_date
        VARCHAR status
        VARCHAR description
        BIGINT version
    }

    BIDS {
        BIGINT id PK
        BIGINT auction_id FK
        BIGINT user_id FK
        NUMERIC amount
        TIMESTAMPTZ bid_date
    }

    AUCTION_STATE_HISTORIES {
        BIGINT id PK
        BIGINT auction_id FK
        BIGINT responsible_user_id FK
        VARCHAR previous_state
        VARCHAR new_state
        TIMESTAMPTZ change_date
        VARCHAR reason
    }

    NOTIFICATIONS {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR message
        TIMESTAMPTZ creation_date
        BOOLEAN is_read
    }

    DISPUTES {
        BIGINT id PK
        BIGINT auction_id FK
        BIGINT initiator_id FK
        BIGINT admin_resolver_id FK
        VARCHAR reason
        VARCHAR description
        TIMESTAMPTZ creation_date
        VARCHAR admin_resolution
    }

    APP_USERS ||--o{ USER_ROLES : tiene
    ROLES ||--o{ USER_ROLES : asigna

    APP_USERS ||--o{ PRODUCTS : vende
    CATEGORIES ||--o{ PRODUCTS : clasifica

    PRODUCTS ||--o{ AUCTIONS : se_subasta_en
    APP_USERS |o--o{ AUCTIONS : gana

    AUCTIONS ||--o{ BIDS : recibe
    APP_USERS ||--o{ BIDS : realiza

    AUCTIONS ||--o{ AUCTION_STATE_HISTORIES : registra
    APP_USERS ||--o{ AUCTION_STATE_HISTORIES : responsable

    APP_USERS ||--o{ NOTIFICATIONS : recibe

    AUCTIONS ||--o{ DISPUTES : tiene
    APP_USERS ||--o{ DISPUTES : inicia
    APP_USERS |o--o{ DISPUTES : resuelve
```

## Relaciones principales

| Relación | Cardinalidad | Explicación |
|---|---:|---|
| `APP_USERS` - `ROLES` | N:M | Un usuario puede tener varios roles y un rol puede pertenecer a varios usuarios. Se resuelve con `USER_ROLES`. |
| `APP_USERS` - `PRODUCTS` | 1:N | Un vendedor puede cargar varios productos. Cada producto pertenece a un vendedor. |
| `CATEGORIES` - `PRODUCTS` | 1:N | Una categoría puede tener varios productos. Cada producto tiene una categoría. |
| `PRODUCTS` - `AUCTIONS` | 1:N | Una subasta se crea sobre un producto. A nivel de BD un producto podría aparecer en varias subastas porque no hay restricción `UNIQUE` sobre `product_id`. |
| `APP_USERS` - `AUCTIONS` | 0..1:N | Una subasta puede tener un ganador o no. Un usuario puede ganar muchas subastas. |
| `AUCTIONS` - `BIDS` | 1:N | Una subasta puede recibir varias pujas. Cada puja pertenece a una subasta. |
| `APP_USERS` - `BIDS` | 1:N | Un usuario puede realizar varias pujas. Cada puja pertenece a un usuario. |
| `AUCTIONS` - `AUCTION_STATE_HISTORIES` | 1:N | Cada cambio de estado de una subasta queda registrado en el historial. |
| `APP_USERS` - `AUCTION_STATE_HISTORIES` | 1:N | Cada registro de historial tiene un usuario responsable. |
| `APP_USERS` - `NOTIFICATIONS` | 1:N | Un usuario puede recibir muchas notificaciones. |
| `AUCTIONS` - `DISPUTES` | 1:N | Una subasta puede tener reclamos o disputas. |
| `APP_USERS` - `DISPUTES` | 1:N | Un usuario puede iniciar varias disputas. |
| `APP_USERS` - `DISPUTES` | 0..1:N | Una disputa puede tener un administrador resolutor o quedar pendiente. |

## Observaciones

- La subasta no guarda un vendedor directo; el vendedor se obtiene desde `Auction -> Product -> Seller`.
- La notificación no guarda título, tipo ni subasta asociada; guarda usuario, mensaje, fecha y si fue leída.
- La disputa no tiene estado propio en base de datos; el estado que cambia es el de la subasta.
- Existen enums como `DisputeStatus` y `NotificationType`, pero el modelo actual no los usa en las entidades persistidas.
