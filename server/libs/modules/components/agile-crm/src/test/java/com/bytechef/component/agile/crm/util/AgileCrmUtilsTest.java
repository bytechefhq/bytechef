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

package com.bytechef.component.agile.crm.util;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.ADDRESS;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.CITY;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.COMPANY;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.COUNTRY;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.EMAIL;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.FIRST_NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.LAST_NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.NAME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.PHONE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.STATE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.VALUE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.WEBSITE;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.ZIP_CODE;
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
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class AgileCrmUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void getPropertiesList() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(ADDRESS, "testAddress", COMPANY, "testCompany"));

        List<Map<String, Object>> result = AgileCrmUtils.getPropertiesList(mockedParameters);

        List<Map<String, Object>> expected = List.of(Map.of(NAME, ADDRESS, VALUE, "testAddress"),
            Map.of(NAME, COMPANY, VALUE, "testCompany"),
            Map.of(NAME, EMAIL, VALUE, ""),
            Map.of(NAME, FIRST_NAME, VALUE, ""),
            Map.of(NAME, LAST_NAME, VALUE, ""),
            Map.of(NAME, PHONE, VALUE, ""),
            Map.of(NAME, WEBSITE, VALUE, "", "subtype", "URL"),
            Map.of(NAME, CITY, VALUE, ""),
            Map.of(NAME, STATE, VALUE, ""),
            Map.of(NAME, COUNTRY, VALUE, ""),
            Map.of(NAME, ZIP_CODE, VALUE, ""));

        assertEquals(expected, result);
    }

    @Test
    void getUserIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("domainUser", Map.of("domain", "testDomain", "id", "testId")));

        List<Option<String>> result = AgileCrmUtils.getUserIdOptions(
            null, null, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("testDomain", "testId"));

        assertEquals(expected, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/users", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.XML, configuration.getResponseType());
    }

    @Test
    void getPipelineIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("name", "testName", "id", 123456L)));

        List<Option<Long>> result = AgileCrmUtils.getPipelineIdOptions(
            null, null, Map.of(), "", mockedContext);

        List<Option<Long>> expected = List.of(option("testName", 123456L));

        assertEquals(expected, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/milestone/pipelines", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void getMilestoneOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of("pipeline_id", 123456));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("name", "testName", "id", 123456L, "milestones", "lost,won")));

        List<Option<String>> result = AgileCrmUtils.getMilestoneOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(option("lost", "lost"), option("won", "won"));

        assertEquals(expected, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/milestone/pipelines", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}
