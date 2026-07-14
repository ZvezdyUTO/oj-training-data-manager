package com.ojtraining.manager.trainingdata.atcoder.domain;

import java.util.List;

public interface AtcoderOdsProblemWriter {
    void upsertBatch(AtcoderCollectBatch batch, List<AtcoderOdsProblem> problems);
}
