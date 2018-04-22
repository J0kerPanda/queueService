CREATE TABLE "Category" (
  id SERIAL NOT NULL PRIMARY KEY,
  parentId INTEGER NULL REFERENCES "Category" (id) ON UPDATE RESTRICT ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  isFinal BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE FUNCTION is_final_category(int) RETURNS BOOLEAN as $$
SELECT EXISTS(
  SELECT id FROM "Category" WHERE id = $1 AND isFinal = TRUE
);
$$ LANGUAGE sql;

CREATE TABLE "User" (
  id bigint NOT NULL PRIMARY KEY,
  firstName VARCHAR(255) NOT NULL,
  surname VARCHAR(255) NOT NULL,
  lastName VARCHAR(255) NOT NULL,
  email varchar(255) NOT NULL UNIQUE,
  password varchar(255) NOT NULL,
  googleId bigint NOT NULL,
  isSuperuser boolean NOT NULL DEFAULT false,
  isBlocked boolean NOT NULL DEFAULT false,
  categoryId INTEGER NOT NULL REFERENCES "Category" (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  CONSTRAINT FinalCategory_check CHECK (is_final_category(categoryId))
);

CREATE TYPE visit_status AS ENUM ('pending', 'finished', 'cancelledByUser', 'cancelledByHost');

CREATE TABLE "TimeTable" (
  id BIGSERIAL NOT NULL PRIMARY KEY,
  hostId BIGINT NOT NULL REFERENCES "User" (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  visitorId BIGINT NOT NULL REFERENCES "User" (id) ON UPDATE RESTRICT ON DELETE RESTRICT,
  date TIMESTAMP NOT NULL,
  status visit_status NOT NULL DEFAULT 'pending',
  CONSTRAINT TimeTable_unique UNIQUE (hostId, date)
)
