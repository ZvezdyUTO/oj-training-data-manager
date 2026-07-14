package com.ojtraining.manager.trainingdata.common.collector.lock;

import java.util.List;

/**
 * Serializes member binding mutations with collection finalization and warehouse refresh.
 *
 * @author huangbingrui.awa
 */
public interface OjDataConsistencyFence {
    <T> T mutateBindings(BindingMutation<T> mutation);

    static OjDataConsistencyFence passthrough() {
        return new OjDataConsistencyFence() {
            @Override
            public <T> T mutateBindings(BindingMutation<T> mutation) {
                return mutation.execute(List.of("ATCODER", "CODEFORCES"));
            }
        };
    }

    @FunctionalInterface
    interface BindingMutation<T> {
        T execute(List<String> lockedOjNames);
    }
}
