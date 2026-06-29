ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ NULL;


INSERT INTO app_users (username, email, password_hash, is_blocked, failed_attempts, locked_until)
VALUES (
    'admin',
    'admin@subastas.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM5lmDdg.1FyZ4omOUWO',
    false,
    0,
    null
);


INSERT INTO user_roles (user_id, role_id)
VALUES (
    (SELECT id FROM app_users WHERE email = 'admin@subastas.com'),
    (SELECT id FROM roles WHERE name = 'ADMIN')
);