package com.careerpilot.repository;

import com.careerpilot.model.Job;
import com.careerpilot.model.JobStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findAllByOrderByCreatedAtDesc();

    List<Job> findAllByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant createdAt);

    List<Job> findAllByStatusInOrderByCreatedAtAsc(Collection<JobStatus> statuses);

    boolean existsByCompanyIgnoreCaseAndTitleIgnoreCase(String company, String title);
}
