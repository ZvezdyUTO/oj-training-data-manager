package com.ojtraining.manager.api.member;

import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjStudentDataPurgeResult;

import java.util.List;

public record MemberDeleteResult(
        String username,
        List<OjStudentDataPurgeResult> purgeResults,
        int deletedMemberRows
) {
    public MemberDeleteResult {
        purgeResults = List.copyOf(purgeResults);
    }

    public int totalDeletedRows() {
        return deletedMemberRows + purgeResults.stream()
                .mapToInt(OjStudentDataPurgeResult::totalDeletedRows)
                .sum();
    }
}
