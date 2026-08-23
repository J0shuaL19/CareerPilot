package com.careerpilot.repository;

import com.careerpilot.model.JobActivity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobActivityRepository extends JpaRepository<JobActivity, Long> {

    List<JobActivity> findAllByJob_IdOrderByOccurredAtDescCreatedAtDesc(Long jobId);

    Optional<JobActivity> findByIdAndJob_Id(Long id, Long jobId);
}
