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

package com.bytechef.component.klaviyo.action;

import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ADDRESS1;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ADDRESS2;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ATTRIBUTES;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.CITY;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.COUNTRY;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.DATA;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.EMAIL;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.FIRST_NAME;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.IMAGE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.LAST_NAME;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.LOCALE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ORGANIZATION;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PHONE_NUMBER;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE_ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.REGION;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.TIMEZONE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.TITLE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.TYPE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ZIP;
import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
class KlaviyoUpdateProfileActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.ofEntries(
            entry(PROFILE_ID, "1"), entry(EMAIL, "test@test.com"), entry(PHONE_NUMBER, "+123456789"),
            entry(FIRST_NAME, "test"), entry(LAST_NAME, "test"), entry(ORGANIZATION, "company"),
            entry(LOCALE, "eu"), entry(TITLE, "manager"), entry(IMAGE, "https://image.com"),
            entry(ADDRESS1, "address 1"), entry(ADDRESS2, "address 2"), entry(CITY, "Zagreb"),
            entry(COUNTRY, "Croatia"), entry(REGION, "Zagreb"), entry(ZIP, "10000"), entry(TIMEZONE, "UTC")));
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = KlaviyoUpdateProfileAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedObject, result);

        Body capturedBody = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of(DATA, Map.of(TYPE, PROFILE, ID, "1", ATTRIBUTES, getAttributes())),
            capturedBody.getContent());
    }

    private static Map<String, Object> getAttributes() {
        Map<String, Object> location = Map.of(
            ADDRESS1, "address 1", ADDRESS2, "address 2", CITY, "Zagreb", COUNTRY, "Croatia",
            REGION, "Zagreb", ZIP, "10000", TIMEZONE, "UTC");

        return Map.of(
            EMAIL, "test@test.com",
            PHONE_NUMBER, "+123456789",
            FIRST_NAME, "test",
            LAST_NAME, "test",
            ORGANIZATION, "company",
            LOCALE, "eu",
            TITLE, "manager",
            IMAGE, "https://image.com",
            "location", location);
    }
}
