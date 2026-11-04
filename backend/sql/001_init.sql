CREATE DATABASE IF NOT EXISTS marine_garbage CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE marine_garbage;
CREATE TABLE IF NOT EXISTS monitoring_record (
 id CHAR(36) PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 media_type VARCHAR(20) NOT NULL,
 media_path TEXT,
 captured_at DATETIME(3),
 observation_area JSON,
 metadata JSON NOT NULL,
 created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);
CREATE TABLE IF NOT EXISTS detection_record (
 id CHAR(36) PRIMARY KEY,
 monitoring_id CHAR(36) NOT NULL,
 model VARCHAR(30) NOT NULL,
 frame_seconds DOUBLE NOT NULL DEFAULT 0,
 image_path TEXT NOT NULL,
 result JSON NOT NULL,
 created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 CONSTRAINT fk_detection_monitor FOREIGN KEY (monitoring_id) REFERENCES monitoring_record(id),
 INDEX idx_detection_monitor (monitoring_id,created_at)
);
CREATE TABLE IF NOT EXISTS trace_record (
 id CHAR(36) PRIMARY KEY,
 monitoring_id CHAR(36) NOT NULL,
 garbage_type VARCHAR(50) NOT NULL,
 status VARCHAR(30) NOT NULL,
 parameters JSON NOT NULL,
 result JSON,
 created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 CONSTRAINT fk_trace_monitor FOREIGN KEY (monitoring_id) REFERENCES monitoring_record(id)
);
CREATE TABLE IF NOT EXISTS pollution_source (
 id CHAR(36) PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 source_type VARCHAR(50) NOT NULL,
 longitude DOUBLE NOT NULL,
 latitude DOUBLE NOT NULL,
 evidence TEXT,
 metadata JSON NOT NULL
);
