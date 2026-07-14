package com.ojtraining.manager.trainingdata.atcoder.domain;

import java.util.List;

public interface AtcoderOdsSubmissionWriter {
    void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsSubmission> submissions);
}
