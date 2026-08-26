/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Pins {@link SecurityConfiguration}'s {@code @EnableMethodSecurity(proxyTargetClass = true)} attribute directly, by
 * reflection, on the annotation itself. This is deliberately separate from
 * {@code WorkflowTestApiControllerProductionProxyModeTest} in {@code platform-workflow-test-rest}: that test reproduces
 * {@code AopAutoConfiguration}'s ambient {@code spring.aop.proxy-target-class=true} default inside its own
 * hand-written, local {@code @EnableMethodSecurity} configuration and never references {@link SecurityConfiguration} at
 * all -- it would keep passing even if this class's {@code proxyTargetClass} attribute were removed entirely, because
 * the ambient autoconfiguration default alone is enough to make that other test's proxy class-based. This test is the
 * one that actually regresses if {@code proxyTargetClass} is ever removed or flipped to {@code false} here.
 *
 * @author Ivica Cardic
 */
class SecurityConfigurationMethodSecurityTest {

    @Test
    void testEnableMethodSecurityPinsProxyTargetClass() {
        EnableMethodSecurity annotation = SecurityConfiguration.class.getAnnotation(EnableMethodSecurity.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.proxyTargetClass()).isTrue();
    }
}
