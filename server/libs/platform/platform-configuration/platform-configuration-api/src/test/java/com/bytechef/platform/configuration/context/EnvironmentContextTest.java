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

package com.bytechef.platform.configuration.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EnvironmentContextTest {

    @AfterEach
    void tearDown() {
        EnvironmentContext.clear();
    }

    @Test
    void testDefaultsToProductionWhenUnset() {
        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    void testReturnsSetEnvironment() {
        EnvironmentContext.set(Environment.STAGING);

        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.STAGING);
    }

    @Test
    void testSetByOrdinal() {
        EnvironmentContext.set(0);

        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.DEVELOPMENT);
    }

    @Test
    void testSetByOrdinalThrowsOnInvalidOrdinal() {
        assertThatThrownBy(() -> EnvironmentContext.set(Environment.values().length))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testClearResetsToDefault() {
        EnvironmentContext.set(Environment.DEVELOPMENT);
        EnvironmentContext.clear();

        assertThat(EnvironmentContext.getCurrentEnvironment()).isEqualTo(Environment.PRODUCTION);
    }
}
