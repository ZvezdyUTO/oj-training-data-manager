package com.ojtraining.manager.trainingdata.common.collector.job;

import com.ojtraining.manager.trainingdata.common.collector.result.OjSubmissionCollectionResult;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static com.ojtraining.manager.trainingdata.common.support.Texts.requireText;

public class OjSubmissionCollectionJobService {
    private static final int MAX_RETAINED_JOBS = 50;
    private static final Logger log = LoggerFactory.getLogger(OjSubmissionCollectionJobService.class);

    private final RecentIdentityCollector collector;
    private final RefreshHandler refreshHandler;
    private final Executor executor;
    private final Clock clock;
    private final Duration itemInterval;
    private final CollectionCursorResolver collectionCursorResolver;
    private final SleepStrategy sleepStrategy;
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor
    ) {
        this(collector, refreshHandler, executor, Clock.systemUTC(), Duration.ZERO);
    }

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Duration itemInterval
    ) {
        this(collector, refreshHandler, executor, Clock.systemUTC(), itemInterval);
    }

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Duration itemInterval,
            CollectionCursorResolver collectionCursorResolver
    ) {
        this(
                collector,
                refreshHandler,
                executor,
                Clock.systemUTC(),
                itemInterval,
                collectionCursorResolver,
                duration -> Thread.sleep(duration.toMillis())
        );
    }

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Clock clock
    ) {
        this(collector, refreshHandler, executor, clock, Duration.ZERO);
    }

    public OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Clock clock,
            Duration itemInterval
    ) {
        this(
                collector,
                refreshHandler,
                executor,
                clock,
                itemInterval,
                (ojName, username) -> true,
                duration -> Thread.sleep(duration.toMillis())
        );
    }

    OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Clock clock,
            Duration itemInterval,
            SleepStrategy sleepStrategy
    ) {
        this(
                collector,
                refreshHandler,
                executor,
                clock,
                itemInterval,
                (ojName, username) -> true,
                sleepStrategy
        );
    }

    OjSubmissionCollectionJobService(
            RecentIdentityCollector collector,
            RefreshHandler refreshHandler,
            Executor executor,
            Clock clock,
            Duration itemInterval,
            CollectionCursorResolver collectionCursorResolver,
            SleepStrategy sleepStrategy
    ) {
        this.collector = Objects.requireNonNull(collector, "collector must not be null");
        this.refreshHandler = Objects.requireNonNull(refreshHandler, "refreshHandler must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.itemInterval = itemInterval == null || itemInterval.isNegative() ? Duration.ZERO : itemInterval;
        this.collectionCursorResolver = Objects.requireNonNull(
                collectionCursorResolver,
                "collectionCursorResolver must not be null"
        );
        this.sleepStrategy = Objects.requireNonNull(sleepStrategy, "sleepStrategy must not be null");
    }

    public OjSubmissionCollectionJobSnapshot startBatchCollection(
            List<String> usernames,
            Duration lookback,
            Map<String, Duration> lookbacksByUsername,
            boolean refreshWarehouse,
            String ojName
    ) {
        List<String> identities = normalizeIdentities(usernames);
        Map<String, Duration> identityLookbacks = normalizeIdentityLookbacks(
                identities,
                lookback,
                lookbacksByUsername
        );
        synchronized (jobs) {
            OjSubmissionCollectionJobSnapshot active = activeJob();
            if (active != null) {
                return active;
            }
            String jobId = UUID.randomUUID().toString();
            String normalizedOjName = normalizeOptionalText(ojName);
            JobState state = new JobState(jobId, normalizedOjName, identities, clock.instant(), "采集任务已创建");
            jobs.put(jobId, state);
            pruneCompletedJobs();
            executor.execute(() -> runJob(
                    state,
                    identities,
                    identityLookbacks,
                    refreshWarehouse,
                    normalizedOjName
            ));
            return state.snapshot();
        }
    }

    public OjSubmissionCollectionJobSnapshot startBatchCollection(
            List<String> usernames,
            Duration lookback,
            boolean refreshWarehouse,
            String ojName
    ) {
        return startBatchCollection(usernames, lookback, Map.of(), refreshWarehouse, ojName);
    }

    public OjSubmissionCollectionJobSnapshot startBatchCollection(
            List<String> usernames,
            Duration lookback,
            boolean refreshWarehouse
    ) {
        return startBatchCollection(usernames, lookback, refreshWarehouse, null);
    }

    public OjSubmissionCollectionJobSnapshot getJob(String jobId) {
        String normalizedJobId = requireText(jobId, "jobId");
        JobState state = jobs.get(normalizedJobId);
        if (state == null) {
            throw new NoSuchElementException("OJ submission collection job not found: " + normalizedJobId);
        }
        return state.snapshot();
    }

    public List<OjSubmissionCollectionJobSnapshot> listJobs() {
        return jobs.values().stream()
                .map(JobState::snapshot)
                .sorted((left, right) -> right.startedAt().compareTo(left.startedAt()))
                .toList();
    }

    private OjSubmissionCollectionJobSnapshot activeJob() {
        return jobs.values().stream()
                .map(JobState::snapshot)
                .filter(job -> job.status() == OjSubmissionCollectionJobStatus.RUNNING)
                .findFirst()
                .orElse(null);
    }

    private void runJob(
            JobState state,
            List<String> identities,
            Map<String, Duration> lookbacksByUsername,
            boolean refreshWarehouse,
            String ojName
    ) {
        state.updateMessage("采集任务运行中");
        for (int index = 0; index < identities.size(); index++) {
            String identity = identities.get(index);
            if (index > 0 && !sleepBeforeNextIdentity(state, identities.subList(index, identities.size()))) {
                break;
            }
            state.markRunning(identity);
            try {
                Duration requestedLookback = lookbacksByUsername.get(identity);
                Duration effectiveLookback = collectionCursorResolver.hasSuccessfulCollection(ojName, identity)
                        ? requestedLookback
                        : Duration.ZERO;
                OjSubmissionCollectionResult collectionResult = collector.collect(
                        ojName,
                        identity,
                        effectiveLookback
                );
                OjSubmissionCollectionJobRefreshResult refreshResult = refreshResult(collectionResult, refreshWarehouse);
                state.markCollected(identity, OjSubmissionCollectionJobItem.collected(
                        identity,
                        collectionResult,
                        refreshResult
                ));
            } catch (Exception ex) {
                log.error(
                        "OJ collection job item failed, errorCode=OJ_COLLECTION_JOB_ITEM_FAILED, jobId={}, ojName={}",
                        state.jobId,
                        ojName,
                        ex
                );
                state.markCollected(identity, OjSubmissionCollectionJobItem.failed(identity, ojName, ex.getMessage()));
            }
        }
        state.finish(clock.instant());
    }

    private OjSubmissionCollectionJobRefreshResult refreshResult(
            OjSubmissionCollectionResult collectionResult,
            boolean refreshWarehouse
    ) {
        if (!refreshWarehouse) {
            return OjSubmissionCollectionJobRefreshResult.notRequested();
        }
        if (collectionResult.batchId() == null) {
            return OjSubmissionCollectionJobRefreshResult.noBatch();
        }
        try {
            return Objects.requireNonNull(
                    refreshHandler.refresh(collectionResult),
                    "refresh result must not be null"
            );
        } catch (Exception ex) {
            log.error(
                    "OJ collection job warehouse refresh failed, errorCode=OJ_COLLECTION_JOB_REFRESH_FAILED, ojName={}, batchId={}",
                    collectionResult.ojName(),
                    collectionResult.batchId(),
                    ex
            );
            return OjSubmissionCollectionJobRefreshResult.failed(ex.getMessage());
        }
    }

    private boolean sleepBeforeNextIdentity(JobState state, List<String> remainingIdentities) {
        if (itemInterval.isZero()) {
            return true;
        }
        try {
            sleepStrategy.sleep(itemInterval);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            state.updateMessage("采集任务被中断");
            remainingIdentities.forEach(identity -> state.markCollected(
                    identity,
                    OjSubmissionCollectionJobItem.failed(
                            identity,
                            state.ojName,
                            "interrupted while rate limiting OJ submission collection job"
                    )
            ));
            return false;
        }
    }

    private void pruneCompletedJobs() {
        List<JobState> completed = jobs.values().stream()
                .filter(state -> state.snapshot().status() != OjSubmissionCollectionJobStatus.RUNNING)
                .sorted((left, right) -> left.snapshot().startedAt().compareTo(right.snapshot().startedAt()))
                .toList();
        int removeCount = Math.max(0, jobs.size() - MAX_RETAINED_JOBS);
        for (int index = 0; index < removeCount && index < completed.size(); index++) {
            jobs.remove(completed.get(index).jobId);
        }
    }

    private static List<String> normalizeIdentities(List<String> usernames) {
        if (usernames == null) {
            throw new IllegalArgumentException("usernames must not be empty");
        }
        List<String> identities = usernames.stream()
                .map(identity -> requireText(identity, "username"))
                .distinct()
                .toList();
        if (identities.isEmpty()) {
            throw new IllegalArgumentException("usernames must not be empty");
        }
        return identities;
    }

    private static Map<String, Duration> normalizeIdentityLookbacks(
            List<String> identities,
            Duration defaultLookback,
            Map<String, Duration> lookbacksByUsername
    ) {
        requireNonNegativeDuration(defaultLookback, "lookback");
        Map<String, Duration> normalizedOverrides = new LinkedHashMap<>();
        if (lookbacksByUsername != null) {
            lookbacksByUsername.forEach((username, lookback) -> {
                String normalizedUsername = requireText(username, "lookback username");
                requireNonNegativeDuration(lookback, "lookback for " + normalizedUsername);
                normalizedOverrides.put(normalizedUsername, lookback);
            });
        }
        Map<String, Duration> normalizedLookbacks = new LinkedHashMap<>();
        identities.forEach(identity -> normalizedLookbacks.put(
                identity,
                normalizedOverrides.getOrDefault(identity, defaultLookback)
        ));
        return Map.copyOf(normalizedLookbacks);
    }

    private static void requireNonNegativeDuration(Duration lookback, String fieldName) {
        if (lookback == null || lookback.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OjNames.normalize(value);
    }

    @FunctionalInterface
    public interface RecentIdentityCollector {
        OjSubmissionCollectionResult collect(String ojName, String username, Duration lookback) throws Exception;
    }

    @FunctionalInterface
    public interface RefreshHandler {
        OjSubmissionCollectionJobRefreshResult refresh(OjSubmissionCollectionResult result) throws Exception;
    }

    @FunctionalInterface
    public interface CollectionCursorResolver {
        boolean hasSuccessfulCollection(String ojName, String username);
    }

    interface SleepStrategy {
        void sleep(Duration duration) throws InterruptedException;
    }

    private static final class JobState {
        private final String jobId;
        private final String ojName;
        private final Instant startedAt;
        private final Map<String, OjSubmissionCollectionJobItem> items = new LinkedHashMap<>();
        private Instant finishedAt;
        private String message;

        private JobState(String jobId, String ojName, List<String> identities, Instant startedAt, String message) {
            this.jobId = jobId;
            this.ojName = ojName;
            this.startedAt = startedAt;
            this.message = message;
            identities.forEach(identity -> items.put(identity, OjSubmissionCollectionJobItem.pending(identity, ojName)));
        }

        private synchronized void updateMessage(String message) {
            this.message = message;
        }

        private synchronized void markRunning(String identity) {
            items.computeIfPresent(identity, (key, item) -> item.running());
        }

        private synchronized void markCollected(String identity, OjSubmissionCollectionJobItem item) {
            items.put(identity, item);
        }

        private synchronized void finish(Instant finishedAt) {
            this.finishedAt = finishedAt;
            this.message = "采集任务已完成";
        }

        private synchronized OjSubmissionCollectionJobSnapshot snapshot() {
            List<OjSubmissionCollectionJobItem> itemList = new ArrayList<>(items.values());
            int completedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == OjSubmissionCollectionJobItemStatus.SUCCESS
                            || item.itemStatus() == OjSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            int collectedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == OjSubmissionCollectionJobItemStatus.SUCCESS)
                    .count();
            int failedCount = (int) itemList.stream()
                    .filter(item -> item.itemStatus() == OjSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            int refreshedCount = (int) itemList.stream()
                    .filter(item -> item.refreshStatus() == OjSubmissionCollectionJobRefreshStatus.SUCCESS)
                    .count();
            int writtenRows = itemList.stream()
                    .mapToInt(OjSubmissionCollectionJobItem::writtenRows)
                    .sum();
            List<String> batchIds = itemList.stream()
                    .map(OjSubmissionCollectionJobItem::batchId)
                    .filter(batchId -> batchId != null && !batchId.isBlank())
                    .distinct()
                    .toList();
            return new OjSubmissionCollectionJobSnapshot(
                    jobId,
                    ojName,
                    status(itemList, finishedAt),
                    itemList.size(),
                    completedCount,
                    collectedCount,
                    failedCount,
                    refreshedCount,
                    writtenRows,
                    batchIds,
                    startedAt,
                    finishedAt,
                    message,
                    itemList
            );
        }

        private static OjSubmissionCollectionJobStatus status(
                List<OjSubmissionCollectionJobItem> items,
                Instant finishedAt
        ) {
            if (finishedAt == null) {
                return OjSubmissionCollectionJobStatus.RUNNING;
            }
            long failedCount = items.stream()
                    .filter(item -> item.itemStatus() == OjSubmissionCollectionJobItemStatus.FAILED)
                    .count();
            if (failedCount == 0) {
                return OjSubmissionCollectionJobStatus.SUCCESS;
            }
            return failedCount == items.size()
                    ? OjSubmissionCollectionJobStatus.FAILED
                    : OjSubmissionCollectionJobStatus.PARTIAL_SUCCESS;
        }
    }
}
