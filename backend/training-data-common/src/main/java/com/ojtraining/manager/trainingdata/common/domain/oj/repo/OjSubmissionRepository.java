package com.ojtraining.manager.trainingdata.common.domain.oj.repo;

import com.ojtraining.manager.trainingdata.common.domain.oj.criteria.OjHandleSubmissionCriteria;
import com.ojtraining.manager.trainingdata.common.domain.oj.criteria.OjProblemSubmissionCriteria;
import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjSubmission;

import java.util.List;

public interface OjSubmissionRepository {
    long countHandleSubmissions(OjHandleSubmissionCriteria query);

    List<OjSubmission> findHandleSubmissions(OjHandleSubmissionCriteria query);

    long countProblemSubmissions(OjProblemSubmissionCriteria query);

    List<OjSubmission> findProblemSubmissions(OjProblemSubmissionCriteria query);
}
