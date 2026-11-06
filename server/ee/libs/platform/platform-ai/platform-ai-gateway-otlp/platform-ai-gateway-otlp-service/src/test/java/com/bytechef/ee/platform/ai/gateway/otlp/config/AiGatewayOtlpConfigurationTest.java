/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.otlp.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.ai.gateway.otlp.mapper.OtlpProtobufMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiGatewayOtlpConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(AiGatewayOtlpConfiguration.class));

    @Test
    void testMapperBeanLoadsWhenEditionIsEeAndGatewayEnabled() {
        contextRunner
            .withPropertyValues("bytechef.edition=ee", "bytechef.ai.gateway.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(OtlpProtobufMapper.class));
    }

    @Test
    void testMapperBeanIsAbsentInCe() {
        contextRunner
            .withPropertyValues("bytechef.edition=ce", "bytechef.ai.gateway.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(OtlpProtobufMapper.class));
    }

    @Test
    void testMapperBeanIsAbsentWhenGatewayDisabled() {
        contextRunner
            .withPropertyValues("bytechef.edition=ee", "bytechef.ai.gateway.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OtlpProtobufMapper.class));
    }

    @Test
    void testMapperBeanIsAbsentWhenEditionPropertyMissing() {
        contextRunner
            .withPropertyValues("bytechef.ai.gateway.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(OtlpProtobufMapper.class));
    }
}
