CREATE TABLE request_deduplication (
                                       request_id VARCHAR(255) PRIMARY KEY,
                                       response_status INTEGER NOT NULL,
                                       response_body TEXT,
                                       created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);