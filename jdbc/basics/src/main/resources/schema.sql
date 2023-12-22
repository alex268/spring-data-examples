CREATE TABLE IF NOT EXISTS category (id INTEGER PRIMARY KEY, name VARCHAR(100), description VARCHAR(2000), age_group VARCHAR(20), created DATETIME, inserted BIGINT);

CREATE TABLE IF NOT EXISTS "lego_set" (id INTEGER, name VARCHAR(100), "min_age" INTEGER, "max_age" INTEGER);
CREATE TABLE IF NOT EXISTS "handbuch" ("handbuch_id" INTEGER, author VARCHAR(100), text CLOB);
CREATE TABLE IF NOT EXISTS "model" ("name" VARCHAR(100), description CLOB, "lego_set" INTEGER);
