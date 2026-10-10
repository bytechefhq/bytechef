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

package com.bytechef.component.ai.agent.utils.cluster.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PrivateAddressGuardedToolCallbackTest {

    private final ToolCallback delegate = mock(ToolCallback.class);

    @Test
    void testCallRefusesLoopbackAddresses() {
        assertRefused("http://127.0.0.1:8080/actuator/env");
        assertRefused("http://localhost/admin");
    }

    @Test
    void testCallRefusesTheCloudMetadataAddress() {
        assertRefused("http://169.254.169.254/latest/meta-data/");
    }

    @Test
    void testCallRefusesPrivateNetworkAddresses() {
        assertRefused("http://10.0.0.5/");
        assertRefused("http://192.168.1.1/");
    }

    @Test
    void testCallRefusesNonHttpSchemes() {
        assertRefused("file:///etc/passwd");
    }

    @Test
    void testCallForwardsPublicAddresses() {
        String toolInput = "{\"url\": \"https://93.184.215.14/\", \"prompt\": \"summarize\"}";

        when(delegate.call(toolInput)).thenReturn("summary");

        assertThat(guard(Set.of()).call(toolInput)).isEqualTo("summary");
    }

    @Test
    void testCallForwardsPrivateHostsAllowedOnTheServer() {
        String toolInput = "{\"url\": \"http://localhost:8080/\", \"prompt\": \"summarize\"}";

        when(delegate.call(toolInput)).thenReturn("summary");

        assertThat(guard(Set.of("localhost")).call(toolInput)).isEqualTo("summary");
    }

    private void assertRefused(String url) {
        String result = guard(Set.of()).call("{\"url\": \"" + url + "\", \"prompt\": \"summarize\"}");

        assertThat(result).startsWith("Error:");

        verify(delegate, never()).call(anyString());
    }

    private PrivateAddressGuardedToolCallback guard(Set<String> allowedPrivateHosts) {
        return new PrivateAddressGuardedToolCallback(delegate, "url", allowedPrivateHosts);
    }
}
