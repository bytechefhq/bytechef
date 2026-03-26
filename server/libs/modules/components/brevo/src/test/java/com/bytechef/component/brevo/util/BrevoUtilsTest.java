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

package com.bytechef.component.brevo.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class BrevoUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(
        option("contact1@test.com", "contact1@test.com"),
        option("contact2@test.com", "contact2@test.com"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach(Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }

    @Test
    void testGetContactsOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "contacts", List.of(
                    Map.of("email", "contact1@test.com"),
                    Map.of("email", "contact2@test.com"))));

        List<Option<String>> result = BrevoUtils.getContactsOptions(
            null, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/contacts", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetSendersOptions(
        Context mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "senders", List.of(
                    Map.of("email", "contact1@test.com"),
                    Map.of("email", "contact2@test.com"))));

        List<Option<String>> result = BrevoUtils.getSendersOptions(
            null, null, null, null, mockedContext);

        assertEquals(expectedOptions, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/senders", stringArgumentCaptor.getValue());
    }
}
