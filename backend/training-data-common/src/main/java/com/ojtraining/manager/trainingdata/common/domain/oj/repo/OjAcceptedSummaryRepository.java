package com.ojtraining.manager.trainingdata.common.domain.oj.repo;

import com.ojtraining.manager.trainingdata.common.domain.oj.criteria.OjAcceptedSummaryCriteria;
import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjDailyRatingAcceptedSummary;

import java.util.List;

public interface OjAcceptedSummaryRepository {
    List<OjDailyRatingAcceptedSummary> findDailyRatingAcceptedSummaries(
            OjAcceptedSummaryCriteria query
    );

    List<OjDailyRatingAcceptedSummary> findDailyRatingAcceptedSummaries(
            List<OjAcceptedSummaryCriteria> queries
    );
}
