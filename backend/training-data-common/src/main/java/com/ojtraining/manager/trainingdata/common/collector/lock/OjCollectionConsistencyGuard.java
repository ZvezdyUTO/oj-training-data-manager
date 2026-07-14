package com.ojtraining.manager.trainingdata.common.collector.lock;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Instant;
import java.util.List;

public interface OjCollectionConsistencyGuard {
    default CollectionSnapshot snapshot(String ojName, CollectionTargetReader targetReader) {
        return new CollectionSnapshot(ojName, 0L, targetReader.readTargets());
    }

    default <T> T withLockedHandles(
            CollectionSnapshot snapshot,
            boolean advanceGeneration,
            LockedHandleWork<T> work
    ) throws JsonProcessingException {
        return withLockedHandles(snapshot.ojName(), snapshot.handles(), work);
    }

    <T> T withLockedHandles(
            String ojName,
            List<String> handles,
            LockedHandleWork<T> work
    ) throws JsonProcessingException;

    static OjCollectionConsistencyGuard passthrough() {
        return new OjCollectionConsistencyGuard() {
            @Override
            public <T> T withLockedHandles(
                    String ojName,
                    List<String> handles,
                    LockedHandleWork<T> work
            ) throws JsonProcessingException {
                return work.execute(handles == null ? List.of() : List.copyOf(handles));
            }
        };
    }

    record CollectionTarget(String handle, Instant lastCollectedAt) {
    }

    record CollectionSnapshot(String ojName, long generation, List<CollectionTarget> targets) {
        public CollectionSnapshot {
            targets = targets == null ? List.of() : List.copyOf(targets);
        }

        public List<String> handles() {
            return targets.stream().map(CollectionTarget::handle).toList();
        }
    }

    @FunctionalInterface
    interface CollectionTargetReader {
        List<CollectionTarget> readTargets();
    }

    @FunctionalInterface
    interface LockedHandleWork<T> {
        T execute(List<String> lockedHandles) throws JsonProcessingException;
    }
}
