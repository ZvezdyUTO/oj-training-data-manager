package com.ojtraining.manager.trainingdata.common.collector.lock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcOjCollectionConsistencyGuardTest {
    private ExecutorService executor;
    private JdbcTemplate jdbcTemplate;
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    private JdbcOjCollectionConsistencyGuard guard;
    private JdbcOjDataConsistencyFence mutationFence;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:collection_guard_" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        var transactionManager = new DataSourceTransactionManager(dataSource);
        guard = new JdbcOjCollectionConsistencyGuard(namedJdbcTemplate, transactionManager);
        mutationFence = new JdbcOjDataConsistencyFence(namedJdbcTemplate, transactionManager);
        executor = Executors.newFixedThreadPool(2);

        jdbcTemplate.execute("""
                create table oj_data_consistency_fence (
                    oj_name varchar(32) not null primary key,
                    generation bigint not null,
                    updated_at timestamp not null default current_timestamp
                )
                """);
        jdbcTemplate.update("""
                insert into oj_data_consistency_fence (oj_name, generation)
                values ('ATCODER', 0), ('CODEFORCES', 0)
                """);
        jdbcTemplate.execute("""
                create table oj_handle_binding (
                    username varchar(128) not null,
                    oj_name varchar(32) not null,
                    handle varchar(128) not null,
                    primary key (username, oj_name),
                    unique (oj_name, handle)
                )
                """);
        jdbcTemplate.execute("create table ods_test (handle varchar(128) not null)");
        jdbcTemplate.update("""
                insert into oj_handle_binding (username, oj_name, handle)
                values ('alice', 'CODEFORCES', 'old-handle')
                """);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void memberMutationWaitsForCollectionThenPurgesItsCommittedWrite() throws Exception {
        CountDownLatch collectionHasBindingLock = new CountDownLatch(1);
        CountDownLatch letCollectionWrite = new CountDownLatch(1);
        CountDownLatch mutationHasBindingLock = new CountDownLatch(1);

        var collection = executor.submit(() -> guard.withLockedHandles(
                "CODEFORCES",
                List.of("old-handle"),
                lockedHandles -> {
                    assertThat(lockedHandles).containsExactly("old-handle");
                    collectionHasBindingLock.countDown();
                    awaitLatch(letCollectionWrite);
                    jdbcTemplate.update("insert into ods_test (handle) values ('old-handle')");
                    return null;
                }
        ));
        assertThat(collectionHasBindingLock.await(2, TimeUnit.SECONDS)).isTrue();

        var mutation = executor.submit(() -> mutationFence.mutateBindings(lockedOjNames -> {
            assertThat(lockedOjNames).containsExactly("ATCODER", "CODEFORCES");
            mutationHasBindingLock.countDown();
            jdbcTemplate.update("delete from ods_test where handle = 'old-handle'");
            jdbcTemplate.update("delete from oj_handle_binding where username = 'alice'");
            return null;
        }));

        assertThat(mutationHasBindingLock.await(150, TimeUnit.MILLISECONDS)).isFalse();
        letCollectionWrite.countDown();
        collection.get(2, TimeUnit.SECONDS);
        mutation.get(2, TimeUnit.SECONDS);

        assertThat(jdbcTemplate.queryForObject("select count(*) from ods_test", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from oj_handle_binding", Integer.class)).isZero();
        assertThat(generation("ATCODER")).isEqualTo(1L);
        assertThat(generation("CODEFORCES")).isEqualTo(1L);
    }

    @Test
    void abaRebindingOfSameHandleInvalidatesCapturedGeneration() throws Exception {
        OjCollectionConsistencyGuard.CollectionSnapshot staleSnapshot = guard.snapshot(
                "CODEFORCES",
                () -> List.of(new OjCollectionConsistencyGuard.CollectionTarget("old-handle", null))
        );
        mutationFence.mutateBindings(ignored -> {
            jdbcTemplate.update("delete from oj_handle_binding where username = 'alice'");
            jdbcTemplate.update("""
                    insert into oj_handle_binding (username, oj_name, handle)
                    values ('bob', 'CODEFORCES', 'old-handle')
                    """);
            return null;
        });

        guard.withLockedHandles(staleSnapshot, true, lockedHandles -> {
            assertThat(lockedHandles).isEmpty();
            lockedHandles.forEach(handle -> jdbcTemplate.update(
                    "insert into ods_test (handle) values (?)", handle
            ));
            return null;
        });

        assertThat(jdbcTemplate.queryForObject("select count(*) from ods_test", Integer.class)).isZero();
        assertThat(generation("CODEFORCES")).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select username from oj_handle_binding where handle = 'old-handle'",
                String.class
        )).isEqualTo("bob");
    }

    @Test
    void onlyFinalizeThatCanAdvanceAHandleCursorIncrementsGeneration() throws Exception {
        OjCollectionConsistencyGuard.CollectionSnapshot failedSnapshot = guard.snapshot(
                "CODEFORCES",
                () -> List.of(new OjCollectionConsistencyGuard.CollectionTarget("old-handle", null))
        );

        guard.withLockedHandles(failedSnapshot, false, lockedHandles -> {
            assertThat(lockedHandles).containsExactly("old-handle");
            return null;
        });

        assertThat(generation("CODEFORCES")).isZero();

        OjCollectionConsistencyGuard.CollectionSnapshot noMatchSnapshot = guard.snapshot(
                "CODEFORCES",
                () -> List.of(new OjCollectionConsistencyGuard.CollectionTarget("old-handle", null))
        );
        guard.withLockedHandles(noMatchSnapshot, true, lockedHandles -> {
            assertThat(lockedHandles).containsExactly("old-handle");
            return null;
        });

        assertThat(generation("CODEFORCES")).isEqualTo(1L);
    }

    private Long generation(String ojName) {
        return jdbcTemplate.queryForObject(
                "select generation from oj_data_consistency_fence where oj_name = ?",
                Long.class,
                ojName
        );
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("test thread interrupted", exception);
        }
    }
}
