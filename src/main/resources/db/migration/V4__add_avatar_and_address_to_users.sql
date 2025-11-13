-- Flyway migration: add avatar and address fields to users

ALTER TABLE users
    ADD COLUMN avatar_url VARCHAR(255) NULL AFTER email,
    ADD COLUMN address VARCHAR(255) NULL AFTER avatar_url;
