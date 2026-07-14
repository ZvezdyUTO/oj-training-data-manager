package com.ojtraining.manager.trainingdata.codeforces.app;

import java.time.Instant;

public record CodeforcesOdsBatchUpsertResult(
        String batchId,
        String tableName,
        int writtenRows,
        Instant fetchedAt
) {
}
