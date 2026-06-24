-- V3: Creación de Subastas y su Historial de Estados
-- Usamos NUMERIC(19,4) para los montos exactos de dinero y TIMESTAMPTZ para las fechas UTC

CREATE TABLE auctions (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id),
    winner_id BIGINT REFERENCES app_users(id),
    base_price NUMERIC(19,4) NOT NULL,
    minimum_increment NUMERIC(19,4) NOT NULL,
    current_price NUMERIC(19,4) NOT NULL,
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    adjudication_date TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL,
    description VARCHAR(2000),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE auction_state_histories (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT NOT NULL REFERENCES auctions(id),
    responsible_user_id BIGINT NOT NULL REFERENCES app_users(id),
    previous_state VARCHAR(30) NOT NULL,
    new_state VARCHAR(30) NOT NULL,
    change_date TIMESTAMPTZ NOT NULL,
    reason VARCHAR(500) NOT NULL
);