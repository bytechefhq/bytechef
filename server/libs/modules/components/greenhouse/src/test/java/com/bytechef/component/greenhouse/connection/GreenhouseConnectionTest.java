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

package com.bytechef.component.greenhouse.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApplyFunction;
import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ConnectionDefinition.BaseUriFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Encoder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class GreenhouseConnectionTest {

    private final Parameters connectionParameters = MockParametersFactory.create(Map.of(USERNAME, "apiKey"));

    @Test
    void testApply() throws Exception {
        Context mockedContext = mock(Context.class);
        Encoder mockedEncoder = mock(Encoder.class);

        when(mockedEncoder.base64Encode(any(byte[].class)))
            .thenAnswer(invocation -> Base64.getEncoder()
                .encodeToString(invocation.getArgument(0)));
        when(mockedContext.encoder(any()))
            .thenAnswer(invocation -> {
                ContextFunction<Encoder, String> encoderFunction = invocation.getArgument(0);

                return encoderFunction.apply(mockedEncoder);
            });

        List<? extends Authorization> authorizations = GreenhouseConnection.CONNECTION_DEFINITION.getAuthorizations();

        Authorization authorization = authorizations.getFirst();

        ApplyFunction applyFunction = authorization.getApply()
            .orElseThrow();

        ApplyResponse applyResponse = applyFunction.apply(connectionParameters, mockedContext);

        String expectedCredentials = Base64.getEncoder()
            .encodeToString("apiKey:".getBytes(StandardCharsets.UTF_8));

        assertEquals(
            Map.of(AUTHORIZATION, List.of("Basic " + expectedCredentials)), applyResponse.getHeaders());
    }

    @Test
    void testBaseUri() {
        BaseUriFunction baseUriFunction = GreenhouseConnection.CONNECTION_DEFINITION.getBaseUri()
            .orElseThrow();

        assertEquals("https://harvest.greenhouse.io/v1", baseUriFunction.apply(connectionParameters, null));
    }
}
