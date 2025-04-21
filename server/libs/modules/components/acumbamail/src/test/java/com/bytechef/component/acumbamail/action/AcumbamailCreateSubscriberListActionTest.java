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

package com.bytechef.component.acumbamail.action;

import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.ADDRESS;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.CITY;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.COMPANY;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.COUNTRY;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.EMAIL;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.LIST_ID;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.NAME;
import static com.bytechef.component.acumbamail.constant.AcumbamailConstants.PHONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class AcumbamailCreateSubscriberListActionTest extends AbstractAcumbamailActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            LIST_ID, "test", EMAIL, "test@test.com", NAME, "test-name", COMPANY, "test-company",
            COUNTRY, "test-country", CITY, "test-city", ADDRESS, "test-address", PHONE, "test-phone"));

    @Test
    void testPerform() {
        Object result = AcumbamailCreateSubscriberListAction.perform(
            mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(responseMap, result);

        Object[] query = queryArgumentCaptor.getValue();
        assertEquals(
            List.of(
                "sender_email", "test@test.com", NAME, "test-name", COMPANY, "test-company",
                COUNTRY, "test-country", CITY, "test-city", ADDRESS, "test-address", PHONE, "test-phone"),
            Arrays.asList(query));
    }
}
