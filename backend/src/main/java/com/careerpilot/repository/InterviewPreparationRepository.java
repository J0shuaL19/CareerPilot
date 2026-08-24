package com.careerpilot.repository;

import com.careerpilot.model.InterviewPreparation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewPreparationRepository
        extends JpaRepository<InterviewPreparation, Long> {

    Optional<InterviewPreparation> findByActivity_Id(Long activityId);

    List<InterviewPreparation> findAllByActivity_IdIn(Collection<Long> activityIds);
}
