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
import java.time.LocalDate;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "job_attention_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobAttentionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobAttentionEventAction action;

    @Column(name = "previous_snoozed_until")
    private LocalDate previousSnoozedUntil;

    @Column(name = "new_snoozed_until")
    private LocalDate newSnoozedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public JobAttentionEvent(
            Job job,
            JobAttentionEventAction action,
            LocalDate previousSnoozedUntil,
            LocalDate newSnoozedUntil
    ) {
        if (previousSnoozedUntil == null && newSnoozedUntil == null) {
            throw new IllegalArgumentException("An attention event requires a previous or new date");
        }
        this.job = Objects.requireNonNull(job, "Job is required");
        this.action = Objects.requireNonNull(action, "Attention event action is required");
        this.previousSnoozedUntil = previousSnoozedUntil;
        this.newSnoozedUntil = newSnoozedUntil;
    }
    public static JobAttentionEvent restore(
            Job job,
            JobAttentionEventAction action,
            LocalDate previousSnoozedUntil,
            LocalDate newSnoozedUntil,
            Instant createdAt
    ) {
        JobAttentionEvent event = new JobAttentionEvent(
                job,
                action,
                previousSnoozedUntil,
                newSnoozedUntil
        );
        event.createdAt = Objects.requireNonNull(createdAt, "Creation time is required");
        return event;
    }

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}