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

package com.bytechef.platform.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class EnvironmentServiceTest {

    private final EnvironmentService environmentService = new EnvironmentServiceImpl();

    @Test
    public void testGetEnvironments() {
        List<Environment> environments = environmentService.getEnvironments();

        assertThat(environments).containsExactly(Environment.PRODUCTION);
    }

    @Test
    public void testGetEnvironmentByName() {
        assertThat(environmentService.getEnvironment("development")).isEqualTo(Environment.DEVELOPMENT);
        assertThat(environmentService.getEnvironment("STAGING")).isEqualTo(Environment.STAGING);
        assertThat(environmentService.getEnvironment("Production")).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    public void testGetEnvironmentById() {
        assertThat(environmentService.getEnvironment(0)).isEqualTo(Environment.DEVELOPMENT);
        assertThat(environmentService.getEnvironment(1)).isEqualTo(Environment.STAGING);
        assertThat(environmentService.getEnvironment(2)).isEqualTo(Environment.PRODUCTION);
    }

    @Test
    public void testInvalidNameThrows() {
        assertThatThrownBy(() -> environmentService.getEnvironment("invalid"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testInvalidIdThrows() {
        assertThatThrownBy(() -> environmentService.getEnvironment(-1))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> environmentService.getEnvironment(3))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
