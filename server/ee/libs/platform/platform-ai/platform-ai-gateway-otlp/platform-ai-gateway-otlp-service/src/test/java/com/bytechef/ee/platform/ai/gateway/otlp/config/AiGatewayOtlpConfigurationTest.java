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
 * Pins the CE / EE conditional gating on {@link AiGatewayOtlpConfiguration}.
 *
 * <p>
 * The configuration carries {@code @ConditionalOnEEVersion} (which expands to
 * {@code @ConditionalOnProperty(name = "bytechef.edition", havingValue = "ee")}) AND
 * {@code @ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")}. A regression
 * dropping either annotation would either:
 * <ul>
 * <li>Load the OTLP mapper bean in CE deployments — wiring the gateway's protobuf surface into a build that does not
 * advertise the AI gateway, breaking dependency-direction guarantees.</li>
 * <li>Load the bean even when the gateway is intentionally disabled (e.g., a worker app variant) — wasting startup time
 * and forcing transitive dependencies into apps that do not need them.</li>
 * </ul>
 * Both regressions are silent at compile time. This test catches them at startup-config time.
 *
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
        // CE edition: the @ConditionalOnEEVersion guard must keep the bean out of the context regardless of
        // the gateway-enabled flag value. Asserts the bean is missing even when "enabled=true" is also set —
        // a regression replacing @ConditionalOnEEVersion with a plain @ConditionalOnProperty on enabled would
        // pass without this combined-flag check.
        contextRunner
            .withPropertyValues("bytechef.edition=ce", "bytechef.ai.gateway.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(OtlpProtobufMapper.class));
    }

    @Test
    void testMapperBeanIsAbsentWhenGatewayDisabled() {
        // EE edition with gateway disabled: the second conditional must also gate the bean out. Catches a
        // regression that drops @ConditionalOnProperty(... enabled ...) — without it, EE deployments where
        // the operator intentionally disabled the gateway (canary, dark launch) would still wire the mapper.
        contextRunner
            .withPropertyValues("bytechef.edition=ee", "bytechef.ai.gateway.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(OtlpProtobufMapper.class));
    }

    @Test
    void testMapperBeanIsAbsentWhenEditionPropertyMissing() {
        // No bytechef.edition property at all: defends against a deployment where the property file ships
        // without the edition key. @ConditionalOnEEVersion's missing-value semantics must be "absent", not
        // "default to ee".
        contextRunner
            .withPropertyValues("bytechef.ai.gateway.enabled=true")
            .run(context -> assertThat(context).doesNotHaveBean(OtlpProtobufMapper.class));
    }
}
