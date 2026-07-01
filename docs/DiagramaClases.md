# Diagrama de Clases UML - Sistema de Subastas Online


```mermaid
classDiagram
    direction LR

    class User {
        +Long id
        +String username
        +String email
        +String passwordHash
        +boolean isBlocked
        +int failedAttempts
        +OffsetDateTime lockedUntil
        +Set~Role~ roles
    }

    class Role {
        +Long id
        +RoleType name
    }

    class Category {
        +Long id
        +String name
        +String description
    }

    class Product {
        +Long id
        +User seller
        +Category category
        +String name
        +String description
    }

    class Auction {
        +Long id
        +Product product
        +User winner
        +BigDecimal basePrice
        +BigDecimal minimumIncrement
        +BigDecimal currentPrice
        +OffsetDateTime startDate
        +OffsetDateTime endDate
        +OffsetDateTime adjudicationDate
        +AuctionStatus status
        +String description
        +Long version
        +boolean validarPeriodoSubasta(OffsetDateTime now)
        +void cambiarEstado(AuctionStatus nuevoEstado)
        +boolean aptaParaDisputa()
        +boolean evaluarCierreAutomatico(OffsetDateTime now)
    }

    class Bid {
        +Long id
        +Auction auction
        +User user
        +BigDecimal amount
        +OffsetDateTime bidDate
        +boolean validarMontoMinimo(BigDecimal precioBase)
        +boolean validarIncrementoMinimo(BigDecimal incrementoMinimo, BigDecimal precioActual)
    }

    class AuctionStateHistory {
        +Long id
        +Auction auction
        +User responsibleUser
        +AuctionStatus previousState
        +AuctionStatus newState
        +OffsetDateTime changeDate
        +String reason
    }

    class Notification {
        +Long id
        +User user
        +String message
        +OffsetDateTime creationDate
        +boolean isRead
    }

    class Dispute {
        +Long id
        +Auction auction
        +User initiator
        +User adminResolver
        +String reason
        +String description
        +OffsetDateTime creationDate
        +String adminResolution
    }

    class AuctionStatus {
        <<enumeration>>
        BORRADOR
        PUBLICADA
        ACTIVA
        FINALIZADA
        CANCELADA
        ADJUDICADA
        EN_DISPUTA
    }

    class RoleType {
        <<enumeration>>
        USER
        SELLER
        ADMIN
    }

    User "0..*" -- "0..*" Role : roles

    User "1" <-- "0..*" Product : seller
    Category "1" <-- "0..*" Product : category

    Product "1" <-- "0..*" Auction : product
    User "0..1" <-- "0..*" Auction : winner
    Auction --> AuctionStatus : status

    Auction "1" <-- "0..*" Bid : auction
    User "1" <-- "0..*" Bid : user

    Auction "1" <-- "0..*" AuctionStateHistory : auction
    User "1" <-- "0..*" AuctionStateHistory : responsibleUser
    AuctionStateHistory --> AuctionStatus : previousState
    AuctionStateHistory --> AuctionStatus : newState

    User "1" <-- "0..*" Notification : user

    Auction "1" <-- "0..*" Dispute : auction
    User "1" <-- "0..*" Dispute : initiator
    User "0..1" <-- "0..*" Dispute : adminResolver
```

## Lectura del diagrama

- `User` representa a los usuarios del sistema. Puede tener uno o más roles mediante la relación con `Role`.
- `Product` pertenece a un vendedor (`User`) y a una `Category`.
- `Auction` pertenece a un `Product`. El vendedor de la subasta se obtiene indirectamente desde el producto.
- `Auction` puede tener un `winner`, que es un `User`. Al crear la subasta este valor puede ser nulo.
- `Bid` representa una puja realizada por un usuario sobre una subasta.
- `AuctionStateHistory` registra los cambios de estado de la subasta, el usuario responsable, la fecha y el motivo.
- `Notification` representa los avisos internos para usuarios.
- `Dispute` representa un reclamo sobre una subasta adjudicada. Puede tener un administrador resolutor cuando ya fue resuelta.

## Observaciones

- Este diagrama está centrado en clases de dominio, no en controladores, servicios, DTOs ni repositorios.
- Los enums `DisputeStatus` y `NotificationType` existen en el código, pero no se incluyen como relaciones porque actualmente no están usados por `Dispute` ni por `Notification`.
- El estado principal de una disputa se refleja mediante el estado de la subasta (`EN_DISPUTA`, `ADJUDICADA`, `FINALIZADA`, `CANCELADA`) y la resolución administrativa guardada en `Dispute.adminResolution`.
