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

package com.bytechef.component.google.maps.action;

import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ADDRESS;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.INCLUDED_TYPES;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LATITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LONGITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.RADIUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.maps.util.GoogleMapsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class GoogleMapsNearbySearchActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(INCLUDED_TYPES, List.of("keyword"), ADDRESS, "mockedAddress", RADIUS, 0.0));
    private final Response mockedResponse = mock(Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());

        try (MockedStatic<GoogleMapsUtils> mockedGoogleMapsUtils = mockStatic(GoogleMapsUtils.class)) {
            mockedGoogleMapsUtils
                .when(() -> GoogleMapsUtils.getAddressGeolocation(contextArgumentCaptor.capture(),
                    stringArgumentCaptor.capture()))
                .thenReturn(Map.of(LATITUDE, 0.0, LONGITUDE, 0.0));

            Map<String, Object> result = GoogleMapsNearbySearchAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(Map.of(), result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(List.of("mockedAddress", "X-Goog-FieldMask", "*"), stringArgumentCaptor.getAllValues());

            Body body = bodyArgumentCaptor.getValue();

            Map<String, Object> expectedBody = Map.of(
                "includedTypes", List.of("keyword"),
                "locationRestriction", Map.of(
                    "circle", Map.of(
                        "center", Map.of(
                            LATITUDE, 0.0,
                            LONGITUDE, 0.0),
                        RADIUS, 0.0)));

            assertEquals(expectedBody, body.getContent());
        }
    }
}
