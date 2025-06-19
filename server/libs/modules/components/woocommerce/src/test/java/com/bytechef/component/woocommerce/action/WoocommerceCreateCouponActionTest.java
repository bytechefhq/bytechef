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

import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.AMOUNT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CODE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DESCRIPTION;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DISCOUNT_TYPE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.EXCLUDE_SALE_ITEMS;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.INDIVIDUAL_USE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.MAXIMUM_AMOUNT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.MINIMUM_AMOUNT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.PRODUCT_IDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class WoocommerceCreateCouponActionTest extends AbstractWoocommerceActionTest {

    @Test
    void testPerform() {
        Map<String, Object> bodyMap = Map.of(
            CODE, "1",
            AMOUNT, "2",
            DISCOUNT_TYPE, "percent",
            DESCRIPTION, "This is a coupon.",
            INDIVIDUAL_USE, true,
            PRODUCT_IDS, List.of("product1", "product2", "product3"),
            EXCLUDE_SALE_ITEMS, false,
            MINIMUM_AMOUNT, "10",
            MAXIMUM_AMOUNT, "100");

        Parameters mockedParameters = MockParametersFactory.create(bodyMap);

        Object result = WoocommerceCreateCouponAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(bodyMap, body.getContent());
    }
}
