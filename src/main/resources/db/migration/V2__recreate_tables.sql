CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE ARKIVERT_VEDTAK
(
    ID                        VARCHAR(36) DEFAULT uuid_generate_v4() PRIMARY KEY,
    VEDTAK_ID                 VARCHAR(36)              NOT NULL UNIQUE,
    FNR                       VARCHAR(11)              NOT NULL,
    JOURNALPOST_ID            VARCHAR(20)              NOT NULL,
    OPPRETTET                 TIMESTAMP WITH TIME ZONE NOT NULL,
    SPINNSYN_FRONTEND_IMAGE   VARCHAR(128)             NOT NULL,
    SPINNSYN_ARKIVERING_IMAGE VARCHAR(128)             NOT NULL
);
