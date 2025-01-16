CREATE TABLE lhm_group_inbox
(
    coo         VARCHAR NOT NULL,
    inbox_name  VARCHAR NOT NULL,
    username    VARCHAR NOT NULL,
    ou          VARCHAR NOT NULL,
    dms_tenant  VARCHAR NOT NULL,
    CONSTRAINT pk_lhm_group_inbox PRIMARY KEY (coo)
);