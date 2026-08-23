CREATE TABLE job_activities (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    details TEXT,
    contact VARCHAR(255),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_activities_job
        FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT chk_job_activities_type CHECK (
        type IN ('APPLICATION', 'INTERVIEW', 'FOLLOW_UP', 'NOTE')
    )
);

CREATE INDEX idx_job_activities_job_occurred_at
    ON job_activities (job_id, occurred_at DESC, created_at DESC);
