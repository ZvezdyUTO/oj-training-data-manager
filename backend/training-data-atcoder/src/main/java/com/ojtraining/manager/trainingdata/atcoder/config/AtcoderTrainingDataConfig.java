package com.ojtraining.manager.trainingdata.atcoder.config;

import com.ojtraining.manager.common.sqltask.SqlTaskRunner;
import com.ojtraining.manager.trainingdata.atcoder.app.AtcoderOdsIngestService;
import com.ojtraining.manager.trainingdata.atcoder.app.AtcoderProblemListCollectionService;
import com.ojtraining.manager.trainingdata.atcoder.app.AtcoderSubmissionCollectionService;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderOdsProblemModelWriter;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderOdsProblemWriter;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderOdsSubmissionWriter;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderProblemModelPayloadParser;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderProblemPayloadParser;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderProblemSourceClient;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderSubmissionPayloadParser;
import com.ojtraining.manager.trainingdata.atcoder.domain.AtcoderSubmissionSourceClient;
import com.ojtraining.manager.trainingdata.atcoder.infra.JdbcAtcoderOdsDataPurgeRepository;
import com.ojtraining.manager.trainingdata.atcoder.infra.JacksonAtcoderPayloadParser;
import com.ojtraining.manager.trainingdata.atcoder.infra.JdbcAtcoderOdsProblemModelWriter;
import com.ojtraining.manager.trainingdata.atcoder.infra.JdbcAtcoderOdsProblemWriter;
import com.ojtraining.manager.trainingdata.atcoder.infra.JdbcAtcoderOdsSubmissionWriter;
import com.ojtraining.manager.trainingdata.atcoder.infra.JdbcAtcoderWarehouseRefreshIntervalRepository;
import com.ojtraining.manager.trainingdata.atcoder.infra.RestClientAtcoderSourceClient;
import com.ojtraining.manager.trainingdata.common.app.account.TrainingUserDirectory;
import com.ojtraining.manager.trainingdata.common.app.warehouse.OjWarehouseRefreshService;
import com.ojtraining.manager.trainingdata.common.collector.job.OjWarehouseRefreshHandler;
import com.ojtraining.manager.trainingdata.common.collector.job.SqlTaskOjWarehouseRefreshHandler;
import com.ojtraining.manager.trainingdata.common.collector.lock.OjCollectionConsistencyGuard;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjWarehouseRefreshIntervalRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
        AtcoderCollectorProperties.class,
        AtcoderProblemListCollectorProperties.class
})
public class AtcoderTrainingDataConfig {
    @Bean
    JacksonAtcoderPayloadParser atcoderPayloadParser(ObjectMapper objectMapper) {
        return new JacksonAtcoderPayloadParser(objectMapper);
    }

    @Bean
    AtcoderOdsSubmissionWriter atcoderOdsSubmissionWriter(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderOdsSubmissionWriter(jdbcTemplate);
    }

    @Bean
    AtcoderOdsProblemWriter atcoderOdsProblemWriter(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderOdsProblemWriter(jdbcTemplate);
    }

    @Bean
    AtcoderOdsProblemModelWriter atcoderOdsProblemModelWriter(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderOdsProblemModelWriter(jdbcTemplate);
    }

    @Bean
    RestClientAtcoderSourceClient atcoderSourceClient(
            AtcoderCollectorProperties properties
    ) {
        return new RestClientAtcoderSourceClient(RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(atcoderRequestFactory(properties))
                .build());
    }

    private static SimpleClientHttpRequestFactory atcoderRequestFactory(AtcoderCollectorProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }

    @Bean
    AtcoderOdsIngestService atcoderOdsIngestService(
            AtcoderSubmissionPayloadParser submissionParser,
            AtcoderProblemPayloadParser problemParser,
            AtcoderProblemModelPayloadParser problemModelParser,
            AtcoderOdsSubmissionWriter submissionWriter,
            AtcoderOdsProblemWriter problemWriter,
            AtcoderOdsProblemModelWriter problemModelWriter,
            ObjectMapper objectMapper
    ) {
        return new AtcoderOdsIngestService(
                submissionParser,
                problemParser,
                problemModelParser,
                submissionWriter,
                problemWriter,
                problemModelWriter,
                objectMapper
        );
    }

    @Bean
    AtcoderSubmissionCollectionService atcoderSubmissionCollectionService(
            TrainingUserDirectory handleAccountService,
            AtcoderSubmissionSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            ObjectMapper objectMapper,
            AtcoderCollectorProperties properties,
            OjCollectionConsistencyGuard consistencyGuard
    ) {
        return new AtcoderSubmissionCollectionService(
                handleAccountService,
                sourceClient,
                ingestService,
                objectMapper,
                properties,
                consistencyGuard
        );
    }

    @Bean
    AtcoderProblemListCollectionService atcoderProblemListCollectionService(
            AtcoderProblemSourceClient sourceClient,
            AtcoderOdsIngestService ingestService,
            AtcoderCollectorProperties properties
    ) {
        return new AtcoderProblemListCollectionService(sourceClient, ingestService, properties);
    }

    @Bean
    OjOdsDataPurgeRepository atcoderOdsDataPurgeRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderOdsDataPurgeRepository(jdbcTemplate);
    }

    @Bean
    OjWarehouseRefreshIntervalRepository atcoderWarehouseRefreshIntervalRepository(
            NamedParameterJdbcTemplate jdbcTemplate
    ) {
        return new JdbcAtcoderWarehouseRefreshIntervalRepository(jdbcTemplate);
    }

    @Bean
    OjWarehouseRefreshService atcoderWarehouseRefreshService(
            SqlTaskRunner sqlTaskRunner,
            @Qualifier("atcoderWarehouseRefreshIntervalRepository")
            OjWarehouseRefreshIntervalRepository intervalRepository
    ) {
        return new OjWarehouseRefreshService(
                sqlTaskRunner,
                intervalRepository,
                "classpath:sql/tasks/atcoder-warehouse-refresh.yml",
                "batchId has no AtCoder submissions"
        );
    }

    @Bean
    OjWarehouseRefreshHandler atcoderWarehouseRefreshHandler(
            @Qualifier("atcoderWarehouseRefreshService")
            OjWarehouseRefreshService refreshService
    ) {
        return new SqlTaskOjWarehouseRefreshHandler(OjNames.ATCODER, refreshService);
    }
}
