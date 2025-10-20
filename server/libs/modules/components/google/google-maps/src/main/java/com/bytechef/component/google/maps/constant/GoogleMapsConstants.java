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

package com.bytechef.component.google.maps.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Nikolina Spehar
 */
public class GoogleMapsConstants {

    public static final String ADDRESS = "address";
    public static final String AVOID_FERRIES = "avoidFerries";
    public static final String AVOID_HIGHWAYS = "avoidHighways";
    public static final String AVOID_TOLLS = "avoidTolls";
    public static final String COMPUTE_ALT_ROUTES = "computeAlternativeRoutes";
    public static final String DESTINATION = "destination";
    public static final String INCLUDED_TYPES = "includedTypes";
    public static final String LAT_LNG = "latLng";
    public static final String LATITUDE = "latitude";
    public static final String LOCATION = "location";
    public static final String LONGITUDE = "longitude";
    public static final String ORIGIN = "origin";
    public static final String RADIUS = "radius";
    public static final String ROUTING_PREFERENCE = "routingPreference";
    public static final String TRAVEL_MODE = "travelMode";
    public static final String UNITS = "units";

    public static final ModifiableObjectProperty FALLBACK_INFO_PROPERTY = object("fallbackInfo")
        .description(
            "In some cases when the server is not able to compute the route results with all of the input " +
                "preferences, it may fallback to using a different way of computation.")
        .properties(
            string("routingMode")
                .description(
                    "Routing mode used for the response. If fallback was triggered, the mode may be different " +
                        "from routing preference set in the original client request."),
            string("reason")
                .description("The reason why fallback response was used instead of the original response."));

    public static final ModifiableObjectProperty GEOCODED_WAYPOINT_PROPERTY = object("geocodedWaypoint")
        .description("Details about the locations used as waypoints.")
        .properties(
            object("status")
                .description("Indicates the status code resulting from the geocoding operation.")
                .properties(
                    integer("code")
                        .description("The status code, which should be an enum value of google.rpc.Code."),
                    string("message")
                        .description("A developer-facing error message, which should be in English."),
                    array("details")
                        .description(
                            "A list of messages that carry the error details. There is a common set of message " +
                                "types for APIs to use.")
                        .items(object())),
            array("type")
                .description("The type(s) of the result, in the form of zero or more type tags.")
                .items(string()),
            bool("partialMatch")
                .description(
                    "Indicates that the geocoder did not return an exact match for the original request, though it " +
                        "was able to match part of the requested address."),
            string("placeId")
                .description("The place ID for this result."),
            integer("intermediateWaypointRequestIndex")
                .description("The index of the corresponding intermediate waypoint in the request."));

    public static final ModifiableObjectProperty GEOCODING_RESULTS_PROPERTY = object("geocodingResults")
        .description("Contains geocoding response info for waypoints specified as addresses.")
        .properties(
            object("origin")
                .description("Origin geocoded waypoint.")
                .properties(GEOCODED_WAYPOINT_PROPERTY),
            object("destination")
                .description("Destination geocoded waypoint.")
                .properties(GEOCODED_WAYPOINT_PROPERTY),
            array("intermediates")
                .description(
                    "A list of intermediate geocoded waypoints each containing an index field that corresponds to " +
                        "the zero-based position of the waypoint in the order they were specified in the request.")
                .items(GEOCODED_WAYPOINT_PROPERTY));

    public static final ModifiableObjectProperty LAT_LNG_PROPERTY = object("latLng")
        .description("Latitude and longitude of the location.")
        .properties(
            number("latitude")
                .description("Latitude of the location."),
            number("longitude")
                .description("Longitude of the location."));

    public static final ModifiableObjectProperty LOCATION_PROPERTY = object("location")
        .properties(
            LAT_LNG_PROPERTY,
            integer("heading")
                .description("The compass heading associated with the direction of the flow of traffic."));

    public static final ModifiableObjectProperty START_LOCATION_PROPERTY = object("startLocation")
        .description("Start location of the route.")
        .properties(LOCATION_PROPERTY);

    public static final ModifiableObjectProperty END_LOCATION_PROPERTY = object("endLocation")
        .description("End location of the route.")
        .properties(LOCATION_PROPERTY);

    public static final ModifiableObjectProperty LOCALIZED_TEXT_PROPERTY = object("localizedText")
        .description("Localized variant of a text in a particular language.")
        .properties(
            string("text")
                .description("Localized string in the language corresponding to languageCode below."),
            string("languageCode")
                .description("The text's BCP-47 language code, such as \"en-US\" or \"sr-Latn\"."));

    public static final ModifiableObjectProperty LOCALIZED_TIME_PROPERTY = object("localizedTime")
        .properties(
            object("time")
                .description("The time specified as a string in a given time zone.")
                .properties(LOCALIZED_TEXT_PROPERTY),
            string("timeZone")
                .description("Contains the time zone."));

    public static final ModifiableObjectProperty MONEY_PROPERTY = object("money")
        .description("Represents an amount of money with its currency type.")
        .properties(
            string("currencyCode")
                .description("The three-letter currency code defined in ISO 4217."),
            string("units")
                .description("The whole units of the amount."),
            integer("nanos")
                .description("Number of nano (10^-9) units of the amount."));

    public static final ModifiableObjectProperty NAVIGATION_INSTRUCTIONS_PROPERTY = object("navigationInstructions")
        .description("Encapsulates navigation instructions for a RouteLegStep")
        .properties(
            string("maneuver")
                .description(
                    "Encapsulates the navigation instructions for the current step (for example, turn left, merge, " +
                        "or straight)."),
            string("instructions")
                .description("Instructions for navigating this step."));

    public static final ModifiableObjectProperty POLYLINE_POINT_INDEX_PROPERTY = object("polylinePointIndex")
        .description(
            "Encapsulates the start and end indexes for a polyline detail. For instances where the data corresponds " +
                "to a single point, startIndex and endIndex will be equal.")
        .properties(
            integer("startIndex")
                .description("The start index of this detail in the polyline."),
            integer("endIndex")
                .description("The end index of this detail in the polyline."));

    public static final ModifiableObjectProperty POLYLINE_PROPERTY = object("polyline")
        .description("Encapsulates an encoded polyline.")
        .properties(
            string("encodedPolyline")
                .description("Encoded polyline of the route."),
            object("goeJsonLinestring")
                .description("Geo JSON line string from polyline."));

    public static final ModifiableArrayProperty SPEED_READING_INTERVAL_PROPERTY = array("speedReadingInterval")
        .description("Traffic density indicator on a contiguous segment of a polyline or path.")
        .items(
            object("speedReadingInterval")
                .description("Traffic density indicator on a contiguous segment of a polyline or path.")
                .properties(
                    integer("startPolylinePointIndex")
                        .description("The starting index of this interval in the polyline."),
                    integer("endPolylinePointIndex")
                        .description("The ending index of this interval in the polyline."),
                    string("speed")
                        .description("Traffic speed in this interval.")));

    public static final ModifiableObjectProperty TOLL_INFO_PROPERTY = object("tollInfo")
        .description("Encapsulates toll information on a Route or on a RouteLeg.")
        .properties(
            array("estimatedPrice")
                .description("The monetary amount of tolls for the corresponding Route or RouteLeg.")
                .items(MONEY_PROPERTY));

    public static final ModifiableObjectProperty TRANSIT_STOP_PROPERTY = object("transitStop")
        .properties(
            string("name")
                .description("The name of the transit stop."),
            object("location")
                .description("The location of the stop expressed in latitude/longitude coordinates.")
                .properties(LOCATION_PROPERTY));

    public static final ModifiableObjectProperty ROUTE_LEG_STEP_PROPERTY = object("routeLegStep")
        .properties(
            integer("distanceMeters")
                .description(
                    "The travel distance of this step, in meters. In some circumstances, this field might not have " +
                        "a value."),
            string("staticDuration")
                .description(
                    "The duration of travel through this step without taking traffic conditions into consideration."),
            POLYLINE_PROPERTY,
            START_LOCATION_PROPERTY,
            END_LOCATION_PROPERTY,
            NAVIGATION_INSTRUCTIONS_PROPERTY,
            object("travelAdvisory")
                .description(
                    "Contains the additional information that the user should be informed about, such as possible " +
                        "traffic zone restrictions, on a leg step.")
                .properties(SPEED_READING_INTERVAL_PROPERTY),
            object("routeLegStepLocalizedValues")
                .description("Text representation of properties of the RouteLegStep.")
                .properties(
                    object("distance")
                        .description("Travel distance represented in text format.")
                        .properties(LOCALIZED_TEXT_PROPERTY),
                    object("staticDuration")
                        .description(
                            "Duration without taking traffic conditions into consideration, represented in the text " +
                                "form.")
                        .properties(LOCALIZED_TEXT_PROPERTY)),
            object("transitDetails")
                .description("Details pertaining to this step if the travel mode is TRANSIT.")
                .properties(
                    object("stopDetails")
                        .description("Information about the arrival and departure for the step.")
                        .properties(
                            object("arrivalStop")
                                .description("Information about the arrival stop for the step.")
                                .properties(TRANSIT_STOP_PROPERTY),
                            string("arrivalTime")
                                .description("The estimated time of arrival for the step."),
                            object("departureStop")
                                .description("Information about the departure stop for the step.")
                                .properties(TRANSIT_STOP_PROPERTY),
                            string("departureTime")
                                .description("The estimated time of departure for the step.")),
                    object("localizedValues")
                        .description("Text representation of properties of the RouteLegStepTransitDetails.")
                        .properties(
                            object("arrivalTime")
                                .description(
                                    "Time in its formatted text representation with a corresponding time zone.")
                                .properties(LOCALIZED_TIME_PROPERTY),
                            object("departureTime")
                                .description(
                                    "Time in its formatted text representation with a corresponding time zone.")
                                .properties(LOCALIZED_TIME_PROPERTY)),
                    string("headsign")
                        .description(
                            "Specifies the direction in which to travel on this line as marked on the vehicle or at " +
                                "the departure stop. The direction is often the terminus station."),
                    string("headway")
                        .description(
                            "Specifies the expected time as a duration between departures from the same stop at this " +
                                "time. For example, with a headway seconds value of 600, you would expect a ten " +
                                "minute wait if you should miss your bus."),
                    object("transitLine")
                        .description("Information about the transit line used in this step.")
                        .properties(
                            array("agencies")
                                .description("The transit agency (or agencies) that operates the transit line.")
                                .items(
                                    object("transitAgency")
                                        .description("A transit agency that operates a transit line.")
                                        .properties(
                                            string("name")
                                                .description("The name of this transit agency."),
                                            string("phoneNumber")
                                                .description(
                                                    "The transit agency's locale-specific formatted phone number."),
                                            string("uri")
                                                .description("The transit agency's URI."))),
                            string("name")
                                .description("The full name of the transit line."),
                            string("uri")
                                .description("The URI for this transit line as provided by the transit agency."),
                            string("color")
                                .description(
                                    "The color commonly used in signage for this line. Represented in hexadecimal."),
                            string("iconUri")
                                .description("The URI for the icon associated with this line."),
                            string("nameShort")
                                .description("The short name of this transit line."),
                            string("textColor")
                                .description(
                                    "The color commonly used in text on signage for this line. Represented in " +
                                        "hexadecimal."),
                            object("vehicle")
                                .description("The type of vehicle that operates on this transit line.")
                                .properties(
                                    object("transitVehicle")
                                        .properties(
                                            object("name")
                                                .description("The name of this vehicle, capitalized.")
                                                .properties(LOCALIZED_TEXT_PROPERTY),
                                            string("type")
                                                .description("The type of vehicle used."),
                                            string("iconUri")
                                                .description("The URI for an icon associated with this vehicle type."),
                                            string("localIconUri")
                                                .description(
                                                    "The URI for the icon associated with this vehicle type, based " +
                                                        "on the local transport signage.")))),
                    integer("stopCount")
                        .description("The number of stops from the departure to the arrival stop."),
                    string("tripShortText")
                        .description(
                            "The text that appears in schedules and sign boards to identify a transit trio to " +
                                "passengers.")),
            string("travelMode")
                .description("The travel mode used for this step"));

    public static final ModifiableObjectProperty ROUTE_PROPERTY = object("route")
        .description("Route object.")
        .properties(
            array("routeLabels")
                .description(
                    "Labels for the Route that are useful to identify specific properties of the route to compare " +
                        "against others.")
                .items(string()),
            array("legs")
                .description("A collection of legs (path segments between waypoints) that make up the route.")
                .items(
                    object()
                        .properties(
                            integer("distanceMeters")
                                .description("Distance from origin and destination in meters."),
                            string("duration")
                                .description("Duration of the route in seconds."),
                            string("staticDuration")
                                .description(
                                    "The duration of travel through the leg, calculated without taking traffic " +
                                        "conditions into consideration."),
                            POLYLINE_PROPERTY,
                            START_LOCATION_PROPERTY,
                            END_LOCATION_PROPERTY),
                    array("steps")
                        .description(
                            "An array of steps denoting segments within this leg. Each step represents one " +
                                "navigation instruction.")
                        .items(ROUTE_LEG_STEP_PROPERTY),
                    object("travelAdvisory")
                        .description(
                            "Contains the additional information that the user should be informed about, " +
                                "such as possible traffic zone restrictions, on a route leg.")
                        .properties(
                            object("routeLegTravelAdvisory")
                                .properties(
                                    TOLL_INFO_PROPERTY,
                                    SPEED_READING_INTERVAL_PROPERTY)),
                    object("localizedValuesRouteLeg")
                        .description("Text representations of certain properties.")
                        .properties(
                            object("distance")
                                .description("Distance of the route.")
                                .properties(LOCALIZED_TEXT_PROPERTY),
                            object("duration")
                                .description("Duration of the route.")
                                .properties(LOCALIZED_TEXT_PROPERTY),
                            object("staticDuration")
                                .description("Static duration of the route.")
                                .properties(LOCALIZED_TEXT_PROPERTY)),
                    object("stepsOverview")
                        .description("Provides overview information about a list of RouteLegStep's.")
                        .properties(
                            array("multiModalSegments")
                                .description(
                                    "Summarized information about different multi-modal segments of the " +
                                        "RouteLeg.steps.")
                                .items(
                                    object("multiModalSegment")
                                        .properties(
                                            NAVIGATION_INSTRUCTIONS_PROPERTY,
                                            string("travelMode")
                                                .description("The travel mode of the multi-modal segment."),
                                            integer("stepStartIndex")
                                                .description(
                                                    "The corresponding RouteLegStep index that is the start " +
                                                        "of a multi-modal segment."),
                                            integer("stepEndIndex")
                                                .description(
                                                    "The corresponding RouteLegStep index that is the end " +
                                                        "of a multi-modal segment."))))),
            integer("distanceMeters")
                .description("The travel distance of the route, in meters."),
            string("duration")
                .description("The length of time needed to navigate the route."),
            string("staticDuration")
                .description(
                    "The duration of travel through the route without taking traffic conditions into consideration."),
            POLYLINE_PROPERTY,
            string("description")
                .description("Description of the route."),
            array("warnings")
                .description("An array of warnings to show when displaying the route.")
                .items(string()),
            object("viewport")
                .description("The viewport bounding box of the polyline.")
                .properties(
                    object("low")
                        .properties(LAT_LNG_PROPERTY),
                    object("high")
                        .properties(LAT_LNG_PROPERTY)),
            object("travelAdvisory")
                .description(
                    "Contains the additional information that the user should be informed about, such as possible " +
                        "traffic zone restrictions, on a route leg.")
                .properties(
                    object("routeTravelAdvisory")
                        .properties(
                            TOLL_INFO_PROPERTY,
                            SPEED_READING_INTERVAL_PROPERTY,
                            string("fuelConsumptionMicroliters")
                                .description("The predicted fuel consumption in microliters."),
                            bool("routeRestrictionsPartiallyIgnored")
                                .description(
                                    "Returned route may have restrictions that are not suitable for requested travel " +
                                        "mode or route modifiers."),
                            object("transitFare")
                                .description(
                                    "If present, contains the total fare or ticket costs on this route. This " +
                                        "property is only returned for TRANSIT requests and only for routes where " +
                                        "fare information is available for all transit steps.")
                                .properties(MONEY_PROPERTY))),
            array("optimizedIntermediateWaypointIndex")
                .description(
                    "If you set optimizeWaypointOrder to true, this field contains the optimized ordering of " +
                        "intermediate waypoints.")
                .items(integer()),
            object("localizedValuesRoute")
                .description("Text representations of certain properties.")
                .properties(
                    object("distance")
                        .description("Distance of the route.")
                        .properties(LOCALIZED_TEXT_PROPERTY),
                    object("duration")
                        .description("Duration of the route.")
                        .properties(LOCALIZED_TEXT_PROPERTY),
                    object("staticDuration")
                        .description("Static duration of the route.")
                        .properties(LOCALIZED_TEXT_PROPERTY),
                    object("transitFare")
                        .description("Transit fare represented in text form.")
                        .properties(LOCALIZED_TEXT_PROPERTY)),
            string("routeToken")
                .description(
                    "An opaque token that can be passed to Navigation SDK to reconstruct the route during " +
                        "navigation, and, in the event of rerouting, honor the original intention when the route was " +
                        "created."),
            object("polylineDetails")
                .description("Contains information about details along the polyline.")
                .properties(
                    array("flyoverInfo")
                        .description("Flyover details along the polyline.")
                        .items(
                            object("flyoverInfo")
                                .description("Encapsulates information about flyovers along the polyline.")
                                .properties(
                                    string("flyoverPresence")
                                        .description("Output only. Denotes whether a flyover exists for a given " +
                                            "stretch of the polyline."),
                                    POLYLINE_POINT_INDEX_PROPERTY)),
                    array("narrowRoadInfo")
                        .description("Narrow road details along the polyline.")
                        .items(
                            object("narrowRoadInfo")
                                .description("Encapsulates information about narrow roads along the polyline.")
                                .properties(
                                    string("narrowRoadPresence")
                                        .description(
                                            "Output only. Denotes whether a narrow road exists for a given stretch " +
                                                "of the polyline."),
                                    POLYLINE_POINT_INDEX_PROPERTY))));

    private GoogleMapsConstants() {
    }
}
