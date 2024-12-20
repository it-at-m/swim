CREATE TABLE lhm_user_inbox
(
    id          VARCHAR(36) NOT NULL,
    coo         VARCHAR NOT NULL,
    inbox_name  VARCHAR NOT NULL,
    username    VARCHAR NOT NULL,
    firstname   VARCHAR,
    surname     VARCHAR,
    ou          VARCHAR,
    street      VARCHAR,
    postal_code VARCHAR,
    city        VARCHAR,
    dms_tenant  VARCHAR NOT NULL,
    CONSTRAINT pk_lhm_user_inbox PRIMARY KEY (id)
);