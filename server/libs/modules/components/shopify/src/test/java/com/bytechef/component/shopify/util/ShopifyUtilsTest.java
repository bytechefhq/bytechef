/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.shopify.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.SHOP_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class ShopifyUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetBaseUrl() {
        when(mockedParameters.getRequiredString(SHOP_NAME))
            .thenReturn("shopName");

        String expectedUrl = "https://shopName.myshopify.com/admin/api/2024-04";

        assertEquals(expectedUrl, ShopifyUtils.getBaseUrl(mockedParameters));
    }

    @Test
    void testGetOrderIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> orders = new ArrayList<>();
        Map<String, Object> orderMap = new LinkedHashMap<>();

        orderMap.put("name", "name");
        orderMap.put("id", 123123L);

        orders.add(orderMap);

        map.put("orders", orders);

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<Long>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("name", 123123L));

        assertEquals(
            expectedOptions,
            ShopifyUtils.getOrderIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testProductIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> products = new ArrayList<>();
        Map<String, Object> productMap = new LinkedHashMap<>();

        productMap.put("title", "title");
        productMap.put("id", 123123L);

        products.add(productMap);

        map.put("products", products);

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<Long>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("title", 123123L));

        assertEquals(
            expectedOptions,
            ShopifyUtils.getProductIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testVariantIdOptions() {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        List<Map<String, Object>> variants = new ArrayList<>();
        Map<String, Object> variantMap = new LinkedHashMap<>();

        variantMap.put("title", "title");
        variantMap.put("id", 123123L);

        variants.add(variantMap);

        map.put("variants", variants);

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(map);

        List<Option<Long>> expectedOptions = new ArrayList<>();

        expectedOptions.add(option("title", 123123L));

        assertEquals(
            expectedOptions,
            ShopifyUtils.getVariantIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));
    }

    @Test
    void testSubscribeWebhok() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("webhook", Map.of(ID, 123L)));

        assertEquals(123L,
            ShopifyUtils.subscribeWebhook(mockedParameters, "webhookUrl", mockedContext, "topic"));

        Http.Body body = bodyArgumentCaptor.getValue();

        Object content = body.getContent();

        assertEquals(Map.of(
            "webhook", Map.of(
                "topic", "topic",
                "address", "webhookUrl",
                "format", "json")), content);
    }

    @Test
    void testUnsubscribeWebhook() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        ShopifyUtils.unsubscribeWebhook(mockedParameters, mockedParameters, mockedContext);

        verify(mockedContext, times(1)).http(any());
        verify(mockedExecutor, times(1)).configuration(any());
        verify(mockedExecutor, times(1)).execute();
    }
}
