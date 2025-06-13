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

package com.bytechef.component.woocommerce.action;

import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ADDRESS_1;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ADDRESS_2;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.BILLING;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CITY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.COMPANY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.COUNTRY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.EMAIL;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.FIRST_NAME;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.LAST_NAME;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PHONE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.POSTCODE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.SHIPPING;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STATE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.USERNAME;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class WoocommerceCreateCustomerActionTest extends AbstractWoocommerceActionTest {

    @Test
    void testPerform() {
        Map<String, Object> bodyMap = Map.of(
            EMAIL, "test@test.com",
            FIRST_NAME, "firstName",
            LAST_NAME, "lastName",
            USERNAME, "username",
            BILLING, Map.ofEntries(
                entry(FIRST_NAME, "firstName"),
                entry(LAST_NAME, "lastName"),
                entry(COMPANY, "company"),
                entry(ADDRESS_1, "test street 1"),
                entry(ADDRESS_2, "test street 2"),
                entry(CITY, "Test city"),
                entry(STATE, "Test state"),
                entry(POSTCODE, "1"),
                entry(COUNTRY, "Test Country"),
                entry(EMAIL, "test@test.com"),
                entry(PHONE, "+123456")),
            SHIPPING, Map.ofEntries(
                entry(FIRST_NAME, "firstName"),
                entry(LAST_NAME, "lastName"),
                entry(COMPANY, "company"),
                entry(ADDRESS_1, "test street 1"),
                entry(ADDRESS_2, "test street 2"),
                entry(CITY, "Test city"),
                entry(STATE, "Test state"),
                entry(POSTCODE, "1"),
                entry(COUNTRY, "Test Country"),
                entry(PHONE, "+123456")));

        Parameters mockedParameters = MockParametersFactory.create(bodyMap);

        Object result = WoocommerceCreateCustomerAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(bodyMap, body.getContent());
    }
}
