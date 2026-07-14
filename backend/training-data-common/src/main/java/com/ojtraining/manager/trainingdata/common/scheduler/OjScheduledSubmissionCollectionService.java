package com.ojtraining.manager.trainingdata.common.scheduler;

import com.ojtraining.manager.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;

public interface OjScheduledSubmissionCollectionService {
    OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            String ojName,
            Duration lookback
    ) throws JsonProcessingException;

    OjSubmissionCollectionResult collectRecentWindowForUsername(
            String ojName,
            String username,
            Duration lookback
    ) throws JsonProcessingException;
}
