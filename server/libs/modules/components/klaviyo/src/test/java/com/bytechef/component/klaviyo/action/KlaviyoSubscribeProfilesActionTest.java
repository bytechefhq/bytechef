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

import static com.bytechef.component.klaviyo.action.KlaviyoSubscribeProfilesAction.SubscriptionType.EMAIL;
import static com.bytechef.component.klaviyo.action.KlaviyoSubscribeProfilesAction.SubscriptionType.SMS;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ATTRIBUTES;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.DATA;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PHONE_NUMBER;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE_ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.SUBSCRIPTION;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.klaviyo.constant.KlaviyoConstants;
import com.bytechef.component.klaviyo.util.KlaviyoUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class KlaviyoSubscribeProfilesActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(PROFILE_ID, List.of("1", "2", "3"), SUBSCRIPTION, List.of(EMAIL.getValue(), SMS.getValue())));

    @Test
    void testPerform(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<KlaviyoUtils> klaviyoUtilsMockedStatic = mockStatic(KlaviyoUtils.class)) {
            klaviyoUtilsMockedStatic
                .when(
                    () -> KlaviyoUtils.getProfileEmail(contextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn("test1@example.com", "test2@example.com", "test3@example.com");

            klaviyoUtilsMockedStatic
                .when(() -> KlaviyoUtils.getProfilePhoneNumber(contextArgumentCaptor.capture(),
                    stringArgumentCaptor.capture()))
                .thenReturn("+1111111", "+2222222", "+3333333");

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);

            assertNull(KlaviyoSubscribeProfilesAction.perform(mockedParameters, null, mockedContext));

            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());

            Body capturedBody = bodyArgumentCaptor.getValue();

            List<Map<String, Object>> expectedProfiles = List.of(
                profileMap("1", "test1@example.com", "+1111111"),
                profileMap("2", "test2@example.com", "+2222222"),
                profileMap("3", "test3@example.com", "+3333333"));

            assertEquals(
                Map.of(
                    DATA, Map.of(
                        TYPE, "profile-subscription-bulk-create-job",
                        ATTRIBUTES, Map.of("profiles", Map.of(DATA, expectedProfiles)))),
                capturedBody.getContent());

            assertEquals(
                List.of(mockedContext, mockedContext, mockedContext, mockedContext, mockedContext, mockedContext),
                contextArgumentCaptor.getAllValues());
            assertEquals(
                List.of("1", "1", "2", "2", "3", "3", "/api/profile-subscription-bulk-create-jobs"),
                stringArgumentCaptor.getAllValues());
        }
    }

    private Map<String, Object> profileMap(String id, String email, String phone) {
        Map<String, Object> subscriptions = Map.of(
            EMAIL.getValue(), Map.of("marketing", Map.of("consent", "SUBSCRIBED")),
            SMS.getValue(), Map.of("marketing", Map.of("consent", "SUBSCRIBED")));

        Map<String, Object> attributes = Map.of(
            KlaviyoConstants.EMAIL, email, PHONE_NUMBER, phone, "subscriptions", subscriptions);

        return Map.of(TYPE, PROFILE, ID, id, ATTRIBUTES, attributes);
    }
}
