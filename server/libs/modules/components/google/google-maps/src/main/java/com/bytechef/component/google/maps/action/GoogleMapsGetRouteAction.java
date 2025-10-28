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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.AVOID_FERRIES;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.AVOID_HIGHWAYS;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.AVOID_TOLLS;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.COMPUTE_ALT_ROUTES;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.DESTINATION;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.FALLBACK_INFO_PROPERTY;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.GEOCODING_RESULTS_PROPERTY;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LATITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LAT_LNG;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LOCATION;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.LONGITUDE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ORIGIN;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ROUTE_PROPERTY;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.ROUTING_PREFERENCE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.TRAVEL_MODE;
import static com.bytechef.component.google.maps.constant.GoogleMapsConstants.UNITS;
import static com.bytechef.component.google.maps.util.GoogleMapsUtils.getAddressGeolocation;

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
public class GoogleMapsGetRouteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getRoute")
        .title("Get Route")
        .description("Get route between inputted origin and destination.")
        .properties(
            string(ORIGIN)
                .label("Origin")
                .description(
                    "Specify address of origin in accordance with the format used by the national postal service of " +
                        "the country concerned. Additional address elements such as business names and unit, suite " +
                        "or floor numbers should be avoided. Street address elements should be delimited by spaces.")
                .required(true),
            string(DESTINATION)
                .label("Destination")
                .description(
                    "Specify address of destination in accordance with the format used by the national postal " +
                        "service of the country concerned. Additional address elements such as business names and " +
                        "unit, suite or floor numbers should be avoided. Street address elements should be delimited " +
                        "by spaces.")
                .required(true),
            string(TRAVEL_MODE)
                .label("Travel Mode")
                .description("Desired travel mode.")
                .defaultValue("DRIVE")
                .options(
                    option("Drive", "DRIVE"),
                    option("Bicycle", "BICYCLE"),
                    option("Walk", "WALK"),
                    option("Transit", "TRANSIT"))
                .required(false),
            string(ROUTING_PREFERENCE)
                .label("Routing Preference")
                .description("Routing preference of the route.")
                .defaultValue("TRAFFIC_UNAWARE")
                .options(
                    option("Traffic Unaware", "TRAFFIC_UNAWARE"),
                    option("Traffic Aware", "TRAFFIC_AWARE"),
                    option("Traffic Aware Optimal", "TRAFFIC_AWARE_OPTIMAL"))
                .required(false),
            bool(COMPUTE_ALT_ROUTES)
                .label("Compute Alternative Routes")
                .description("Whether alternative routes should be computed.")
                .defaultValue(false)
                .required(false),
            bool(AVOID_TOLLS)
                .label("Avoid Tolls")
                .description("Whether to avoid tolls.")
                .defaultValue(false)
                .required(false),
            bool(AVOID_HIGHWAYS)
                .label("Avoid Highways")
                .description("Whether to avoid highways.")
                .defaultValue(false)
                .required(false),
            bool(AVOID_FERRIES)
                .label("Avoid Ferries")
                .description("Whether to avoid ferries.")
                .defaultValue(false)
                .required(false),
            string(UNITS)
                .label("Units")
                .description("Metrics of the route")
                .defaultValue("METRIC")
                .options(
                    option("Metric", "METRIC"),
                    option("Imperial", "IMPERIAL"))
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("routes")
                            .description("Routes from origin to destination.")
                            .items(ROUTE_PROPERTY),
                        FALLBACK_INFO_PROPERTY,
                        GEOCODING_RESULTS_PROPERTY)))
        .perform(GoogleMapsGetRouteAction::perform);

    private GoogleMapsGetRouteAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> originGeoLocation = getAddressGeolocation(
            context, inputParameters.getRequiredString(ORIGIN));

        Map<String, Object> destinationGeoLocation = getAddressGeolocation(
            context, inputParameters.getRequiredString(DESTINATION));

        return context.http(http -> http.post("https://routes.googleapis.com/directions/v2:computeRoutes"))
            .configuration(responseType(ResponseType.JSON))
            .header("X-Goog-FieldMask", "*")
            .body(
                Body.of(
                    ORIGIN, Map.of(
                        LOCATION, Map.of(
                            LAT_LNG, Map.of(
                                LATITUDE, originGeoLocation.get(LATITUDE),
                                LONGITUDE, originGeoLocation.get(LONGITUDE)))),
                    DESTINATION, Map.of(
                        LOCATION, Map.of(
                            LAT_LNG, Map.of(
                                LATITUDE, destinationGeoLocation.get(LATITUDE),
                                LONGITUDE, destinationGeoLocation.get(LONGITUDE)))),
                    TRAVEL_MODE, inputParameters.getString(TRAVEL_MODE),
                    ROUTING_PREFERENCE, inputParameters.getString(ROUTING_PREFERENCE),
                    COMPUTE_ALT_ROUTES, inputParameters.getBoolean(COMPUTE_ALT_ROUTES),
                    "routeModifiers", Map.of(
                        AVOID_TOLLS, inputParameters.getBoolean(AVOID_TOLLS),
                        AVOID_HIGHWAYS, inputParameters.getBoolean(AVOID_HIGHWAYS),
                        AVOID_FERRIES, inputParameters.getBoolean(AVOID_FERRIES)),
                    UNITS, inputParameters.getString(UNITS)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
