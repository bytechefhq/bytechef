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

package com.bytechef.server;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * Guards that the optional OpenNLP guardrails detector module is actually on this application's classpath.
 *
 * <p>
 * This exists because it once was not. The module was included in {@code settings.gradle.kts}, so it compiled and its
 * own tests ran under {@code check}, but no application declared it as a dependency — meaning
 * {@code OpenNlpGuardrailsConfiguration} could never be instantiated in any shipped artifact. An operator who supplied
 * models and set {@code bytechef.ai.guardrails.opennlp.enabled=true} would have got silence, because
 * {@code @ConditionalOnProperty} on an absent class produces no error. Every task-level review passed; only a
 * whole-branch review caught it.
 * </p>
 *
 * <p>
 * <b>What this proves, and what it does not.</b> It proves the module ships with this application. It does NOT prove
 * that Spring registers the detector bean — the module's own {@code OpenNlpGuardrailsConfigurationTest} covers the
 * registration conditions with an {@code ApplicationContextRunner}, but that test loads the configuration class by
 * direct reference and so passes whether or not any application depends on the module. This test is the half that one
 * cannot reach.
 * </p>
 *
 * <p>
 * Deliberately a plain classpath assertion rather than a {@code @SpringBootTest}: proving the bean registers for real
 * would require a valid model file, and the module ships none by design (Apache distributes no NER models). Training
 * one in-memory here would pull {@code opennlp-tools} onto this application's test classpath for a single assertion.
 * </p>
 *
 * @author Ivica Cardic
 */
class GuardrailsDetectorModuleWiringTest {

    private static final String OPEN_NLP_CONFIGURATION_CLASS =
        "com.bytechef.ee.platform.ai.guardrails.opennlp.OpenNlpGuardrailsConfiguration";

    @Test
    void testOpenNlpGuardrailsModuleIsOnTheClasspath() {
        assertThatCode(() -> Class.forName(OPEN_NLP_CONFIGURATION_CLASS))
            .as(
                "%s must be on this application's classpath, otherwise the OpenNLP detector can never register and " +
                    "enabling it in configuration silently does nothing",
                OPEN_NLP_CONFIGURATION_CLASS)
            .doesNotThrowAnyException();
    }
}
