-- V4: Creación de la tabla de Pujas (Bids)

CREATE TABLE bids (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT NOT NULL REFERENCES auctions(id),
    user_id BIGINT NOT NULL REFERENCES app_users(id),
    amount NUMERIC(19,4) NOT NULL,
    bid_date TIMESTAMPTZ NOT NULL
);