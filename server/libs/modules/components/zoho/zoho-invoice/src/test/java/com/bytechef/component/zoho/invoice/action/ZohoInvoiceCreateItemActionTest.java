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

import static com.bytechef.component.zoho.invoice.constant.ZohoInvoiceConstants.DESCRIPTION;
import static com.bytechef.component.zoho.invoice.constant.ZohoInvoiceConstants.NAME;
import static com.bytechef.component.zoho.invoice.constant.ZohoInvoiceConstants.PRODUCT_TYPE;
import static com.bytechef.component.zoho.invoice.constant.ZohoInvoiceConstants.RATE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class ZohoInvoiceCreateItemActionTest extends AbstractZohoInvoiceActionTest {

    @Test
    void testPerform() {
        Map<String, Object> parametersMap = Map.of(
            NAME, "name",
            RATE, 1.0,
            PRODUCT_TYPE, "goods",
            DESCRIPTION, "This is a test.");

        mockedParameters = MockParametersFactory.create(parametersMap);

        Object result = ZohoInvoiceCreateItemAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(parametersMap, body.getContent());
    }
}
