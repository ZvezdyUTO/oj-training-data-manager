package com.ojtraining.manager.trainingdata.codeforces.domain;

import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjOdsDataPurgeRepository;
import com.ojtraining.manager.trainingdata.common.domain.oj.value.OjNames;

public interface CodeforcesOdsDataPurgeRepository extends OjOdsDataPurgeRepository {
    @Override
    default String ojName() {
        return OjNames.CODEFORCES;
    }
}
