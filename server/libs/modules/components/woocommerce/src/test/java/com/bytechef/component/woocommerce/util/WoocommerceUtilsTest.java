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

package com.bytechef.component.woocommerce.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class WoocommerceUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void createWebhook() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);

        String webhookUrl = "https://example.com/webhook";
        String topic = "order.created";

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, 123));

        WebhookEnableOutput result = WoocommerceUtils.createWebhook(webhookUrl, mockedTriggerContext, topic);

        assertEquals(new WebhookEnableOutput(Map.of(ID, 123), null), result);
        Body body = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of("delivery_url", webhookUrl, "name", "New Webhook", "topic", topic), body.getContent());
    }

    @Test
    void testGetCategoryIdOptions() {
        List<Map<String, Object>> mockResponse = List.of(
            Map.of("name", "One", "id", "1"),
            Map.of("name", "Two", "id", "2"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = WoocommerceUtils.getCategoryIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("One", "1"),
            option("Two", "2"));

        assertEquals(expected, result);
    }

    @Test
    void testGetCustomerIdOptions() {
        List<Map<String, Object>> mockResponse = List.of(
            Map.of("username", "One", "id", "1"),
            Map.of("username", "Two", "id", "2"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = WoocommerceUtils.getCustomerIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("One", "1"),
            option("Two", "2"));

        assertEquals(expected, result);
    }

    @Test
    void testGetPaymentIdOptions() {
        List<Map<String, Object>> mockResponse = List.of(
            Map.of("title", "One", "id", "1"),
            Map.of("title", "Two", "id", "2"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = WoocommerceUtils.getPaymentIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("One", "1"),
            option("Two", "2"));

        assertEquals(expected, result);
    }

    @Test
    void testGetProductIdOptions() {
        List<Map<String, Object>> mockResponse = List.of(
            Map.of("name", "One", "id", "1"),
            Map.of("name", "Two", "id", "2"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = WoocommerceUtils.getProductIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("One", "1"),
            option("Two", "2"));

        assertEquals(expected, result);
    }

    @Test
    void testGetTagIdOptions() {
        List<Map<String, Object>> mockResponse = List.of(
            Map.of("name", "One", "id", "1"),
            Map.of("name", "Two", "id", "2"));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockResponse);

        List<Option<String>> result = WoocommerceUtils.getTagIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expected = List.of(
            option("One", "1"),
            option("Two", "2"));

        assertEquals(expected, result);
    }
}
