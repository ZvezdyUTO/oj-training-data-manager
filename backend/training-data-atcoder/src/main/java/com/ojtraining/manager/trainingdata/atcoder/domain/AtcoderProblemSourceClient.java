package com.ojtraining.manager.trainingdata.atcoder.domain;

import com.fasterxml.jackson.databind.JsonNode;

public interface AtcoderProblemSourceClient {
    JsonNode fetchProblems();

    JsonNode fetchProblemModels();
}
