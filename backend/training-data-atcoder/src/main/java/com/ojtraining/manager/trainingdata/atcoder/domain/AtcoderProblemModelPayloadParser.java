package com.ojtraining.manager.trainingdata.atcoder.domain;

import java.util.List;

public interface AtcoderProblemModelPayloadParser {
    List<AtcoderOdsProblemModel> parseProblemModels(String problemModelPayload, AtcoderCollectBatch batch);
}
