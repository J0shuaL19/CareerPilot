package com.careerpilot.repository;

import com.careerpilot.model.JobAttentionEvent;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAttentionEventRepository extends JpaRepository<JobAttentionEvent, Long> {

    @EntityGraph(attributePaths = "job")
    List<JobAttentionEvent> findTop20ByOrderByCreatedAtDescIdDesc();
}