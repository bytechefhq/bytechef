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

import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.CATEGORIES;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DESCRIPTION;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.DIMENSIONS;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.HEIGHT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.ID;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.LENGTH;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.MANAGE_STOCK;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.NAME;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.REGULAR_PRICE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STOCK_QUANTITY;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.STOCK_STATUS;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.TYPE;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.WEIGHT;
import static com.bytechef.component.woocommerce.constants.WoocommerceConstants.WIDTH;
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
class WoocommerceCreateProductActionTest extends AbstractWoocommerceActionTest {

    @Test
    void testPerform() {
        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                NAME, "Product",
                REGULAR_PRICE, "10",
                TYPE, "simple",
                DESCRIPTION, "This is a test product.",
                MANAGE_STOCK, true,
                STOCK_QUANTITY, 100,
                STOCK_STATUS, "instock",
                WEIGHT, "1",
                CATEGORIES, List.of("category1", "category2"),
                DIMENSIONS, Map.of(
                    LENGTH, "1",
                    WIDTH, "2",
                    HEIGHT, "3")));

        Object result = WoocommerceCreateProductAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Map<String, Object> expectedBody = Map.of(
            NAME, "Product",
            REGULAR_PRICE, "10",
            TYPE, "simple",
            DESCRIPTION, "This is a test product.",
            MANAGE_STOCK, true,
            STOCK_QUANTITY, 100,
            STOCK_STATUS, "instock",
            WEIGHT, "1",
            CATEGORIES, List.of(Map.of(ID, "category1"), Map.of(ID, "category2")),
            DIMENSIONS, Map.of(
                LENGTH, "1",
                WIDTH, "2",
                HEIGHT, "3"));

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(expectedBody, body.getContent());
    }
}
