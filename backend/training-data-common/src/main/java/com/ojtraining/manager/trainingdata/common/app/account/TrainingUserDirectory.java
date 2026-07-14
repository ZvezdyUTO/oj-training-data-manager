package com.ojtraining.manager.trainingdata.common.app.account;

import com.ojtraining.manager.trainingdata.common.domain.oj.model.OjHandleAccount;

import java.time.Instant;
import java.util.List;

public interface TrainingUserDirectory {
    List<OjHandleAccount> listAll();

    OjHandleAccount getByUsername(String username);

    OjHandleAccount getByHandle(String ojName, String handle);

    String getHandle(OjHandleAccount account, String ojName);

    void markCollectedByHandle(
            String ojName, String handle, Instant collectedAt);
}
