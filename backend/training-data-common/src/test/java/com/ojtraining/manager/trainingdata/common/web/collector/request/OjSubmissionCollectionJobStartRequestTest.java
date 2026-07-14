package com.ojtraining.manager.trainingdata.common.web.collector.request;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Author: huangbingrui.awa
class OjSubmissionCollectionJobStartRequestTest {
    @Test
    void acceptsZeroAndPerUsernameLookbacks() {
        OjSubmissionCollectionJobStartRequest request = new OjSubmissionCollectionJobStartRequest(
                List.of("new-user", "existing-user"),
                24L,
                Map.of("new-user", 0L, "existing-user", 6L),
                true,
                "CODEFORCES"
        );

        assertThat(request.requireLookbackDuration()).isEqualTo(Duration.ofHours(24));
        assertThat(request.requireLookbackDurationsByUsername()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "new-user", Duration.ZERO,
                "existing-user", Duration.ofHours(6)
        ));
    }

    @Test
    void rejectsNegativeLookbacks() {
        OjSubmissionCollectionJobStartRequest negativeDefault = new OjSubmissionCollectionJobStartRequest(
                List.of("alice"),
                -1L,
                Map.of(),
                true,
                "CODEFORCES"
        );
        OjSubmissionCollectionJobStartRequest negativeOverride = new OjSubmissionCollectionJobStartRequest(
                List.of("alice"),
                24L,
                Map.of("alice", -1L),
                true,
                "CODEFORCES"
        );

        assertThatThrownBy(negativeDefault::requireLookbackDuration)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
        assertThatThrownBy(negativeOverride::requireLookbackDurationsByUsername)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
    }
}
