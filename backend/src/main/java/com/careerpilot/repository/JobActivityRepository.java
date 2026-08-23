package com.careerpilot.repository;

import com.careerpilot.model.JobActivity;
import com.careerpilot.model.JobActivityType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobActivityRepository extends JpaRepository<JobActivity, Long> {

    List<JobActivity> findAllByJob_IdOrderByOccurredAtDescCreatedAtDesc(Long jobId);

    Optional<JobActivity> findByIdAndJob_Id(Long id, Long jobId);

    @EntityGraph(attributePaths = "job")
    List<JobActivity> findAllByTypeInAndOccurredAtBetweenOrderByOccurredAtAscCreatedAtAsc(
            Collection<JobActivityType> types,
            Instant start,
            Instant end
    );
}
