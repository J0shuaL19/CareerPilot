package com.careerpilot.repository;

import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobActivityRepository extends JpaRepository<JobActivity, Long> {

    List<JobActivity> findAllByJob_IdOrderByOccurredAtDescCreatedAtDesc(Long jobId);

    List<JobActivity> findAllByJob_IdInAndTypeIn(
            Collection<Long> jobIds,
            Collection<JobActivityType> types
    );

    Optional<JobActivity> findByIdAndJob_Id(Long id, Long jobId);

    boolean existsByJob_IdAndTypeAndTitleIgnoreCaseAndOccurredAt(
            Long jobId,
            JobActivityType type,
            String title,
            Instant occurredAt
    );

    @EntityGraph(attributePaths = "job")
    List<JobActivity> findAllByIdIn(Collection<Long> ids);

    @Query("""
            select activity.job.id as jobId,
                   max(coalesce(activity.completedAt, activity.occurredAt)) as lastOccurredAt
            from JobActivity activity
            where activity.job.id in :jobIds
            group by activity.job.id
            """)
    List<JobActivityLastTouchProjection> findLatestOccurredAtByJobIds(
            @Param("jobIds") Collection<Long> jobIds
    );

    @EntityGraph(attributePaths = "job")
    List<JobActivity> findAllByTypeInAndCompletedAtIsNullAndOccurredAtBetweenOrderByOccurredAtAscCreatedAtAsc(
            Collection<JobActivityType> types,
            Instant start,
            Instant end
    );

    @EntityGraph(attributePaths = "job")
    List<JobActivity> findAllByTypeInAndCompletedAtIsNullAndOccurredAtBeforeOrderByOccurredAtAscCreatedAtAsc(
            Collection<JobActivityType> types,
            Instant cutoff
    );

    @EntityGraph(attributePaths = "job")
    @Query("""
            select activity
            from JobActivity activity
            where activity.type in :types
              and activity.occurredAt >= :start
              and activity.occurredAt < :end
            order by activity.occurredAt asc, activity.createdAt asc
            """)
    List<JobActivity> findScheduledActivitiesBetween(
            @Param("types") Collection<JobActivityType> types,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
