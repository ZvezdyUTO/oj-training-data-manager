package com.ojtraining.manager.trainingdata.common.collector.job;

public interface OjWarehouseRefreshHandler {
    String ojName();

    OjSubmissionCollectionJobRefreshResult refresh(String batchId);
}
