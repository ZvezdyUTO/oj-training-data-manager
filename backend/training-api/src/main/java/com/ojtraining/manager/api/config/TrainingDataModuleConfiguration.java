package com.ojtraining.manager.api.config;

import com.ojtraining.manager.trainingdata.atcoder.config.AtcoderProblemListSchedulingConfig;
import com.ojtraining.manager.trainingdata.atcoder.config.AtcoderProblemListBootstrapRunner;
import com.ojtraining.manager.trainingdata.atcoder.config.AtcoderTrainingDataConfig;
import com.ojtraining.manager.trainingdata.codeforces.config.CodeforcesTrainingDataConfig;
import com.ojtraining.manager.trainingdata.common.app.account.OjHandleAccountService;
import com.ojtraining.manager.trainingdata.common.config.CommonTrainingDataConfig;
import com.ojtraining.manager.trainingdata.common.domain.oj.repo.OjHandleAccountRepository;
import com.ojtraining.manager.trainingdata.common.scheduler.OjCollectorSchedulingConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
@Import({
        CommonTrainingDataConfig.class,
        CodeforcesTrainingDataConfig.class,
        AtcoderTrainingDataConfig.class,
        AtcoderProblemListBootstrapRunner.class,
        OjCollectorSchedulingConfig.class,
        AtcoderProblemListSchedulingConfig.class
})
public class TrainingDataModuleConfiguration {
    @Bean
    OjHandleAccountService ojHandleAccountService(OjHandleAccountRepository repository) {
        return new OjHandleAccountService(repository, Clock.system(ZoneOffset.ofHours(8)));
    }
}
