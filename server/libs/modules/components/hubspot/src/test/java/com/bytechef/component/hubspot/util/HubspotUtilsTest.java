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

package com.bytechef.component.hubspot.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.hubspot.constant.HubspotConstants.EVENT_TYPE;
import static com.bytechef.component.hubspot.constant.HubspotConstants.HAPIKEY;
import static com.bytechef.component.hubspot.constant.HubspotConstants.ID;
import static com.bytechef.component.hubspot.constant.HubspotConstants.LABEL;
import static com.bytechef.component.hubspot.constant.HubspotConstants.RESULTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
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
class HubspotUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private Parameters mockedParameters = mock(Parameters.class);
    private final Response mockedResponse = mock(Response.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testExtractFirstContentMap() {
        List<Map<String, Object>> contentList = List.of(Map.of("key1", "value1"), Map.of("key2", "value2"));

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(contentList);

        Map<String, Object> result = HubspotUtils.extractFirstContentMap(mockedWebhookBody);

        assertEquals("value1", result.get("key1"));
    }

    @Test
    void testGetContactIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> propertiesMap = Map.of("firstname", "first", "lastname", "last");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", "properties", propertiesMap))));

        List<Option<String>> expectedOptions = List.of(option("first last", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getContactIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/crm/v3/objects/contacts", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetDealstageOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of("properties", Map.of("pipeline", "123")));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", LABEL, "label"))));

        List<Option<String>> expectedOptions = List.of(option("label", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getDealstageOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/crm/v3/pipelines/deals/123/stages", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetHubspotOwnerIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", "email", "label"))));

        List<Option<String>> expectedOptions = List.of(option("label", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getHubspotOwnerIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/crm/v3/owners", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetPipelineOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", LABEL, "label"))));

        List<Option<String>> expectedOptions = List.of(option("label", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getPipelineOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/crm/v3/pipelines/deals", stringArgumentCaptor.getValue());
    }

    @Test
    void testGetTicketIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, Object> propertiesMap = Map.of("subject", "ticket");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(RESULTS, List.of(Map.of(ID, "123", "properties", propertiesMap))));

        List<Option<String>> expectedOptions = List.of(option("ticket", "123"));

        assertEquals(
            expectedOptions,
            HubspotUtils.getTicketIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/crm/v3/objects/tickets", stringArgumentCaptor.getValue());
    }

    @Test
    void testSubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "abc"));

        String result = HubspotUtils.subscribeWebhook(
            "eventType", "appId", "hubspot key", "webhookUrl", mockedTriggerContext);

        assertEquals("abc", result);

        assertEquals(List.of(HAPIKEY, "hubspot key", HAPIKEY, "hubspot key"), stringArgumentCaptor.getAllValues());

        List<Object> contents = bodyArgumentCaptor.getAllValues()
            .stream()
            .map(Http.Body::getContent)
            .toList();

        assertEquals(List.of(
            Map.of("throttling", Map.of(
                "period", "SECONDLY",
                "maxConcurrentRequests", 10),
                "targetUrl", "webhookUrl"),
            Map.of(EVENT_TYPE, "eventType", "active", true)),
            contents);
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        HubspotUtils.unsubscribeWebhook("appId", "subscriptionId", "hubspot key", mockedTriggerContext);

        assertEquals(List.of(HAPIKEY, "hubspot key"), stringArgumentCaptor.getAllValues());
    }
}
