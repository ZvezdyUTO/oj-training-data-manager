package com.ojtraining.manager.trainingdata.common.web.collector.response;

import com.ojtraining.manager.trainingdata.common.collector.result.OjSubmissionCollectionHandleResult;
import com.ojtraining.manager.trainingdata.common.collector.result.OjSubmissionCollectionHandleStatus;

public record OjSubmissionCollectionHandleResponse(
        String handle,
        OjSubmissionCollectionHandleStatus status,
        int fetchedSubmissionCount,
        int matchedSubmissionCount,
        String errorCode,
        String message
) {
    public static OjSubmissionCollectionHandleResponse from(
            OjSubmissionCollectionHandleResult result
    ) {
        return new OjSubmissionCollectionHandleResponse(
                result.handle(),
                result.status(),
                result.fetchedSubmissionCount(),
                result.matchedSubmissionCount(),
                result.errorCode(),
                result.message()
        );
    }
}
