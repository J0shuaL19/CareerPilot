CREATE TABLE job_attention_settings (
    id BIGINT PRIMARY KEY,
    applied_days INTEGER NOT NULL,
    online_assessment_days INTEGER NOT NULL,
    interview_days INTEGER NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_job_attention_settings_applied_days
        CHECK (applied_days BETWEEN 1 AND 90),
    CONSTRAINT chk_job_attention_settings_online_assessment_days
        CHECK (online_assessment_days BETWEEN 1 AND 90),
    CONSTRAINT chk_job_attention_settings_interview_days
        CHECK (interview_days BETWEEN 1 AND 90)
);

INSERT INTO job_attention_settings (
    id,
    applied_days,
    online_assessment_days,
    interview_days
) VALUES (1, 7, 7, 7);
