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

package com.bytechef.component.liferay;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class LiferayComponentHandlerTest {

    @Test
    void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/liferay_v1.json", new LiferayComponentHandler().getDefinition());
    }
}

//void testPerform() {
//    when(mockedContext.http(any()))
//        .thenReturn(mockedExecutor);
//    when(mockedExecutor.configuration(any()))
//        .thenReturn(mockedExecutor);
//    when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
//        .thenReturn(mockedExecutor);
//    when(mockedExecutor.body(bodyArgumentCaptor.capture()))
//        .thenReturn(mockedExecutor);
//    when(mockedExecutor.execute())
//        .thenReturn(mockedResponse);
//    when(mockedResponse.getBody(any(TypeReference.class)))
//        .thenReturn(Map.of());
//
//    try (MockedStatic<GoogleMapsUtils> mockedGoogleMapsUtils = mockStatic(GoogleMapsUtils.class)) {
//
//        mockedGoogleMapsUtils
//            .when(() -> GoogleMapsUtils.getAddressLatLng(contextArgumentCaptor.capture(),
//                stringArgumentCaptor.capture()))
//            .thenReturn(Map.of(LATITUDE, 0.0, LONGITUDE, 0.0));
//
//        Map<String, Object> result = GoogleMapsGetRouteAction.perform(
//            mockedParameters, mockedParameters, mockedContext);
//
//        assertEquals(Map.of(), result);
//
//        assertEquals(mockedContext, contextArgumentCaptor.getValue());
//        assertEquals(List.of("origin", "destination", "X-Goog-FieldMask", "*"),
//            stringArgumentCaptor.getAllValues());
//
//        Body body = bodyArgumentCaptor.getValue();
//
//        Map<String, Object> expectedBody = Map.of(
//            ORIGIN, Map.of(
//                LOCATION, Map.of(
//                    LAT_LNG, Map.of(
//                        LATITUDE, 0.0,
//                        LONGITUDE, 0.0))),
//            DESTINATION, Map.of(
//                LOCATION, Map.of(
//                    LAT_LNG, Map.of(
//                        LATITUDE, 0.0,
//                        LONGITUDE, 0.0))),
//            TRAVEL_MODE, "DRIVE",
//            ROUTING_PREFERENCE, "TRAFFIC_UNAWARE",
//            COMPUTE_ALT_ROUTES, false,
//            "routeModifiers", Map.of(
//                AVOID_TOLLS, false,
//                AVOID_HIGHWAYS, false,
//                AVOID_FERRIES, false),
//            UNITS, "METRIC");
//
//        assertEquals(expectedBody, body.getContent());
//    }
//}
