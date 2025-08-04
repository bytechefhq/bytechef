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

package com.bytechef.component.wolfram.alpha.full.results.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class WolframAlphaFullResultsGetFullResultAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getFullResult")
        .title("Get Full Result")
        .description("Returns a full result of your query.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/query", "responseType", ResponseType.XML

            ))
        .properties(string("input").label("Query")
            .description("Query that will be answered.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.QUERY)),
            array("format").items(string().description("The desired format for individual result pods.")
                .options(option("Image", "image"), option("Imagemap", "imagemap"), option("Plaintext", "plaintext"),
                    option("Minput", "minput"), option("Moutput", "moutput"), option("Cell", "cell"),
                    option("Mathml", "mathml"), option("Sound", "sound"), option("Wav", "wav")))
                .placeholder("Add to Format")
                .label("Format")
                .description("The desired format for individual result pods.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("output").label("Output")
                .description("The desired format for full results.")
                .options(option("Xml", "xml"), option("Json", "json"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("ip").label("IP")
                .description("Specifies a custom query location based on an IP address.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("mag").label("Magnification")
                .description("Specify magnification of objects within a pod.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("units").label("Units")
                .description("What system of units to use for measurements and quantities.")
                .options(option("Metric", "metric"), option("Imperial", "imperial"))
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("width").label("Width")
                .description("Specify an approximate width limit for text and tables.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("scanner").label("Scanner")
                .description(
                    "Specifies that only pods produced by the given scanner should be returned. e.g. \"Numeric\", \"Data\", \"Traveling\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("latlong").label("Latitude Longitude")
                .description(
                    "Specifies a custom query location based on a latitude/longitude pair. e.g. \"40.42,-3.71\", \"40.11, -88.24\", \"0,0\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("location").label("Location")
                .description(
                    "Specifies a custom query location based on a string. e.g. \"The North Pole\", \"Beijing\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("maxwidth").label("Max Width")
                .description("Specify an extended maximum width for large objects.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("podstate").label("Pod State")
                .description(
                    "Specifies a pod state change, which replaces a pod with a modified version, such as displaying more digits of a large decimal value. e.g. \"WeatherCharts:WeatherData__Past+5+years\", \"2@DecimalApproximation__More+digits\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("podtitle").label("Pod Title")
                .description(
                    "Specifies a pod title to include in the result. e.g. \"Basic+Information\", \"Image\", \"Alternative representations\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("assumption").label("Assumption")
                .description(
                    "Specifies an assumption, such as the meaning of a word or the value of a formula variable. e.g. \"*C.pi-_*Movie\", \"DateOrder_**Day.Month.Year--\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("plotwidth").label("Plot Width")
                .description("Specify an approximate width limit for plots and graphics. e.g. \"100\", \"200\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            bool("ignorecase").label("Ignore Case")
                .description("Force Wolfram Alpha to ignore case in queries.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("podtimeout").label("Pod Timeout")
                .description(
                    "The number of seconds to allow Wolfram Alpha to spend in the \"format\" stage for any one pod e.g. \"0.5\", \"5.0\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            bool("reinterpret").label("Reinterpret")
                .description("Allow Wolfram Alpha to reinterpret queries that would otherwise not be understood.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            bool("translation").label("Translation")
                .description("Allow Wolfram Alpha to try to translate simple queries into English.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("scantimeout").label("Scan Timeout")
                .description(
                    "The number of seconds to allow Wolfram Alpha to compute results in the \"scan\" stage of processing. e.g. \"0.5\", \"5.0\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("parsetimeout").label("Parse Timeout")
                .description(
                    "The number of seconds to allow Wolfram Alpha to spend in the \"parsing\" stage of processing. e.g. \"0.5\", \"5.0\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("totaltimeout").label("Total Timeout")
                .description(
                    "The total number of seconds to allow Wolfram Alpha to spend on a query. e.g. \"0.5\", \"5.0\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("excludepodid").label("Exclude Pod ID")
                .description(
                    "Specifies a pod ID to exclude from the result e.g. \"Result\", \"BasicInformation: PeopleData\", \"DecimalApproximation\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("formattimeout").label("Format Timeout")
                .description(
                    "The number of seconds to allow Wolfram Alpha to spend in the \"format\" stage for the entire collection of pods. e.g. \"0.5\", \"5.0\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("includepodid").label("Include Pod ID")
                .description(
                    "Specifies a pod ID to include in the result e.g. \"Result\", \"BasicInformation: PeopleData\", \"DecimalApproximation\".")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output();

    private WolframAlphaFullResultsGetFullResultAction() {
    }
}
