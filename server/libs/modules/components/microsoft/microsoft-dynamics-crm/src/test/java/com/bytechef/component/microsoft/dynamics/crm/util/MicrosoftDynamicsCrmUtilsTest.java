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

package com.bytechef.component.microsoft.dynamics.crm.util;

import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.ENTITY_TYPE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftDynamicsCrmUtilsTest {

    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testGetEntityTypeOptions(
        Context mockedContext, Http mockedHttp, Http.Executor mockedExecutor, Http.Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "value",
                List.of(Map.of("EntitySetName", "Accounts"), Map.of("EntitySetName", "Contacts"))));

        List<Option<String>> options = MicrosoftDynamicsCrmUtils.getEntityTypeOptions(
            null, null, Map.of(), null, mockedContext);

        assertEquals(List.of(option("Accounts", "Accounts"), option("Contacts", "Contacts")), options);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals("/EntityDefinitions", stringArgumentCaptor.getValue());

        Object[] objects = {
            "$select", "EntitySetName,LogicalName"
        };

        assertArrayEquals(objects, objectsArgumentCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetRecordIdOptions(
        Context mockedContext, Http mockedHttp, Http.Executor mockedExecutor, Http.Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters inputParameters = MockParametersFactory.create(Map.of(ENTITY_TYPE, "account"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "value",
                    List.of(Map.of("PrimaryIdAttribute", "accountid", "PrimaryNameAttribute", "name"))))
            .thenReturn(
                Map.of(
                    "value",
                    List.of(
                        Map.of("accountid", "id1", "name", "Account 1"),
                        Map.of("accountid", "id2", "name", "Account 2"))));

        List<Option<String>> options = MicrosoftDynamicsCrmUtils.getRecordIdOptions(
            inputParameters, null, Map.of(), null, mockedContext);

        assertEquals(List.of(option("Account 1", "id1"), option("Account 2", "id2")), options);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("/EntityDefinitions", "/account"), stringArgumentCaptor.getAllValues());

        List<Object[]> allQueryParams = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allQueryParams.size());

        Object[] queryParameters1 = {
            "$select", "PrimaryIdAttribute,PrimaryNameAttribute,LogicalName",
            "$filter", "EntitySetName eq 'account'"
        };

        Object[] queryParameters2 = {
            "$select", "name"
        };

        assertArrayEquals(queryParameters1, allQueryParams.get(0));
        assertArrayEquals(queryParameters2, allQueryParams.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetEntityFieldsProperties(
        ActionContext mockedActionContext, Http mockedHttp, Http.Executor mockedExecutor, Http.Response mockedResponse,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) throws Exception {

        Parameters inputParameters = MockParametersFactory.create(Map.of(ENTITY_TYPE, "account"));

        Map<String, Object> attributesBody = Map.of(
            "value", List.of(
                Map.of(
                    "LogicalName", "name",
                    "AttributeType", "String",
                    "DisplayName", Map.of("UserLocalizedLabel", Map.of("Label", "Account Name")),
                    "Description", Map.of("UserLocalizedLabel", Map.of("Label", "Name of the account")),
                    "IsPrimaryName", true,
                    "IsValidForCreate", true),
                Map.of(
                    "LogicalName", "revenue",
                    "AttributeType", "Double",
                    "DisplayName", Map.of("UserLocalizedLabel", Map.of("Label", "Annual Revenue")),
                    "Description", Map.of("UserLocalizedLabel", Map.of("Label", "Annual revenue for the account")),
                    "IsPrimaryName", false,
                    "IsValidForCreate", true),
                Map.of(
                    "LogicalName", "unsupported",
                    "AttributeType", "Lookup",
                    "IsValidForCreate", true)));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("value", List.of(Map.of("LogicalName", "account"))))
            .thenReturn(attributesBody);

        PropertiesFunction propertiesFunction = MicrosoftDynamicsCrmUtils.getEntityFieldsProperties(true);

        List<? extends ValueProperty<?>> properties =
            propertiesFunction.apply(inputParameters, null, Map.of(), mockedActionContext);

        assertEquals(
            List.of(
                string("name")
                    .label("Account Name")
                    .description("Name of the account")
                    .required(true),
                number("revenue")
                    .label("Annual Revenue")
                    .description("Annual revenue for the account")
                    .required(false)),
            properties);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Http.Configuration configuration = configurationBuilder.build();

        Http.ResponseType responseType = configuration.getResponseType();

        assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("/EntityDefinitions", "/EntityDefinitions(LogicalName='account')/Attributes"),
            stringArgumentCaptor.getAllValues());

        List<Object[]> allQueryParams = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allQueryParams.size());

        Object[] queryParameters1 = {
            "$select", "PrimaryIdAttribute,PrimaryNameAttribute,LogicalName",
            "$filter", "EntitySetName eq 'account'"
        };

        Object[] queryParameters2 = {
            "$select", "AttributeType,LogicalName,Description,DisplayName,IsPrimaryName,IsValidForCreate"
        };

        assertArrayEquals(queryParameters1, allQueryParams.get(0));
        assertArrayEquals(queryParameters2, allQueryParams.get(1));
    }
}
