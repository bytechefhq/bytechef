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

package com.bytechef.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.mock.env.MockEnvironment;

/**
 * @author Ivica Cardic
 */
class ConfigTest {

    @Test
    void testReturnsValueForAllowedPrefix() throws Exception {
        MockEnvironment environment = new MockEnvironment();

        environment.setProperty("bytechef.workflow.config.allowed-prefixes", "app.");
        environment.setProperty("app.setting", "value");

        Config config = new Config(environment);

        assertThat(config.execute(null, null, "app.setting")
            .getValue()).isEqualTo("value");
    }

    @Test
    void testDeniesPropertyOutsideAllowlist() {
        MockEnvironment environment = new MockEnvironment();

        environment.setProperty("bytechef.workflow.config.allowed-prefixes", "app.");
        environment.setProperty("spring.datasource.password", "secret");

        Config config = new Config(environment);

        assertThatThrownBy(() -> config.execute(null, null, "spring.datasource.password"))
            .isInstanceOf(SpelEvaluationException.class);
    }

    @Test
    void testEmptyAllowlistDeniesEverything() {
        MockEnvironment environment = new MockEnvironment();

        environment.setProperty("app.setting", "value");

        Config config = new Config(environment);

        assertThatThrownBy(() -> config.execute(null, null, "app.setting"))
            .isInstanceOf(SpelEvaluationException.class);
    }
}
