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

    @Query("""
            select activity.job.id as jobId, max(activity.occurredAt) as lastOccurredAt
            from JobActivity activity
            where activity.job.id in :jobIds
            group by activity.job.id
            """)
    List<JobActivityLastTouchProjection> findLatestOccurredAtByJobIds(
            @Param("jobIds") Collection<Long> jobIds
    );

    @EntityGraph(attributePaths = "job")
    List<JobActivity> findAllByTypeInAndOccurredAtBetweenOrderByOccurredAtAscCreatedAtAsc(
            Collection<JobActivityType> types,
            Instant start,
            Instant end
    );
}
