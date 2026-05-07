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

package com.bytechef.component.zoho.crm.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
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
 * @author Luka Ljubić
 */
@ExtendWith(MockContextSetupExtension.class)
class ZohoCrmUtilTest {

    private final List<Option<String>> expectedOptions = List.of(option("option", "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @BeforeEach
    void beforeEach(Http mockedHttp, Executor mockedExecutor) {
        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
    }

    @Test
    void testGetProfileOptions(
        ActionContext mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("profiles", List.of(Map.of("name", "option", "id", "123"))));

        assertEquals(
            expectedOptions,
            ZohoCrmUtils.getProfileOptions(
                null, null, null, null, mockedContext));
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/settings/profiles", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetRoleOptions(
        ActionContext mockedContext, Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("roles", List.of(Map.of("name", "option", "id", "123"))));

        assertEquals(
            expectedOptions,
            ZohoCrmUtils.getRoleOptions(
                null, null, null, null, mockedContext));
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(Http.ResponseType.JSON, configuration.getResponseType());
        assertEquals("/settings/roles", stringArgumentCaptor.getValue());
    }
}
