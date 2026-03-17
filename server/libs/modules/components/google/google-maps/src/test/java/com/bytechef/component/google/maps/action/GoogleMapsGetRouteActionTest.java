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

import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.AVOID_FERRIES;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.AVOID_HIGHWAYS;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.AVOID_TOLLS;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.COMPUTE_ALT_ROUTES;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.DESTINATION;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LATITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LAT_LNG;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LOCATION;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LONGITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ORIGIN;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ROUTING_PREFERENCE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.TRAVEL_MODE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.maps.util.GoogleMapsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleMapsGetRouteActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ORIGIN, "origin", DESTINATION, "destination", TRAVEL_MODE, "DRIVE",
            ROUTING_PREFERENCE, "TRAFFIC_UNAWARE", COMPUTE_ALT_ROUTES, false, AVOID_TOLLS, false,
            AVOID_HIGHWAYS, false, AVOID_FERRIES, false, UNITS, "METRIC"));
    private final Map<String, Object> responseMap = Map.of();
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        try (MockedStatic<GoogleMapsUtils> mockedGoogleMapsUtils = mockStatic(GoogleMapsUtils.class)) {
            mockedGoogleMapsUtils
                .when(() -> GoogleMapsUtils.getAddressGeolocation(
                    contextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(Map.of(LATITUDE, 0.0, LONGITUDE, 0.0));

            Map<String, Object> result = GoogleMapsGetRouteAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(
                List.of(
                    "origin", "destination", "https://routes.googleapis.com/directions/v2:computeRoutes",
                    "X-Goog-FieldMask", "*"),
                stringArgumentCaptor.getAllValues());

            Map<String, Object> expectedBody = Map.of(
                ORIGIN, Map.of(
                    LOCATION, Map.of(
                        LAT_LNG, Map.of(
                            LATITUDE, 0.0,
                            LONGITUDE, 0.0))),
                DESTINATION, Map.of(
                    LOCATION, Map.of(
                        LAT_LNG, Map.of(
                            LATITUDE, 0.0,
                            LONGITUDE, 0.0))),
                TRAVEL_MODE, "DRIVE",
                ROUTING_PREFERENCE, "TRAFFIC_UNAWARE",
                COMPUTE_ALT_ROUTES, false,
                "routeModifiers", Map.of(
                    AVOID_TOLLS, false,
                    AVOID_HIGHWAYS, false,
                    AVOID_FERRIES, false),
                UNITS, "METRIC");

            assertEquals(Body.of(expectedBody, BodyContentType.JSON), bodyArgumentCaptor.getValue());

            assertNotNull(httpFunctionArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }
    }
}
