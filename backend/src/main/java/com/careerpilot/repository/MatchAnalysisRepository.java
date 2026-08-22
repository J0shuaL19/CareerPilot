package com.careerpilot.repository;

import com.careerpilot.model.MatchAnalysis;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchAnalysisRepository extends JpaRepository<MatchAnalysis, Long> {

    @Override
    @EntityGraph(attributePaths = {"job", "resume"})
    Optional<MatchAnalysis> findById(Long id);

    @EntityGraph(attributePaths = {"job", "resume"})
    List<MatchAnalysis> findAllByOrderByCreatedAtDesc();

    @Query("""
            SELECT analysis
            FROM MatchAnalysis analysis
            JOIN FETCH analysis.job job
            JOIN FETCH analysis.resume resume
            WHERE job.id = :jobId AND resume.id = :resumeId
            ORDER BY analysis.createdAt DESC
            """)
    List<MatchAnalysis> findAllByJob_IdAndResume_IdOrderByCreatedAtDesc(
            @Param("jobId") Long jobId,
            @Param("resumeId") Long resumeId
    );
}
