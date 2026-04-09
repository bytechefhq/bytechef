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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class KlaviyoUpdateProfileActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.ofEntries(
            entry(PROFILE_ID, "1"), entry(EMAIL, "test@test.com"), entry(PHONE_NUMBER, "+123456789"),
            entry(FIRST_NAME, "test"), entry(LAST_NAME, "test"), entry(ORGANIZATION, "company"),
            entry(LOCALE, "eu"), entry(TITLE, "manager"), entry(IMAGE, "https://image.com"),
            entry(ADDRESS1, "address 1"), entry(ADDRESS2, "address 2"), entry(CITY, "Zagreb"),
            entry(COUNTRY, "Croatia"), entry(REGION, "Zagreb"), entry(ZIP, "10000"), entry(TIMEZONE, "UTC")));
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.patch(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        Object result = KlaviyoUpdateProfileAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());

        Body capturedBody = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of(DATA, Map.of(TYPE, PROFILE, ID, "1", ATTRIBUTES, getAttributes())),
            capturedBody.getContent());

        assertEquals("/api/profiles/1", stringArgumentCaptor.getValue());
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
