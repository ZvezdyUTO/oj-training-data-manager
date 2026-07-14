package com.ojtraining.manager.trainingdata.common.collector.job;

import com.ojtraining.manager.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.ojtraining.manager.trainingdata.common.collector.lock.OjCollectionConsistencyGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OjWarehouseRefreshDispatcher implements OjSubmissionCollectionJobService.RefreshHandler {
    private final Map<String, OjWarehouseRefreshHandler> handlersByOjName;
    private final OjCollectionConsistencyGuard consistencyGuard;

    public OjWarehouseRefreshDispatcher(List<OjWarehouseRefreshHandler> handlers) {
        this(handlers, OjCollectionConsistencyGuard.passthrough());
    }

    public OjWarehouseRefreshDispatcher(
            List<OjWarehouseRefreshHandler> handlers,
            OjCollectionConsistencyGuard consistencyGuard
    ) {
        this.handlersByOjName = handlersByOjName(handlers);
        this.consistencyGuard = Objects.requireNonNull(consistencyGuard, "consistencyGuard must not be null");
    }

    @Override
    public OjSubmissionCollectionJobRefreshResult refresh(OjSubmissionCollectionResult result) {
        String ojName = OjNames.normalize(result.ojName());
        OjWarehouseRefreshHandler handler = handlersByOjName.get(ojName);
        if (handler == null) {
            return OjSubmissionCollectionJobRefreshResult.failed(ojName + " warehouse refresh is not implemented");
        }
        try {
            return consistencyGuard.withLockedHandles(
                    ojName,
                    List.of(),
                    ignored -> handler.refresh(result.batchId())
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("unexpected JSON failure while locking warehouse refresh", exception);
        }
    }

    private static Map<String, OjWarehouseRefreshHandler> handlersByOjName(List<OjWarehouseRefreshHandler> handlers) {
        Map<String, OjWarehouseRefreshHandler> indexed = new LinkedHashMap<>();
        for (OjWarehouseRefreshHandler handler : handlers == null ? List.<OjWarehouseRefreshHandler>of() : handlers) {
            OjWarehouseRefreshHandler nonNullHandler = Objects.requireNonNull(handler, "handler must not be null");
            String ojName = OjNames.normalize(nonNullHandler.ojName());
            if (indexed.putIfAbsent(ojName, nonNullHandler) != null) {
                throw new IllegalArgumentException("duplicate OJ warehouse refresh handler: " + ojName);
            }
        }
        return Map.copyOf(indexed);
    }
}
