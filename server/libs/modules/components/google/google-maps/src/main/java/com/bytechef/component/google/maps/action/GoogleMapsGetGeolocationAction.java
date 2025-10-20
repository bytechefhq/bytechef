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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ADDRESS;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleMapsGetGeolocationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getGeolocation")
        .title("Get Geolocation")
        .description("Get geolocation of address.")
        .properties(
            string(ADDRESS)
                .label("Address")
                .description("Specify address you want geolocation of.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("results")
                            .description(
                                "When the geocoder returns results, it places them within a (JSON) results array.")
                            .items(
                                object()
                                    .properties(
                                        array("address_components")
                                            .description(
                                                "Array containing the separate components applicable to this address.")
                                            .items(
                                                object()
                                                    .properties(
                                                        string("long_name")
                                                            .description(
                                                                "Full text description or name of the address " +
                                                                    "component as returned by the Geocoder."),
                                                        string("short_name")
                                                            .description(
                                                                "Abbreviated textual name for the address component, " +
                                                                    "if available. "),
                                                        array("types")
                                                            .description(
                                                                "Array indicating the type of the address component.")
                                                            .items(string()))),
                                        string("formatted_address")
                                            .description(
                                                "String containing the human-readable address of this location."),
                                        object("geometry")
                                            .description("Contains information calculated by Geocoder.")
                                            .properties(
                                                object("location")
                                                    .description("Contains the geocoded latitude, longitude value.")
                                                    .properties(
                                                        number("lat"),
                                                        number("lng")),
                                                string("location_type")
                                                    .description(
                                                        "Stores additional data about the specified location."),
                                                object("viewport")
                                                    .description(
                                                        "Contains the recommended viewport for displaying the " +
                                                            "returned result.")
                                                    .properties(
                                                        object("northeast")
                                                            .properties(
                                                                number("lat"),
                                                                number("lng")),
                                                        object("southwest")
                                                            .properties(
                                                                number("lat"),
                                                                number("lng")))),
                                        array("navigation_points")
                                            .description("Array of navigation points for the address.")
                                            .items(
                                                object("location")
                                                    .properties(
                                                        number("latitude"),
                                                        number("longitude"))),
                                        string("place_id")
                                            .description(
                                                "A unique identifier that can be used with other Google APIs."),
                                        object("plus_code")
                                            .description(
                                                "An encoded location reference, derived from latitude and longitude " +
                                                    "coordinates")
                                            .properties(
                                                string("compound_code")
                                                    .description(
                                                        "A 6 character or longer local code with an explicit location"),
                                                string("global_code")
                                                    .description(
                                                        "A 4 character area code and 6 character or longer local " +
                                                            "code.")),
                                        array("types")
                                            .description(
                                                "This array contains a set of zero or more tags identifying the type" +
                                                    " of feature returned in the result.")
                                            .items(string()))),
                        string("status")
                            .description("Status of the request."))))
        .perform(GoogleMapsGetGeolocationAction::perform);

    private GoogleMapsGetGeolocationAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.get("https://maps.googleapis.com/maps/api/geocode/json"))
            .queryParameter(
                ADDRESS, context.encoder(
                    encoder -> encoder.urlEncode(inputParameters.getRequiredString(ADDRESS))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
