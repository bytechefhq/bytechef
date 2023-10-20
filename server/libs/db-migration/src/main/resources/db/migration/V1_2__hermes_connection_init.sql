CREATE TABLE IF NOT EXISTS connection (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    name                     VARCHAR(256) NOT NULL,
    key                      VARCHAR(256) NOT NULL,
    component_name           VARCHAR(256) NOT NULL,
    authorization_name       VARCHAR(256) NULL,
    parameters               TEXT         NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
);

ALTER TABLE connection ADD CONSTRAINT uk_connection_name UNIQUE (name);

CREATE TABLE IF NOT EXISTS connection_tag (
    id                       BIGSERIAL    NOT NULL PRIMARY KEY,
    connection_id            BIGSERIAL    NOT NULL,
    tag_id                   BIGSERIAL    NOT NULL
);

ALTER TABLE connection_tag ADD CONSTRAINT fk_connection_tag_connection FOREIGN KEY (connection_id) REFERENCES connection (id);
ALTER TABLE connection_tag ADD CONSTRAINT fk_connection_tag_tag FOREIGN KEY (tag_id) REFERENCES tag (id);
