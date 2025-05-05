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

package com.bytechef.component.zoho.invoice.action;

import static com.bytechef.component.zoho.commons.ZohoConstants.CUSTOMER_ID;
import static com.bytechef.component.zoho.commons.ZohoConstants.DATE;
import static com.bytechef.component.zoho.commons.ZohoConstants.INVOICE_NUMBER;
import static com.bytechef.component.zoho.commons.ZohoConstants.LINE_ITEMS;
import static com.bytechef.component.zoho.commons.ZohoConstants.PAYMENT_TERMS;
import static com.bytechef.component.zoho.commons.ZohoConstants.USE_CUSTOM_INVOICE_NUMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class ZohoInvoiceCreateInvoiceActionTest extends AbstractZohoInvoiceActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        mockedParameters = MockParametersFactory.create(
            Map.of(
                CUSTOMER_ID, "1",
                USE_CUSTOM_INVOICE_NUMBER, "true",
                INVOICE_NUMBER, "1",
                LINE_ITEMS, List.of(Map.of("item_id", "1", " quantity", 1)),
                DATE, "2025-04-29",
                PAYMENT_TERMS, 15));
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Object result = ZohoInvoiceCreateInvoiceAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);
        assertEquals(List.of("ignore_auto_number_generation", "true"), stringArgumentCaptor.getAllValues());

        Map<String, Object> expectedBodyMap = Map.of(
            CUSTOMER_ID, "1", INVOICE_NUMBER, "1",
            LINE_ITEMS, List.of(Map.of("item_id", "1", " quantity", 1)),
            DATE, "2025-04-29", PAYMENT_TERMS, 15);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(expectedBodyMap, body.getContent());
    }
}
