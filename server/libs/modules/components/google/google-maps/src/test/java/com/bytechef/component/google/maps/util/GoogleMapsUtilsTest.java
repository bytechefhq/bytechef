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

package com.bytechef.component.google.maps.util;

import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ADDRESS;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LATITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LONGITUDE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class GoogleMapsUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Http.Executor.class);
    private final Response mockedResponse = mock(Response.class);
    private final Map<String, Object> responseMap = Map.of(
        "results", List.of(
            Map.of("geometry", Map.of("location", Map.of("lat", 0.0, "lng", 0.0)))));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetAddressGeolocation() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        String urlEncodedAddress = "urlEncodedAddress";
        when(mockedContext.encoder(any()))
            .thenReturn(urlEncodedAddress);

        String mockedAddress = "mockedAddress";
        Map<String, Object> result = GoogleMapsUtils.getAddressGeolocation(mockedContext, mockedAddress);
        Map<String, Object> expected = Map.of(LATITUDE, 0.0, LONGITUDE, 0.0);

        assertEquals(expected, result);

        assertEquals(List.of(ADDRESS, urlEncodedAddress), stringArgumentCaptor.getAllValues());
    }
}
