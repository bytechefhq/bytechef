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

import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LATITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LONGITUDE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
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
class GoogleMapsGetAddressActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(LATITUDE, 0.0, LONGITUDE, 0.0));
    private final Map<String, Object> responseMap = Map.of();
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        try (MockedStatic<GoogleMapsUtils> mockedGoogleMapsUtils = mockStatic(GoogleMapsUtils.class)) {
            mockedGoogleMapsUtils
                .when(() -> GoogleMapsUtils.geocodeHttpRequest(
                    contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(responseMap);

            Map<String, Object> result = GoogleMapsGetAddressAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(List.of("latlng", "0.0,0.0"), stringArgumentCaptor.getAllValues());
        }
    }
}
