package com.ojtraining.manager.trainingdata.common.collector.lock;

import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Objects;

/**
 * JDBC implementation backed by one fixed fence row per OJ.
 *
 * @author huangbingrui.awa
 */
public class JdbcOjDataConsistencyFence implements OjDataConsistencyFence {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcOjDataConsistencyFence(
            NamedParameterJdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager must not be null")
        );
    }

    @Override
    public <T> T mutateBindings(BindingMutation<T> mutation) {
        List<String> normalizedOjNames = OjNames.supportedNames().stream().sorted().toList();
        Objects.requireNonNull(mutation, "mutation must not be null");
        return transactionTemplate.execute(status -> {
            List<String> lockedOjNames = jdbcTemplate.queryForList("""
                            select oj_name
                            from oj_data_consistency_fence
                            where oj_name in (:ojNames)
                            order by oj_name
                            for update
                            """,
                    new MapSqlParameterSource("ojNames", normalizedOjNames),
                    String.class);
            if (!lockedOjNames.equals(normalizedOjNames)) {
                throw new IllegalStateException(
                        "missing OJ data consistency fence, expected=" + normalizedOjNames
                                + ", actual=" + lockedOjNames
                );
            }
            T result = mutation.execute(List.copyOf(lockedOjNames));
            int updated = jdbcTemplate.update("""
                            update oj_data_consistency_fence
                            set generation = generation + 1,
                                updated_at = current_timestamp(6)
                            where oj_name in (:ojNames)
                            """,
                    new MapSqlParameterSource("ojNames", normalizedOjNames));
            if (updated != normalizedOjNames.size()) {
                throw new IllegalStateException(
                        "expected to advance " + normalizedOjNames.size()
                                + " OJ data consistency fences, updated=" + updated
                );
            }
            return result;
        });
    }
}
