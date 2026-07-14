package com.ojtraining.manager.api.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record OperationPasswordProperties(
        @NotBlank(message = "app.operation-password must be configured") String operationPassword
) {
}
