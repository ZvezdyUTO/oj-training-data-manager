package com.ojtraining.manager.trainingdata.common.config;

import com.ojtraining.manager.common.sqltask.SqlTaskRunner;
import com.ojtraining.manager.trainingdata.common.app.account.TrainingUserDirectory;
import com.ojtraining.manager.trainingdata.common.app.purge.OjStudentDataPurgeService;
import com.ojtraining.manager.trainingdata.common.app.query.OjAcceptedSummaryQueryService;
import com.ojtraining.manager.trainingdata.common.app.query.OjFirstAcceptedProblemQueryService;
import com.ojtraining.manager.trainingdata.common.app.query.OjSubmissionQueryService;
import com.ojtraining.manager.trainingdata.common.app.query.OjWarehouseQueryFacade;
import com.ojtraining.manager.trainingdata.common.collector.config.OjCollectorSchedulingProperties;
import com.ojtraining.manager.trainingdata.common.collector.dispatch.OjRecentSubmissionCollector;
import com.ojtraining.manager.trainingdata.common.collector.dispatch.OjSubmissionCollectionDispatcher;
import com.ojtraining.manager.trainingdata.common.collector.job.OjSubmissionCollectionJobService;
import com.ojtraining.manager.trainingdata.common.collector.job.OjWarehouseRefreshDispatcher;
import com.ojtraining.manager.trainingdata.common.collector.job.OjWarehouseRefreshHandler;
import com.ojtraining.manager.trainingdata.common.collector.lock.JdbcOjCollectionConsistencyGuard;
import com.ojtraining.manager.trainingdata.common.collector.lock.JdbcOjDataConsistencyFence;
import com.ojtraining.manager.trainingdata.common.collector.lock.OjCollectionConsistencyGuard;
import com.ojtraining.manager.trainingdata.common.collector.lock.OjDataConsistencyFence;
import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjHandleCollectionState;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjAcceptedSummaryRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjFirstAcceptedProblemRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjSubmissionRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjWarehouseDataPurgeRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjDifficultyBucketPolicies;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;
import com.ojtraining.manager.trainingdata.common.infra.oj.repo.account.JdbcOjHandleAccountRepository;
import com.ojtraining.manager.trainingdata.common.infra.oj.repo.query.JdbcOjAcceptedSummaryRepository;
import com.ojtraining.manager.trainingdata.common.infra.oj.repo.query.JdbcOjFirstAcceptedProblemRepository;
import com.ojtraining.manager.trainingdata.common.infra.oj.repo.query.JdbcOjSubmissionRepository;
import com.ojtraining.manager.trainingdata.common.infra.oj.repo.warehouse.JdbcOjWarehouseDataPurgeRepository;
import com.ojtraining.manager.trainingdata.common.scheduler.OjScheduledSubmissionCollectionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties(OjCollectorSchedulingProperties.class)
public class CommonTrainingDataConfig {
    @Bean
    SqlTaskRunner sqlTaskRunner(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            ResourceLoader resourceLoader
    ) {
        return new SqlTaskRunner(jdbcTemplate, transactionManager, resourceLoader);
    }

    @Bean
    OjDifficultyBucketPolicies ojDifficultyBucketPolicies() {
        return OjDifficultyBucketPolicies.defaults();
    }

    @Bean
    OjAcceptedSummaryRepository ojAcceptedSummaryRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            OjDifficultyBucketPolicies bucketPolicies
    ) {
        return new JdbcOjAcceptedSummaryRepository(jdbcTemplate, bucketPolicies);
    }

    @Bean
    OjSubmissionRepository ojSubmissionRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            OjDifficultyBucketPolicies bucketPolicies
    ) {
        return new JdbcOjSubmissionRepository(jdbcTemplate, bucketPolicies);
    }

    @Bean
    OjFirstAcceptedProblemRepository ojFirstAcceptedProblemRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            OjDifficultyBucketPolicies bucketPolicies
    ) {
        return new JdbcOjFirstAcceptedProblemRepository(jdbcTemplate, bucketPolicies);
    }

    @Bean
    OjHandleAccountRepository ojHandleAccountRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        return new JdbcOjHandleAccountRepository(jdbcTemplate, transactionManager);
    }

    @Bean
    OjCollectionConsistencyGuard ojCollectionConsistencyGuard(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        return new JdbcOjCollectionConsistencyGuard(jdbcTemplate, transactionManager);
    }

    @Bean
    OjDataConsistencyFence ojDataConsistencyFence(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        return new JdbcOjDataConsistencyFence(jdbcTemplate, transactionManager);
    }

    @Bean
    OjWarehouseDataPurgeRepository ojWarehouseDataPurgeRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        return new JdbcOjWarehouseDataPurgeRepository(jdbcTemplate, transactionManager);
    }

    @Bean
    OjAcceptedSummaryQueryService ojAcceptedSummaryQueryService(
            OjAcceptedSummaryRepository repository,
            TrainingUserDirectory handleAccountService,
            OjDifficultyBucketPolicies bucketPolicies
    ) {
        return new OjAcceptedSummaryQueryService(repository, handleAccountService, bucketPolicies);
    }

    @Bean
    OjSubmissionQueryService ojSubmissionQueryService(
            OjSubmissionRepository repository,
            TrainingUserDirectory handleAccountService
    ) {
        return new OjSubmissionQueryService(repository, handleAccountService);
    }

    @Bean
    OjFirstAcceptedProblemQueryService ojFirstAcceptedProblemQueryService(
            OjFirstAcceptedProblemRepository repository,
            TrainingUserDirectory handleAccountService
    ) {
        return new OjFirstAcceptedProblemQueryService(repository, handleAccountService);
    }

    @Bean
    OjWarehouseQueryFacade ojWarehouseQueryFacade(
            OjAcceptedSummaryQueryService acceptedSummaryQueryService,
            OjSubmissionQueryService submissionQueryService,
            OjFirstAcceptedProblemQueryService firstAcceptedProblemQueryService
    ) {
        return new OjWarehouseQueryFacade(
                acceptedSummaryQueryService,
                submissionQueryService,
                firstAcceptedProblemQueryService
        );
    }

    @Bean
    OjScheduledSubmissionCollectionService ojScheduledSubmissionCollectionService(
            List<OjRecentSubmissionCollector> collectors
    ) {
        return new OjSubmissionCollectionDispatcher(OjNames.CODEFORCES, collectors);
    }

    @Bean
    OjWarehouseRefreshDispatcher ojWarehouseRefreshDispatcher(
            List<OjWarehouseRefreshHandler> refreshHandlers,
            OjCollectionConsistencyGuard consistencyGuard
    ) {
        return new OjWarehouseRefreshDispatcher(refreshHandlers, consistencyGuard);
    }

    @Bean(destroyMethod = "shutdown")
    ExecutorService ojSubmissionCollectionJobExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "oj-submission-collection-job");
            thread.setDaemon(false);
            return thread;
        });
    }

    @Bean
    OjSubmissionCollectionJobService ojSubmissionCollectionJobService(
            OjScheduledSubmissionCollectionService collectionService,
            OjWarehouseRefreshDispatcher warehouseRefreshDispatcher,
            ExecutorService ojSubmissionCollectionJobExecutor,
            OjCollectorSchedulingProperties properties,
            TrainingUserDirectory handleAccountService
    ) {
        return new OjSubmissionCollectionJobService(
                collectionService::collectRecentWindowForUsername,
                warehouseRefreshDispatcher,
                ojSubmissionCollectionJobExecutor,
                properties.jobItemInterval(),
                (ojName, username) -> {
                    String effectiveOjName = ojName == null ? OjNames.CODEFORCES : OjNames.normalize(ojName);
                    return handleAccountService.getByUsername(username)
                            .collectionStates()
                            .getOrDefault(effectiveOjName, OjHandleCollectionState.empty())
                            .lastCollectedAt() != null;
                }
        );
    }

    @Bean
    OjStudentDataPurgeService ojStudentDataPurgeService(
            List<OjOdsDataPurgeRepository> odsDataPurgeRepositories,
            OjWarehouseDataPurgeRepository warehouseDataPurgeRepository,
            TrainingUserDirectory handleAccountService,
            PlatformTransactionManager transactionManager
    ) {
        return new OjStudentDataPurgeService(
                odsDataPurgeRepositories,
                warehouseDataPurgeRepository,
                handleAccountService,
                new TransactionTemplate(transactionManager)
        );
    }
}
