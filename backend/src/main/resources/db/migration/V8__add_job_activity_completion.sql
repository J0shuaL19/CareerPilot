ALTER TABLE job_activities
    ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE job_activities
    ADD COLUMN completion_note TEXT;

ALTER TABLE job_activities
    ADD CONSTRAINT chk_job_activities_completion_note_length
        CHECK (completion_note IS NULL OR char_length(completion_note) <= 2000);