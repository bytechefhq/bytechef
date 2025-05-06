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

import static com.bytechef.component.zoho.commons.ZohoConstants.BILLING_ADDRESS;
import static com.bytechef.component.zoho.commons.ZohoConstants.COMPANY_NAME;
import static com.bytechef.component.zoho.commons.ZohoConstants.CONTACT_NAME;
import static com.bytechef.component.zoho.commons.ZohoConstants.CURRENCY_ID;
import static com.bytechef.component.zoho.commons.ZohoConstants.SHIPPING_ADDRESS;
import static com.bytechef.component.zoho.commons.ZohoConstants.WEBSITE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class ZohoInvoiceCreateContactActionTest extends AbstractZohoInvoiceActionTest {

    @Test
    void testPerform() {
        Map<String, Object> parametersMap = Map.of(
            CONTACT_NAME, "name",
            COMPANY_NAME, "company",
            WEBSITE, "www.test.com",
            CURRENCY_ID, "euro",
            BILLING_ADDRESS,
            Map.of("attention", "test",
                "address", "test street 123",
                "street2", "test street 456",
                "state_code", "1",
                "city", "test city",
                "state", "test state",
                "zip", "10000",
                "country", "test country",
                "fax", "123456",
                "phone", "+123456789"),
            SHIPPING_ADDRESS,
            Map.of("attention", "test",
                "address", "test street 123",
                "street2", "test street 456",
                "state_code", "1",
                "city", "test city",
                "state", "test state",
                "zip", "10000",
                "country", "test country",
                "fax", "123456",
                "phone", "+123456789"));

        mockedParameters = MockParametersFactory.create(parametersMap);

        Object result = ZohoInvoiceCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(parametersMap, body.getContent());
    }
}
