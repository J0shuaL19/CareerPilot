package com.careerpilot.service;

import com.careerpilot.dto.JobAttentionHistoryResponse;
import com.careerpilot.model.JobAttentionEvent;
import com.careerpilot.repository.JobAttentionEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobAttentionHistoryService {

    private final JobAttentionEventRepository jobAttentionEventRepository;

    public JobAttentionHistoryService(JobAttentionEventRepository jobAttentionEventRepository) {
        this.jobAttentionEventRepository = jobAttentionEventRepository;
    }

    @Transactional(readOnly = true)
    public List<JobAttentionHistoryResponse> getRecentHistory() {
        return jobAttentionEventRepository.findTop20ByOrderByCreatedAtDescIdDesc()
                .stream()
                .map(JobAttentionHistoryService::toResponse)
                .toList();
    }

    private static JobAttentionHistoryResponse toResponse(JobAttentionEvent event) {
        return new JobAttentionHistoryResponse(
                event.getId(),
                event.getJob().getId(),
                event.getJob().getCompany(),
                event.getJob().getTitle(),
                event.getAction(),
                event.getPreviousSnoozedUntil(),
                event.getNewSnoozedUntil(),
                event.getCreatedAt()
        );
    }
}