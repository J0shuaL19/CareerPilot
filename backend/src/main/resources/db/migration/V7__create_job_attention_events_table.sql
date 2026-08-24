CREATE TABLE job_attention_events (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    previous_snoozed_until DATE,
    new_snoozed_until DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_attention_events_job
        FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT chk_job_attention_events_action CHECK (
        action IN (
            'SNOOZED',
            'RESCHEDULED',
            'RESUMED',
            'RESTORED',
            'CLEARED_BY_ACTIVITY'
        )
    ),
    CONSTRAINT chk_job_attention_events_dates CHECK (
        previous_snoozed_until IS NOT NULL OR new_snoozed_until IS NOT NULL
    )
);

CREATE INDEX idx_job_attention_events_created_at
    ON job_attention_events (created_at DESC, id DESC);