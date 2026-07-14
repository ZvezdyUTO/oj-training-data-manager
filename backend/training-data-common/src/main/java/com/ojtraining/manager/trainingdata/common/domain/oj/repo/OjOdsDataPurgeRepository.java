package com.ojtraining.manager.trainingdata.common.domain.oj.repo;

public interface OjOdsDataPurgeRepository {
    String ojName();

    int purgeAllByHandle(String handle);
}
