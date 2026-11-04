USE marine_garbage;
CREATE TABLE IF NOT EXISTS live_session (
 id CHAR(36) PRIMARY KEY, monitoring_id CHAR(36) NOT NULL,
 publisher_key VARCHAR(255) NOT NULL UNIQUE, stream_path VARCHAR(255) NOT NULL,
 state VARCHAR(24) NOT NULL DEFAULT 'PENDING', mode VARCHAR(16) NOT NULL DEFAULT 'MANUAL',
 started_at DATETIME(3), ended_at DATETIME(3), last_error TEXT, latest_sample JSON,
 created_at TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),
 CONSTRAINT fk_live_monitor FOREIGN KEY(monitoring_id) REFERENCES monitoring_record(id)
);
