/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.config;

import com.bytechef.ee.platform.ai.gateway.otlp.mapper.OtlpProtobufMapper;
import com.bytechef.ee.platform.ai.gateway.otlp.mapper.OtlpProtobufMapperImpl;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * @author Ivica Cardic
 * @version ee
 */
@AutoConfiguration
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayOtlpConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OtlpProtobufMapper otlpProtobufMapper(ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new OtlpProtobufMapperImpl(meterRegistryProvider.getIfAvailable());
    }
}
