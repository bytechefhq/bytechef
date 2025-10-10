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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ADDRESS;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.INCLUDED_TYPES;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LATITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LONGITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.RADIUS;
import static com.bytechef.component.google.maps.util.GoogleMapsUtils.getAddressLatLng;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleMapsNearbySearchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("nearbySearch")
        .title("Nearby Search")
        .description(
            "Action takes one or more place types, and returns a list of matching places within the specified area.")
        .properties(
            array(INCLUDED_TYPES)
                .label("Included")
                .required(true)
                .description("Keywords which will be used for filtering.")
                .items(
                    string("keyword")
                        .label("Keyword")
                        .description("Filter keyword, e.g. restaurant, cafe, gas station.")
                        .required(true)),
            string(ADDRESS)
                .label("Address")
                .description("Center address of the nearby search.")
                .required(true),
            number(RADIUS)
                .label("Radius")
                .description(
                    "Radius of circle area that will be searched. The radius must be between 0.0 meters and 50000.0 " +
                        "meters inclusive.")
                .minValue(0.0)
                .maxValue(50000.0)
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                object("location")
                                    .description("Location of the place.")
                                    .properties(
                                        number("latitude")
                                            .description("Latitude of the geolocation."),
                                        number("longitude")
                                            .description("Longitude of the geolocation.")),
                                string("businessStatus")
                                    .description("Business status of the place."),
                                object("displayName")
                                    .description("Display name of the place.")
                                    .properties(
                                        string("text")
                                            .description("Text of the display name."),
                                        string("languageCode")
                                            .description("Language code of the language that is used.")),
                                object("accessibilityOptions")
                                    .description("Accessibility options of the place.")
                                    .properties(
                                        bool("wheelchairAccessibleParking")
                                            .description("Whether the place has a wheelchair accessible parking."),
                                        bool("wheelchairAccessibleEntrance")
                                            .description("Whether the place has a wheelchair accessible entrance."),
                                        bool("wheelchairAccessibleRestroom")
                                            .description("Whether the place has a wheelchair accessible restroom."),
                                        bool("wheelchairAccessibleSeating")
                                            .description("Whether the place has a wheelchair accessible seating.")),
                                object("googleMapsLinks")
                                    .description("Google Maps links connected to the place.")
                                    .properties(
                                        string("directionsUri")
                                            .description("URL for the directions."),
                                        string("placeUri")
                                            .description("URL of the place."),
                                        string("writeAReviewUri")
                                            .description("Write a review link."),
                                        string("reviewsUri")
                                            .description("Reviews URL."),
                                        string("photosUri")
                                            .description("Photos URL."))))))
        .perform(GoogleMapsNearbySearchAction::perform);

    private GoogleMapsNearbySearchAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> addressGeoLocation = getAddressLatLng(
            context, inputParameters.getRequiredString(ADDRESS));

        return context.http(
            http -> http.post("https://places.googleapis.com/v1/places:searchNearby"))
            .header(
                "X-Goog-FieldMask",
                "places.accessibilityOptions,places.displayName,places.googleMapsLinks,places.location," +
                    "places.businessStatus")
            .body(
                Body.of(
                    "includedTypes", inputParameters.getRequiredList(INCLUDED_TYPES),
                    "locationRestriction", Map.of(
                        "circle", Map.of(
                            "center", Map.of(
                                LATITUDE, addressGeoLocation.get(LATITUDE),
                                LONGITUDE, addressGeoLocation.get(LONGITUDE)),
                            RADIUS, inputParameters.getRequiredDouble(RADIUS)))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
