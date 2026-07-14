package com.ojtraining.manager.api;

import com.ojtraining.manager.trainingdata.atcoder.config.AtcoderProblemListBootstrapRunner;
import com.ojtraining.manager.trainingdata.atcoder.config.AtcoderTrainingDataConfig;
import com.ojtraining.manager.trainingdata.codeforces.config.CodeforcesTrainingDataConfig;
import com.ojtraining.manager.trainingdata.common.config.CommonTrainingDataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "app.operation-password=test-operation-password",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:training-context;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "platform.training-data.atcoder.problem-list-collector.bootstrap-on-startup=false"
})
class TrainingDataModuleContextTest {
    @Autowired
    private ApplicationContext context;

    @Test
    void composesBothOjModulesAndAtcoderBootstrapRunner() {
        assertNotNull(context.getBean(CommonTrainingDataConfig.class));
        assertNotNull(context.getBean(CodeforcesTrainingDataConfig.class));
        assertNotNull(context.getBean(AtcoderTrainingDataConfig.class));
        assertNotNull(context.getBean(AtcoderProblemListBootstrapRunner.class));
    }
}
