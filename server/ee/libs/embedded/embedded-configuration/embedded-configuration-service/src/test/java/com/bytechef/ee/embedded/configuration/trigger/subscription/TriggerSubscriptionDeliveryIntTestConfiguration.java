/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import static org.mockito.Mockito.mock;

import com.bytechef.platform.external.invocation.ExternalInvocationRecorder;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import java.util.concurrent.Executor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;

/**
 * Supplies the webhook delivery lane, its executor and the invocation recorder to integration test contexts that
 * component-scan {@code com.bytechef.ee.embedded.configuration} without the modules that provide them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class TriggerSubscriptionDeliveryIntTestConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ExternalInvocationRecorder externalInvocationRecorder() {
        return mock(ExternalInvocationRecorder.class);
    }

    @Bean("webhookDeliveryExecutor")
    @ConditionalOnMissingBean(name = "webhookDeliveryExecutor")
    Executor webhookDeliveryExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean
    @ConditionalOnMissingBean
    WebhookDeliveryService webhookDeliveryService() {
        return mock(WebhookDeliveryService.class);
    }
}
