package com.ojtraining.manager.trainingdata.atcoder.app;

public record AtcoderProblemMetadataCollectionResult(
        AtcoderOdsBatchUpsertResult problemResult,
        AtcoderOdsBatchUpsertResult problemModelResult
) {
    public int writtenRows() {
        return problemResult.writtenRows() + problemModelResult.writtenRows();
    }
}
