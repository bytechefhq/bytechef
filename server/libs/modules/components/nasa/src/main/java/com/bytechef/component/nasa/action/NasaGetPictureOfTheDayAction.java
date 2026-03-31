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
public class NasaGetPictureOfTheDayAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getPictureOfTheDay")
        .title("Get Astronomy Picture of the Day")
        .description("Returns NASA's Astronomy Picture of the Day.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/planetary/apod"

            ))
        .properties(date("date").label("Date")
            .description("The date of the APOD image to retrieve.")
            .required(false)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            date("start_date").label("Start Date")
                .description("The start of a date range.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            date("end_date").label("End Date")
                .description("The end of the date range.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            integer("count").label("Count")
                .description("If specified, returns a randomly chosen images.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            bool("thumbs").label("Thumbs")
                .description("Return the URL of video thumbnail.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object().properties(
            date("date").description("The date of the Astronomy Picture of the Day.")
                .required(false),
            string("explanation").description("A detailed explanation of the image or video provided by NASA.")
                .required(false),
            string("media_type").description("The type of media returned (e.g., 'image' or 'video').")
                .required(false),
            string("service_version").description("The version of the NASA API service used to generate this response.")
                .required(false),
            string("title").description("The title of the Astronomy Picture of the Day.")
                .required(false),
            string("url").description("The URL where the standard resolution image or video can be accessed.")
                .required(false),
            string("hdurl").description("The URL for the high-definition version of the image, if available.")
                .required(false))
            .additionalProperties(
                array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))))
        .help("", "https://docs.bytechef.io/reference/components/nasa_v1#get-astronomy-picture-of-the-day");

    private NasaGetPictureOfTheDayAction() {
    }
}
