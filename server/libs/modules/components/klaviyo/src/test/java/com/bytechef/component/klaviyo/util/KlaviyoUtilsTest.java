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

package com.bytechef.component.klaviyo.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ATTRIBUTES;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.DATA;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.EMAIL;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ID;
import static com.bytechef.component.klaviyo.util.KlaviyoUtils.getProfileEmail;
import static com.bytechef.component.klaviyo.util.KlaviyoUtils.getProfilePhoneNumber;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class KlaviyoUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @BeforeEach
    void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetProfileIdOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                DATA, List.of(
                    Map.of(ID, "1", ATTRIBUTES, Map.of(EMAIL, "contact1@test.com")),
                    Map.of(ID, "2", ATTRIBUTES, Map.of("email", "contact2@test.com")))));

        List<Option<String>> actualOptions = KlaviyoUtils.getProfileIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("contact1@test.com", "1"),
            option("contact2@test.com", "2"));

        assertEquals(expectedOptions, actualOptions);
    }

    @Test
    void testGetProfileEmail() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                DATA, Map.of(
                    ATTRIBUTES, Map.of("email", "contact@test.com"))));

        String email = getProfileEmail(mockedContext, "123");

        assertEquals("contact@test.com", email);
    }

    @Test
    void testGetProfilePhoneNumber() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(
                "data", Map.of(
                    "attributes", Map.of("phone_number", "+1234567890"))));

        String phoneNumber = getProfilePhoneNumber(mockedContext, "123");

        assertEquals("+1234567890", phoneNumber);
    }
}
