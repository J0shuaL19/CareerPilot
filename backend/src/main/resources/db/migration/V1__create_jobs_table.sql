CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    company VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    job_url VARCHAR(2048),
    status VARCHAR(32) NOT NULL DEFAULT 'SAVED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_jobs_status CHECK (
        status IN (
            'SAVED',
            'APPLIED',
            'OA',
            'INTERVIEW',
            'OFFER',
            'REJECTED',
            'WITHDRAWN'
        )
    )
);

CREATE INDEX idx_jobs_created_at ON jobs (created_at DESC);
