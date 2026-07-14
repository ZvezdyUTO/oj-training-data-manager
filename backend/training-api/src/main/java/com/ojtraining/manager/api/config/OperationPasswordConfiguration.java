package com.ojtraining.manager.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OperationPasswordProperties.class)
public class OperationPasswordConfiguration {
}
