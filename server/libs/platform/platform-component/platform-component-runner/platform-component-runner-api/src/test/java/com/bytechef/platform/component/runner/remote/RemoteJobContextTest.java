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

package com.bytechef.platform.component.runner.remote;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class RemoteJobContextTest {

    @Test
    public void testToStringShowsOnlyNamesAndKeysNeverSecretValues() {
        RemoteJobContext context = new RemoteJobContext(
            new ComponentConnection("kubernetes", 1, 7L, Map.of("token", "connection-secret"), null),
            Map.of("apiKey", "operator-secret"), "tcp://docker:2376",
            ParametersFactory.create(Map.of("credentials", Map.of("password", "registry-secret"))), 1024L,
            Duration.ofMinutes(5));

        String text = context.toString();

        assertThat(text).contains("kubernetes/v1")
            .contains("apiKey")
            .contains("tcp://docker:2376")
            .contains("credentials")
            .contains("1024")
            .contains("PT5M")
            .doesNotContain("connection-secret")
            .doesNotContain("operator-secret")
            .doesNotContain("registry-secret")
            .doesNotContain("password");
    }

    @Test
    public void testToStringWithoutAConnection() {
        RemoteJobContext context = new RemoteJobContext(
            null, Map.of(), null, ParametersFactory.create(Map.of()), 1L, Duration.ofSeconds(1));

        assertThat(context.toString()).contains("connection=none");
    }
}
