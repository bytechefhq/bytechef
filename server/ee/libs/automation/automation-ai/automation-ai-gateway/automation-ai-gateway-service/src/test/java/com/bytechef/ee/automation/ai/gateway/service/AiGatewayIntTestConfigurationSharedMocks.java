/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityNotificationDispatcher;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.scheduler.AlertScheduler;
import com.bytechef.platform.scheduler.ExportScheduler;
import com.bytechef.platform.tag.service.TagService;
import io.micrometer.core.instrument.MeterRegistry;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Shared mock beans for AI Gateway integration tests.
 *
 * @version ee
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {
    AiGatewayChatModelFactory.class, AiGatewayEmbeddingModelFactory.class, AiGatewayMetrics.class,
    AiObservabilityNotificationDispatcher.class, AlertScheduler.class, ExportScheduler.class,
    FileStorageService.class, MeterRegistry.class, PropertyService.class, TagService.class
})
public @interface AiGatewayIntTestConfigurationSharedMocks {
}
