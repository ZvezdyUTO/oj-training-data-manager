package com.ojtraining.manager.trainingdata.common.collector.lock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

import static com.ojtraining.manager.trainingdata.common.support.Texts.requireText;

public class JdbcOjCollectionConsistencyGuard implements OjCollectionConsistencyGuard {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcOjCollectionConsistencyGuard(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager must not be null")
        );
    }

    @Override
    public CollectionSnapshot snapshot(String ojName, CollectionTargetReader targetReader) {
        String normalizedOjName = OjNames.normalize(ojName);
        Objects.requireNonNull(targetReader, "targetReader must not be null");
        return transactionTemplate.execute(status -> {
            Long generation = lockGeneration(normalizedOjName);
            return new CollectionSnapshot(
                    normalizedOjName,
                    generation,
                    normalizeTargets(targetReader.readTargets())
            );
        });
    }

    @Override
    public <T> T withLockedHandles(
            CollectionSnapshot snapshot,
            boolean advanceGeneration,
            LockedHandleWork<T> work
    ) throws JsonProcessingException {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        return withLockedHandles(
                snapshot.ojName(),
                snapshot.handles(),
                snapshot.generation(),
                advanceGeneration,
                work
        );
    }

    @Override
    public <T> T withLockedHandles(
            String ojName,
            List<String> handles,
            LockedHandleWork<T> work
    ) throws JsonProcessingException {
        return withLockedHandles(ojName, handles, null, false, work);
    }

    private <T> T withLockedHandles(
            String ojName,
            List<String> handles,
            Long expectedGeneration,
            boolean incrementGeneration,
            LockedHandleWork<T> work
    ) throws JsonProcessingException {
        String normalizedOjName = OjNames.normalize(ojName);
        List<String> normalizedHandles = normalizeHandles(handles);
        Objects.requireNonNull(work, "work must not be null");
        try {
            return transactionTemplate.execute(status -> {
                Long lockedGeneration = lockGeneration(normalizedOjName);
                if (expectedGeneration != null && lockedGeneration.longValue() != expectedGeneration.longValue()) {
                    return executeWork(work, List.of());
                }
                List<String> lockedHandles = normalizedHandles.isEmpty()
                        ? List.of()
                        : jdbcTemplate.queryForList("""
                                select handle
                                from oj_handle_binding
                                where oj_name = :ojName
                                  and handle in (:handles)
                                order by handle
                                """,
                        new MapSqlParameterSource()
                                .addValue("ojName", normalizedOjName)
                                .addValue("handles", normalizedHandles),
                        String.class);
                T result = executeWork(work, List.copyOf(lockedHandles));
                if (incrementGeneration) {
                    int updated = jdbcTemplate.update("""
                                    update oj_data_consistency_fence
                                    set generation = generation + 1,
                                        updated_at = current_timestamp(6)
                                    where oj_name = :ojName
                                    """,
                            new MapSqlParameterSource("ojName", normalizedOjName));
                    if (updated != 1) {
                        throw new IllegalStateException(
                                "expected to advance one OJ data consistency fence, updated=" + updated
                        );
                    }
                }
                return result;
            });
        } catch (CollectionWorkException exception) {
            throw exception.cause;
        }
    }

    private Long lockGeneration(String normalizedOjName) {
        Long generation = jdbcTemplate.queryForObject("""
                        select generation
                        from oj_data_consistency_fence
                        where oj_name = :ojName
                        for update
                        """,
                new MapSqlParameterSource("ojName", normalizedOjName),
                Long.class);
        if (generation == null) {
            throw new IllegalStateException("missing OJ data consistency fence: " + normalizedOjName);
        }
        return generation;
    }

    private static <T> T executeWork(LockedHandleWork<T> work, List<String> lockedHandles) {
        try {
            return work.execute(lockedHandles);
        } catch (JsonProcessingException exception) {
            throw new CollectionWorkException(exception);
        }
    }

    private static List<String> normalizeHandles(List<String> handles) {
        if (handles == null) {
            return List.of();
        }
        return handles.stream()
                .map(handle -> requireText(handle, "handle"))
                .distinct()
                .sorted()
                .toList();
    }

    private static List<CollectionTarget> normalizeTargets(List<CollectionTarget> targets) {
        if (targets == null) {
            return List.of();
        }
        return targets.stream()
                .filter(Objects::nonNull)
                .map(target -> new CollectionTarget(
                        requireText(target.handle(), "handle"),
                        target.lastCollectedAt()
                ))
                .distinct()
                .sorted((left, right) -> left.handle().compareTo(right.handle()))
                .toList();
    }

    private static final class CollectionWorkException extends RuntimeException {
        private final JsonProcessingException cause;

        private CollectionWorkException(JsonProcessingException cause) {
            super(cause);
            this.cause = cause;
        }
    }
}
