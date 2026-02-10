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

package com.bytechef.component.shopify.action;

import static com.bytechef.component.shopify.constant.ShopifyConstants.NOTIFY_CUSTOMER;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORDER_ID;
import static com.bytechef.component.shopify.constant.ShopifyConstants.ORIGINAL_PAYMENT_METHODS_REFUND;
import static com.bytechef.component.shopify.constant.ShopifyConstants.REASON;
import static com.bytechef.component.shopify.constant.ShopifyConstants.REFUND_METHOD;
import static com.bytechef.component.shopify.constant.ShopifyConstants.RESTOCK;
import static com.bytechef.component.shopify.constant.ShopifyConstants.STAFF_NOTE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.shopify.util.ShopifyUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class ShopifyCancelOrderActionTest extends AbstractShopifyActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            NOTIFY_CUSTOMER, false,
            ORDER_ID, "orderId",
            REASON, "reason",
            ORIGINAL_PAYMENT_METHODS_REFUND, false,
            RESTOCK, false,
            STAFF_NOTE, "staff note"));

    @Test
    void testPerform() {

        shopifyUtilsMockedStatic
            .when(() -> ShopifyUtils.executeGraphQlOperation(
                stringArgumentCaptor.capture(),
                contextArgumentCaptor.capture(),
                mapArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
            .thenReturn(Map.of());

        Object result = ShopifyCancelOrderAction.perform(
            mockedParameters, null, mockedContext);

        assertEquals(Map.of(), result);

        String expectedQuery = """
            mutation OrderCancel(
              $orderId: ID!
              $notifyCustomer: Boolean
              $refundMethod: OrderCancelRefundMethodInput!
              $restock: Boolean!
              $reason: OrderCancelReason!
              $staffNote: String
            ) {
              orderCancel(
                orderId: $orderId
                notifyCustomer: $notifyCustomer
                refundMethod: $refundMethod
                restock: $restock
                reason: $reason
                staffNote: $staffNote
              ) {
                job {
                  id
                  done
                }
                orderCancelUserErrors {
                  field
                  message
                  code
                }
                userErrors {
                  field
                  message
                }
              }
            }""";

        Map<String, Object> expectedVariables = Map.of(
            NOTIFY_CUSTOMER, false,
            ORDER_ID, "orderId",
            REASON, "reason",
            REFUND_METHOD, Map.of(ORIGINAL_PAYMENT_METHODS_REFUND, false),
            RESTOCK, false,
            STAFF_NOTE, "staff note");

        assertEquals(List.of(expectedQuery, "orderCancel"), stringArgumentCaptor.getAllValues());
        assertEquals(expectedVariables, mapArgumentCaptor.getValue());
        assertEquals(mockedContext, contextArgumentCaptor.getValue());
    }
}
