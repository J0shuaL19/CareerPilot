package com.careerpilot.repository;

import com.careerpilot.model.Resume;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findAllByOrderByCreatedAtDesc();
}
