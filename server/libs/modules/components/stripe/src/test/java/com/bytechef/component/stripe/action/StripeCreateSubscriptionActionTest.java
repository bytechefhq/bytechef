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

package com.bytechef.component.stripe.action;

import static com.bytechef.component.stripe.constant.StripeConstants.COLLECTION_METHOD;
import static com.bytechef.component.stripe.constant.StripeConstants.COUPON_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.CUSTOMER_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.DAYS_UNTIL_DUE;
import static com.bytechef.component.stripe.constant.StripeConstants.DEFAULT_PAYMENT_METHOD;
import static com.bytechef.component.stripe.constant.StripeConstants.ITEMS;
import static com.bytechef.component.stripe.constant.StripeConstants.METADATA;
import static com.bytechef.component.stripe.constant.StripeConstants.PRICE_ID;
import static com.bytechef.component.stripe.constant.StripeConstants.QUANTITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
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
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class StripeCreateSubscriptionActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedParameters =
        MockParametersFactory.create(
            Map.of(CUSTOMER_ID, "xy", ITEMS, List.of(Map.of(PRICE_ID, "1", COUPON_ID, "2", QUANTITY, 3)),
                COLLECTION_METHOD, "send_invoice", DAYS_UNTIL_DUE, 2, DEFAULT_PAYMENT_METHOD, "abc",
                METADATA, Map.of("key", "value")));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Map<String, Object> responseMap = Map.of("key", "value");

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Object result = StripeCreateSubscriptionAction.perform(mockedParameters, null, mockedContext);

        assertEquals(responseMap, result);

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder =
            configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/subscriptions", stringArgumentCaptor.getValue());

        assertEquals(
            Body.of(
                Map.of(CUSTOMER_ID, "xy", COLLECTION_METHOD, "send_invoice", DAYS_UNTIL_DUE, 2,
                    DEFAULT_PAYMENT_METHOD, "abc", "items[0][discounts][0][coupon]", "2",
                    "items[0][price]", "1", "items[0][quantity]", 3,
                    "metadata[key]", "value"),
                BodyContentType.FORM_URL_ENCODED),
            bodyArgumentCaptor.getValue());
    }
}
