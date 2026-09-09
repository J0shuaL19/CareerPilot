package com.careerpilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "job_url", length = 2048)
    private String jobUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "attention_snoozed_until")
    private LocalDate attentionSnoozedUntil;

    public Job(String company, String title, String description, String jobUrl) {
        this.company = company;
        this.title = title;
        this.description = description;
        this.jobUrl = jobUrl;
        this.status = JobStatus.SAVED;
    }
    public static Job restore(
            String company,
            String title,
            String description,
            String jobUrl,
            JobStatus status,
            Instant createdAt,
            LocalDate attentionSnoozedUntil
    ) {
        Job job = new Job(company, title, description, jobUrl);
        job.status = Objects.requireNonNull(status, "Job status is required");
        job.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        job.attentionSnoozedUntil = attentionSnoozedUntil;
        return job;
    }

    public void updateStatus(JobStatus status) {
        this.status = Objects.requireNonNull(status, "Job status is required");
    }

    public void updateDetails(String company, String title, String description, String jobUrl) {
        this.company = Objects.requireNonNull(company, "Company is required");
        this.title = Objects.requireNonNull(title, "Title is required");
        this.description = Objects.requireNonNull(description, "Description is required");
        this.jobUrl = jobUrl;
    }

    public void snoozeAttentionUntil(LocalDate date) {
        this.attentionSnoozedUntil = Objects.requireNonNull(date, "Snooze date is required");
    }

    public void clearAttentionSnooze() {
        this.attentionSnoozedUntil = null;
    }

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = JobStatus.SAVED;
        }
    }
}
