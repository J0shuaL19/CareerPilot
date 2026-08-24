CREATE TABLE interview_preparations (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL UNIQUE,
    company_research TEXT,
    company_research_done BOOLEAN NOT NULL DEFAULT FALSE,
    role_priorities TEXT,
    role_priorities_done BOOLEAN NOT NULL DEFAULT FALSE,
    star_stories TEXT,
    star_stories_done BOOLEAN NOT NULL DEFAULT FALSE,
    questions_to_ask TEXT,
    questions_to_ask_done BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_interview_preparations_activity
        FOREIGN KEY (activity_id) REFERENCES job_activities(id) ON DELETE CASCADE
);
