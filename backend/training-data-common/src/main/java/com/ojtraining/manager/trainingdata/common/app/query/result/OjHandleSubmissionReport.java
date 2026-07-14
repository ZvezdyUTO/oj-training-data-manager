package com.ojtraining.manager.trainingdata.common.app.query.result;

import java.util.List;

public record OjHandleSubmissionReport(
        String username,
        String authorHandle,
        int page,
        int limit,
        long total,
        long totalPages,
        boolean hasMore,
        List<OjSubmissionItem> submissions
) {
}
