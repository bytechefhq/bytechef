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

import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ADDRESS;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LATITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LONGITUDE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleMapsUtils {

    public static Map<String, Object> geocodeHttpRequest(Context context, String key, String value) {
        return context.http(http -> http.get("https://maps.googleapis.com/maps/api/geocode/json"))
            .queryParameter(key, value)
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    public static Map<String, Object> getAddressGeolocation(Context context, String address) {

        Map<String, Object> geocoderInformation = context.http(
            http -> http.get("https://maps.googleapis.com/maps/api/geocode/json"))
            .queryParameter(ADDRESS, context.encoder(encoder -> encoder.base64UrlEncode(address)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        Map<String, Object> geoLocation = new HashMap<>();

        if (geocoderInformation.get("results") instanceof List<?> resultsList &&
            resultsList.getFirst() instanceof Map<?, ?> result &&
            result.get("geometry") instanceof Map<?, ?> geometry &&
            geometry.get("location") instanceof Map<?, ?> location) {

            geoLocation.put(LATITUDE, location.get("lat"));
            geoLocation.put(LONGITUDE, location.get("lng"));
        }

        return geoLocation;
    }
}
