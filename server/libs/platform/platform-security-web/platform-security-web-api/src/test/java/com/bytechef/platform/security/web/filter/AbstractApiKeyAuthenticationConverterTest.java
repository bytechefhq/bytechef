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

package com.bytechef.platform.security.web.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.tenant.domain.TenantKey;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @author Ivica Cardic
 */
class AbstractApiKeyAuthenticationConverterTest {

    private final AtomicReference<String> convertedTenantId = new AtomicReference<>();

    private final AbstractApiKeyAuthenticationConverter apiKeyAuthenticationConverter =
        new AbstractApiKeyAuthenticationConverter(() -> List.of(Environment.values())) {

            @Override
            @Nullable
            protected Authentication doConvert(int environment, String authToken, String tenantId) {
                convertedTenantId.set(tenantId);

                return null;
            }
        };

    @Test
    void testConvertPassesTheTenantOfTheApiKey() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer " + TenantKey.of("acme"));

        apiKeyAuthenticationConverter.convert(mockHttpServletRequest);

        assertThat(convertedTenantId.get()).isEqualTo("acme");
    }

    @Test
    void testConvertRejectsABearerTokenThatIsNotAnApiKey() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

        mockHttpServletRequest.addHeader("Authorization", "Bearer eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ4In0.c2ln");

        assertThatExceptionOfType(BadCredentialsException.class)
            .isThrownBy(() -> apiKeyAuthenticationConverter.convert(mockHttpServletRequest));

        assertThat(convertedTenantId.get()).isNull();
    }
}
