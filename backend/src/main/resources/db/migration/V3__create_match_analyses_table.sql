CREATE TABLE match_analyses (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    resume_id BIGINT NOT NULL,
    match_score INTEGER NOT NULL,
    summary TEXT NOT NULL,
    strengths TEXT NOT NULL,
    gaps TEXT NOT NULL,
    recommendations TEXT NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_analyses_job
        FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_match_analyses_resume
        FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE,
    CONSTRAINT chk_match_analyses_score CHECK (match_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_match_analyses_created_at
    ON match_analyses (created_at DESC);

CREATE INDEX idx_match_analyses_job_resume_created_at
    ON match_analyses (job_id, resume_id, created_at DESC);
