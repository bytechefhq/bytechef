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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.GEOCODING_RESPONSE_PROPERTY;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LATITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LONGITUDE;
import static com.bytechef.component.google.maps.util.GoogleMapsUtils.geocodeHttpRequest;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleMapsGetAddressAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getAddress")
        .title("Get Address")
        .description("Get address from inputted geolocation.")
        .properties(
            number(LATITUDE)
                .label("Latitude")
                .description("Latitude of the geolocation.")
                .required(true),
            number(LONGITUDE)
                .label("Longitude")
                .description("Longitude of the geolocation.")
                .required(true))
        .output(outputSchema(GEOCODING_RESPONSE_PROPERTY))
        .perform(GoogleMapsGetAddressAction::perform);

    private GoogleMapsGetAddressAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String latitude = String.valueOf(inputParameters.getRequiredDouble(LATITUDE));
        String longitude = String.valueOf(inputParameters.getRequiredDouble(LONGITUDE));

        return geocodeHttpRequest(context, "latlng", "%s,%s".formatted(latitude, longitude));
    }
}
