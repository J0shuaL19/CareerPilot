package com.careerpilot.repository;

import java.time.Instant;

public interface JobActivityLastTouchProjection {

    Long getJobId();

    Instant getLastOccurredAt();
}
