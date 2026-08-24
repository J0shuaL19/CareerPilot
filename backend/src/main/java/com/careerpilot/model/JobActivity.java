package com.careerpilot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobActivityType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 255)
    private String contact;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "completion_note", columnDefinition = "TEXT")
    private String completionNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_previous_job_status", length = 32)
    private JobStatus completionPreviousJobStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_applied_job_status", length = 32)
    private JobStatus completionAppliedJobStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public JobActivity(
            Job job,
            JobActivityType type,
            String title,
            String details,
            String contact,
            Instant occurredAt
    ) {
        this.job = Objects.requireNonNull(job, "Job is required");
        this.type = Objects.requireNonNull(type, "Activity type is required");
        this.title = Objects.requireNonNull(title, "Activity title is required");
        this.details = details;
        this.contact = contact;
        this.occurredAt = Objects.requireNonNull(occurredAt, "Activity time is required");
    }

    public void update(
            JobActivityType type,
            String title,
            String details,
            String contact,
            Instant occurredAt
    ) {
        this.type = Objects.requireNonNull(type, "Activity type is required");
        this.title = Objects.requireNonNull(title, "Activity title is required");
        this.details = details;
        this.contact = contact;
        this.occurredAt = Objects.requireNonNull(occurredAt, "Activity time is required");
    }

    public void complete(
            Instant completedAt,
            String completionNote,
            JobStatus previousJobStatus,
            JobStatus appliedJobStatus
    ) {
        if ((previousJobStatus == null) != (appliedJobStatus == null)) {
            throw new IllegalArgumentException("Completion status snapshots must be paired");
        }
        this.completedAt = Objects.requireNonNull(completedAt, "Completion time is required");
        this.completionNote = completionNote;
        this.completionPreviousJobStatus = previousJobStatus;
        this.completionAppliedJobStatus = appliedJobStatus;
    }

    public void reopen() {
        this.completedAt = null;
        this.completionNote = null;
        this.completionPreviousJobStatus = null;
        this.completionAppliedJobStatus = null;
    }

    public void reschedule(Instant occurredAt) {
        this.occurredAt = Objects.requireNonNull(occurredAt, "Activity time is required");
    }

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
