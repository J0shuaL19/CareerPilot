ALTER TABLE job_activities
    ADD COLUMN completion_previous_job_status VARCHAR(32);

ALTER TABLE job_activities
    ADD COLUMN completion_applied_job_status VARCHAR(32);

ALTER TABLE job_activities
    ADD CONSTRAINT chk_job_activities_completion_previous_status
        CHECK (
            completion_previous_job_status IS NULL
            OR completion_previous_job_status IN (
                'SAVED', 'APPLIED', 'OA', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'
            )
        );

ALTER TABLE job_activities
    ADD CONSTRAINT chk_job_activities_completion_applied_status
        CHECK (
            completion_applied_job_status IS NULL
            OR completion_applied_job_status IN (
                'SAVED', 'APPLIED', 'OA', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'
            )
        );

ALTER TABLE job_activities
    ADD CONSTRAINT chk_job_activities_completion_status_pair
        CHECK (
            (completion_previous_job_status IS NULL AND completion_applied_job_status IS NULL)
            OR
            (completion_previous_job_status IS NOT NULL AND completion_applied_job_status IS NOT NULL)
        );