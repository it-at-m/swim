CREATE TABLE protocol
(
    id                      VARCHAR(36) NOT NULL,
    use_case                VARCHAR NOT NULL,
    protocol_name           VARCHAR NOT NULL,
    file_name               VARCHAR NOT NULL,
    page_count              INT NOT NULL,
    pagination_id           VARCHAR,
    document_type           VARCHAR,
    coo_address             VARCHAR,
    additional_properties   jsonb,
    created_at              TIMESTAMP NOT NULL,
    CONSTRAINT pk_protocol PRIMARY KEY (id),
    CONSTRAINT protocol_entries_unique UNIQUE (use_case, protocol_name, file_name)
);