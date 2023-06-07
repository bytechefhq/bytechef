CREATE TABLE IF NOT EXISTS workflow (
    id					     VARCHAR(256) NOT NULL PRIMARY KEY,
    definition    			 TEXT         NOT NULL,
    format     			     INT          NOT NULL,
    created_date             TIMESTAMP    NOT NULL,
    created_by               VARCHAR(256) NOT NULL,
    last_modified_date       TIMESTAMP    NOT NULL,
    last_modified_by         VARCHAR(256) NOT NULL,
    version                  BIGINT       NOT NULL
    );
