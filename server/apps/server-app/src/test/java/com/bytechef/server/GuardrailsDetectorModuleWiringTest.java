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
