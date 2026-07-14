package com.ojtraining.manager.trainingdata.common.domain.oj.repo;

import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjWarehouseRefreshInterval;

import java.util.Optional;

public interface OjWarehouseRefreshIntervalRepository {
    Optional<String> findLatestBatchId();

    Optional<OjWarehouseRefreshInterval> findBatchDateInterval(String batchId);
}
