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

package com.bytechef.component.nasa.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class NasaGetAsteroidAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getAsteroid")
        .title("Get Asteroid")
        .description("Lookup a specific Asteroid based on its NASA JPL small body (SPK-ID) ID.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/neo/rest/v1/neo/{asteroidId}"

            ))
        .properties(integer("asteroidId").label("Asteroid Id")
            .description("Asteroid SPK-ID correlates to the NASA JPL small body.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(string("id").description("The unique identifier of the asteroid.")
            .required(false),
            string("neo_reference_id").description("NASA's Near Earth Object reference ID for the asteroid.")
                .required(false),
            string("name").description("The official name or designation of the asteroid.")
                .required(false),
            string("nasa_jpl_url").description(
                "A URL to the NASA JPL page with detailed information about the asteroid.")
                .required(false),
            number("absolute_magnitude_h").description(
                "The absolute magnitude of the asteroid, representing its intrinsic brightness.")
                .required(false),
            bool("is_potentially_hazardous_asteroid").description(
                "Indicates whether the asteroid is considered potentially hazardous to Earth.")
                .required(false),
            object("estimated_diameter").properties(object("kilometers")
                .properties(number("estimated_diameter_min")
                    .description("The minimum estimated diameter of the asteroid in kilometers.")
                    .required(false),
                    number("estimated_diameter_max")
                        .description("The maximum estimated diameter of the asteroid in kilometers.")
                        .required(false))
                .description("Estimated diameter range expressed in kilometers.")
                .required(false))
                .description("Estimated diameter measurements of the asteroid in various units.")
                .required(false),
            array("close_approach_data")
                .items(object()
                    .properties(string("close_approach_date")
                        .description("The date when the asteroid makes its close approach.")
                        .required(false),
                        object("relative_velocity")
                            .properties(
                                string("kilometers_per_second")
                                    .description("The relative velocity in kilometers per second.")
                                    .required(false),
                                string("kilometers_per_hour")
                                    .description("The relative velocity in kilometers per hour.")
                                    .required(false))
                            .description("The velocity of the asteroid relative to the body it approaches.")
                            .required(false),
                        object("miss_distance")
                            .properties(string("kilometers").description("The miss distance measured in kilometers.")
                                .required(false),
                                string("lunar")
                                    .description(
                                        "The miss distance expressed in lunar distances (distance from Earth to Moon).")
                                    .required(false))
                            .description("The distance by which the asteroid misses the celestial body.")
                            .required(false),
                        string("orbiting_body")
                            .description("The celestial body that the asteroid is approaching (e.g., Earth).")
                            .required(false))
                    .description("A list of records describing close approaches of the asteroid to celestial bodies."))
                .description("A list of records describing close approaches of the asteroid to celestial bodies.")
                .required(false),
            object("orbital_data")
                .properties(string("orbit_id").description("A unique identifier for the asteroid's orbit solution.")
                    .required(false),
                    string("eccentricity")
                        .description("A measure of how much the orbit deviates from a perfect circle.")
                        .required(false),
                    string("semi_major_axis").description(
                        "The semi-major axis of the asteroid's orbit, representing its average distance from the Sun.")
                        .required(false),
                    string("inclination")
                        .description("The tilt of the asteroid's orbit relative to the plane of the solar system.")
                        .required(false),
                    string("orbital_period")
                        .description("The time it takes for the asteroid to complete one full orbit around the Sun.")
                        .required(false))
                .description("Orbital parameters describing the asteroid's trajectory.")
                .required(false))
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/nasa_v1#get-asteroid");

    private NasaGetAsteroidAction() {
    }
}
