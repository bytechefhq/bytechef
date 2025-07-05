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

import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.PROFILE_ID;
import static com.bytechef.component.klaviyo.constant.KlaviyoConstants.SUBSCRIPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.klaviyo.util.KlaviyoUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class KlaviyoSubscribeProfilesActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<Map<String, List<String>>> headerArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(PROFILE_ID, List.of("1", "2", "3"), SUBSCRIPTION, List.of("email", "sms")));
    private final Object mockedObject = mock(Object.class);

    @Test
    void testPerform() {
        try (MockedStatic<KlaviyoUtils> klaviyoUtilsMockedStatic = mockStatic(KlaviyoUtils.class)) {
            klaviyoUtilsMockedStatic
                .when(() -> KlaviyoUtils.getProfileEmail(any(), eq("1")))
                .thenReturn("test1@example.com");
            klaviyoUtilsMockedStatic
                .when(() -> KlaviyoUtils.getProfileEmail(any(), eq("2")))
                .thenReturn("test2@example.com");
            klaviyoUtilsMockedStatic
                .when(() -> KlaviyoUtils.getProfileEmail(any(), eq("3")))
                .thenReturn("test3@example.com");

            klaviyoUtilsMockedStatic
                .when(() -> KlaviyoUtils.getProfilePhoneNumber(any(), eq("1")))
                .thenReturn("+1111111");
            klaviyoUtilsMockedStatic
                .when(() -> KlaviyoUtils.getProfilePhoneNumber(any(), eq("2")))
                .thenReturn("+2222222");
            klaviyoUtilsMockedStatic
                .when(() -> KlaviyoUtils.getProfilePhoneNumber(any(), eq("3")))
                .thenReturn("+3333333");

            when(mockedContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.headers(headerArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody())
                .thenReturn(mockedObject);

            Object result = KlaviyoSubscribeProfilesAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);

            assertEquals(
                List.of(Map.of(
                    "accept", List.of("application/vnd.api+json"),
                    "revision", List.of("2025-04-15"))),
                headerArgumentCaptor.getAllValues());

            Body capturedBody = bodyArgumentCaptor.getValue();

            List<Map<String, Object>> expectedProfiles = List.of(
                profileMap("1", "test1@example.com", "+1111111"),
                profileMap("2", "test2@example.com", "+2222222"),
                profileMap("3", "test3@example.com", "+3333333"));

            assertEquals(Map.of(
                "data", Map.of(
                    "type", "profile-subscription-bulk-create-job",
                    "attributes", Map.of(
                        "profiles", Map.of("data", expectedProfiles)))),
                capturedBody.getContent());
        }
    }

    private Map<String, Object> profileMap(String id, String email, String phone) {
        Map<String, Object> subscriptions = Map.of(
            "email", Map.of("marketing", Map.of("consent", "SUBSCRIBED")),
            "sms", Map.of("marketing", Map.of("consent", "SUBSCRIBED")));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("phone_number", phone);
        attributes.put("subscriptions", subscriptions);

        return Map.of(
            "type", "profile",
            "id", id,
            "attributes", attributes);
    }
}
