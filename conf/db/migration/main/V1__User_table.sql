CREATE TABLE "User" (
  id bigint NOT NULL PRIMARY KEY,
  firstName VARCHAR(255) NOT NULL,
  surname VARCHAR(255) NOT NULL,
  lastName VARCHAR(255) NOT NULL,
  email varchar(255) NOT NULL UNIQUE,
  password varchar(255) NOT NULL,
  googleId bigint NOT NULL,
  isSuperuser boolean NOT NULL DEFAULT false,
  isBlocked boolean NOT NULL DEFAULT false
);

