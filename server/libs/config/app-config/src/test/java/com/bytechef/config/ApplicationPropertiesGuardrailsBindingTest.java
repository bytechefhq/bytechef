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

package com.bytechef.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * Guards the strict-binding contract for the OpenNLP guardrails properties. `ApplicationProperties` binds the whole
 * `bytechef` tree with `ignoreUnknownFields = false`, so a key that is PRESENT in configuration but has no matching
 * field fails the application context — for every app, not just the one that owns the feature. The OpenNLP detector
 * lives in an optional module and is inert until an operator sets `enabled`, so its keys are present-by-construction in
 * deployments whose apps may not carry the module at all.
 */
class ApplicationPropertiesGuardrailsBindingTest {

    @Test
    void testOpenNlpGuardrailsKeysBindWithoutUnboundElements() {
        ConfigurationPropertySource source = new MapConfigurationPropertySource(
            Map.of(
                "bytechef.ai.guardrails.opennlp.enabled", "true",
                "bytechef.ai.guardrails.opennlp.min-confidence", "0.9",
                "bytechef.ai.guardrails.opennlp.tokenizer-model", "classpath:tokenizer.bin",
                "bytechef.ai.guardrails.opennlp.entity-models.PERSON", "classpath:person.bin"));

        Binder binder = new Binder(source);

        assertThatCode(
            () -> binder.bind(
                "bytechef", Bindable.of(ApplicationProperties.class),
                new NoUnboundElementsBindHandler(BindHandler.DEFAULT)))
                    .doesNotThrowAnyException();
    }
}
