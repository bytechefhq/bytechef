/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

/**
 * Pins that every {@code security.scope.*} {@link PermissionScopeProvider} is gated behind
 * {@code @ConditionalOnEEVersion}: none of them may register in a CE process. These beans are compiled into the single
 * shared server-app artifact used by both editions, so the annotation on the class itself is what keeps a CE context
 * from populating {@code PermissionScopeRegistry} with EE-only scopes such as {@code AI_GATEWAY_VIEW} and the
 * {@code MCP_*} family.
 *
 * <p>
 * The assertion is on the {@link PermissionScopeProvider} bean type collectively, discovered by scanning the
 * {@code security.scope} package rather than by registering a hardcoded list of class names, so a future provider that
 * forgets the annotation fails this test: it would be scanned into the context alongside the existing providers and,
 * unlike them, survive into the CE/absent-edition assertions.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PermissionScopeProviderEditionGatingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(PermissionScopeProviderScanConfiguration.class);

    @Test
    void testCeEditionRegistersNoPermissionScopeProviders() {
        contextRunner.withPropertyValues("bytechef.edition=ce")
            .run(context -> assertThat(context.getBeansOfType(PermissionScopeProvider.class)).isEmpty());
    }

    @Test
    void testAbsentEditionPropertyRegistersNoPermissionScopeProviders() {
        contextRunner.run(context -> assertThat(context.getBeansOfType(PermissionScopeProvider.class)).isEmpty());
    }

    @Test
    void testEeEditionRegistersEveryPermissionScopeProvider() {
        contextRunner.withPropertyValues("bytechef.edition=ee")
            .run(context -> assertThat(context.getBeansOfType(PermissionScopeProvider.class)).isNotEmpty());
    }

    @ComponentScan("com.bytechef.ee.automation.configuration.security.scope")
    private static class PermissionScopeProviderScanConfiguration {
    }
}
