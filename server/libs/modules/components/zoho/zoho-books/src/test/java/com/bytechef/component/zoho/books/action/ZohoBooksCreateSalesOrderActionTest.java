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

package com.bytechef.component.zoho.books.action;

import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.CURRENCY_ID;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.CUSTOMER_ID;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.DATE;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.LINE_ITEMS;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.PAYMENT_TERMS;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.SALES_ORDER_NUMBER;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.SHIPMENT_DATE;
import static com.bytechef.component.zoho.books.constant.ZohoBooksConstants.USE_CUSTOM_SALES_ORDER_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class ZohoBooksCreateSalesOrderActionTest extends AbstractZohoBooksActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        mockedParameters = MockParametersFactory.create(
            Map.of(
                CUSTOMER_ID, "1", USE_CUSTOM_SALES_ORDER_NUMBER, "true", SALES_ORDER_NUMBER, "1",
                LINE_ITEMS, List.of(Map.of("item_id", "1", " quantity", 1)), CURRENCY_ID, "euro",
                DATE, "2025-04-29", SHIPMENT_DATE, "2025-05-29", PAYMENT_TERMS, 15));

        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Object result = ZohoBooksCreateSalesOrderAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);
        assertEquals(List.of("ignore_auto_number_generation", "true"), stringArgumentCaptor.getAllValues());

        Map<String, Object> expectedBodyMap = Map.of(
            CUSTOMER_ID, "1", SALES_ORDER_NUMBER, "1",
            LINE_ITEMS, List.of(Map.of("item_id", "1", " quantity", 1)), CURRENCY_ID, "euro",
            DATE, "2025-04-29", SHIPMENT_DATE, "2025-05-29", PAYMENT_TERMS, 15);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(expectedBodyMap, body.getContent());
    }
}
