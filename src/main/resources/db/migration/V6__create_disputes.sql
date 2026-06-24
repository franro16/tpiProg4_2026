-- V6: Creación de la tabla de Reclamos / Disputas

CREATE TABLE disputes (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT NOT NULL REFERENCES auctions(id),
    initiator_id BIGINT NOT NULL REFERENCES app_users(id),
    admin_resolver_id BIGINT REFERENCES app_users(id),
    reason VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    creation_date TIMESTAMPTZ NOT NULL,
    admin_resolution VARCHAR(2000)
);