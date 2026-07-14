package com.ojtraining.manager.trainingdata.atcoder.app;

import com.ojtraining.manager.trainingdata.atcoder.config.AtcoderCollectorProperties;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import com.ojtraining.manager.trainingdata.common.app.account.TrainingUserDirectory;
import com.ojtraining.manager.trainingdata.common.collector.OjCollectionRequestExecutor;
import com.ojtraining.manager.trainingdata.common.collector.OjHandleAccountCollectionHandleResolver;
import com.ojtraining.manager.trainingdata.common.collector.OjSubmissionCollectionService;
import com.ojtraining.manager.trainingdata.common.collector.lock.OjCollectionConsistencyGuard;
import com.ojtraining.manager.trainingdata.common.collector.dispatch.OjRecentSubmissionCollector;
import com.ojtraining.manager.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;

public class AtcoderSubmissionCollectionService implements OjRecentSubmissionCollector {
    private final OjSubmissionCollectionService delegate;

    public AtcoderSubmissionCollectionService(
            TrainingUserDirectory handleAccountService,
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            AtcoderCollectorProperties properties
    ) {
        this(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                properties,
                OjCollectionConsistencyGuard.passthrough()
        );
    }

    public AtcoderSubmissionCollectionService(
            TrainingUserDirectory handleAccountService,
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            AtcoderCollectorProperties properties,
            OjCollectionConsistencyGuard consistencyGuard
    ) {
        this(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                properties.pageSize(),
                properties.maxRequestAttempts(),
                properties.requestInterval(),
                Clock.systemUTC(),
                duration -> Thread.sleep(duration.toMillis()),
                consistencyGuard
        );
    }

    public AtcoderSubmissionCollectionService(
            TrainingUserDirectory handleAccountService,
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            int pageSize,
            int maxRequestAttempts,
            Duration requestInterval,
            Clock clock,
            OjCollectionRequestExecutor.SleepStrategy sleepStrategy
    ) {
        this(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                pageSize,
                maxRequestAttempts,
                requestInterval,
                clock,
                sleepStrategy,
                OjCollectionConsistencyGuard.passthrough()
        );
    }

    public AtcoderSubmissionCollectionService(
            TrainingUserDirectory handleAccountService,
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            int pageSize,
            int maxRequestAttempts,
            Duration requestInterval,
            Clock clock,
            OjCollectionRequestExecutor.SleepStrategy sleepStrategy,
            OjCollectionConsistencyGuard consistencyGuard
    ) {
        this.delegate = new OjSubmissionCollectionService(
                new OjHandleAccountCollectionHandleResolver(handleAccountService),
                new AtcoderSubmissionCollectionAdapter(sourceClient, ingestService, objectMapper, pageSize),
                maxRequestAttempts,
                requestInterval,
                clock,
                sleepStrategy,
                consistencyGuard
        );
    }

    @Override
    public String ojName() {
        return OjNames.ATCODER;
    }

    @Override
    public OjSubmissionCollectionResult collectRecentWindowForConfiguredHandles(
            Duration lookback
    ) throws JsonProcessingException {
        return delegate.collectRecentWindowForConfiguredHandles(OjNames.ATCODER, lookback);
    }

    @Override
    public OjSubmissionCollectionResult collectRecentWindowForUsername(
            String username,
            Duration lookback
    ) throws JsonProcessingException {
        return delegate.collectRecentWindowForUsername(OjNames.ATCODER, username, lookback);
    }

}
