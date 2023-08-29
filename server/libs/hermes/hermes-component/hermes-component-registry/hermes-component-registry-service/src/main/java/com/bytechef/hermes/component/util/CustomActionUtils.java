
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.util;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.Help;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.util.HttpClientUtils.Body;
import com.bytechef.hermes.component.util.HttpClientUtils.BodyContentType;
import com.bytechef.hermes.component.util.HttpClientUtils.RequestMethod;
import com.bytechef.hermes.component.util.HttpClientUtils.ResponseType;
import com.bytechef.hermes.definition.Property.ControlType;
import com.bytechef.hermes.definition.Property.StringProperty.SampleDataType;

import java.util.List;
import java.util.Map;

import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.util.HttpClientUtils.responseType;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.date;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.nullable;
import static com.bytechef.hermes.definition.DefinitionDSL.number;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;
import static com.bytechef.hermes.definition.DefinitionDSL.time;

/**
 * @author Ivica Cardic
 */
public class CustomActionUtils {

    private static final String CUSTOM = "custom";

    private static final String BODY_CONTENT = "bodyContent";
    private static final String BODY_CONTENT_MIME_TYPE = "bodyContentMimeType";
    private static final String BODY_CONTENT_TYPE = "bodyContentType";
    private static final String HEADERS = "headers";
    private static final String METHOD = "method";
    private static final String QUERY_PARAMETERS = "queryParameters";
    private static final String OUTPUT_SCHEMA = "outputSchema";
    private static final String PATH = "path";

    public static ActionDefinition getCustomActionDefinition(ComponentDefinition componentDefinition) {
        ModifiableActionDefinition customActionDefinition = ComponentDSL.action(CUSTOM, componentDefinition)
            .title("Custom Action")
            .description(
                "By using custom actions, you can take advantage of the existing connector infrastructure to create new actions.")
            .properties(
                string(PATH)
                    .label("Path")
                    .description(
                        "The relative URI that will be appended to the end of the base URI. Do not prepend '/' in your relative URL.")
                    .required(true),
                string(METHOD)
                    .label("Method")
                    .description("The http method.")
                    .options(
                        option(RequestMethod.DELETE.name(), RequestMethod.DELETE.name()),
                        option(RequestMethod.GET.name(), RequestMethod.GET.name()),
                        option(RequestMethod.PATCH.name(), RequestMethod.PATCH.name()),
                        option(RequestMethod.POST.name(), RequestMethod.POST.name()),
                        option(RequestMethod.PUT.name(), RequestMethod.PUT.name()))
                    .required(true)
                    .defaultValue(RequestMethod.GET.name()),

                //
                // Header parameters properties
                //

                object(HEADERS)
                    .label("Headers")
                    .description("Headers to send.")
                    .placeholder("Add header")
                    .additionalProperties(string()),

                //
                // Query parameters properties
                //

                object(QUERY_PARAMETERS)
                    .label("Query Parameters")
                    .description("Query parameters to send.")
                    .placeholder("Add parameter")
                    .additionalProperties(string()),

                //
                // Body properties
                //

                string(BODY_CONTENT_TYPE)
                    .label("Body Content Type")
                    .description("Content-Type to use when sending body parameters.")
                    .displayCondition(
                        "['%s','%s','%s'].includes('%s')".formatted(
                            RequestMethod.PATCH.name(), RequestMethod.POST.name(), RequestMethod.PUT.name(), METHOD))
                    .options(
                        option("None", ""),
                        option("JSON", BodyContentType.JSON.name()),
                        option("Form-Data", BodyContentType.FORM_DATA.name()),
                        option("Form-Urlencoded", BodyContentType.FORM_URL_ENCODED.name()),
                        option("Raw", BodyContentType.RAW.name()))
                    .defaultValue(""),

                object(BODY_CONTENT)
                    .label("Body Content - JSON")
                    .description("Body Parameters to send.")
                    .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.JSON.name()))
                    .additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                        time())
                    .placeholder("Add Parameter"),
                object(BODY_CONTENT)
                    .label("Body Content - XML")
                    .description("XML content to send.")
                    .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.XML.name()))
                    .additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                        time())
                    .placeholder("Add Parameter"),
                object(BODY_CONTENT)
                    .label("Body Content - Form Data")
                    .description("Body parameters to send.")
                    .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.FORM_DATA.name()))
                    .placeholder("Add Parameter")
                    .additionalProperties(string(), fileEntry()),
                object(BODY_CONTENT)
                    .label("Body Content - Form URL-Encoded")
                    .description("Body parameters to send.")
                    .displayCondition(
                        "%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.FORM_URL_ENCODED.name()))
                    .placeholder("Add Parameter")
                    .additionalProperties(string()),
                string(BODY_CONTENT)
                    .label("Body Content - Raw")
                    .description("The raw text to send.")
                    .displayCondition("%s === '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.RAW.name())),

                string(BODY_CONTENT_MIME_TYPE)
                    .label("Content Type")
                    .description("Mime-Type to use when sending raw body content.")
                    .displayCondition(
                        "'%s' === '%s'".formatted(
                            BodyContentType.RAW.name(), BODY_CONTENT_TYPE))
                    .defaultValue("text/plain")
                    .placeholder("text/plain"),

                //
                // TODO Add support for using it in the editor
                //

                string(OUTPUT_SCHEMA)
                    .label("Output Schema")
                    .description(
                        "Please provide a description of the desired format for the API's output schema. This format will then be utilized to generate the data tree for the output.")
                    .controlType(ControlType.SCHEMA_DESIGNER)
                    .sampleDataType(SampleDataType.JSON))
            .outputSchema(getOutputSchemaFunction())
            .perform(CustomActionUtils::perform);

        return customActionDefinition.help(
            OptionalUtils.mapOrElse(componentDefinition.getCustomActionHelp(), Help::getBody, null),
            OptionalUtils.orElse(
                OptionalUtils.mapOptional(
                    componentDefinition.getCustomActionHelp(),
                    help -> OptionalUtils.orElse(help.getLearnMoreUrl(), null)),
                null));
    }

    protected static Object perform(Map<String, ?> inputParameters, ActionDefinition.ActionContext context) {
        Map<String, ?> headers = MapUtils.getMap(inputParameters, HEADERS, Map.of());
        Map<String, ?> queryParameters = MapUtils.getMap(inputParameters, QUERY_PARAMETERS, Map.of());

        return HttpClientUtils.exchange(
            MapUtils.getRequiredString(inputParameters, PATH),
            MapUtils.getRequired(inputParameters, METHOD, RequestMethod.class))
            .configuration(responseType(ResponseType.JSON))
            .body(getBody(MapUtils.get(inputParameters, BODY_CONTENT_TYPE, BodyContentType.class), inputParameters))
            .headers(MapUtils.toMap(headers, Map.Entry::getKey, entry -> List.of((String) entry.getValue())))
            .queryParameters(
                MapUtils.toMap(queryParameters, Map.Entry::getKey, entry -> List.of((String) entry.getValue())))
            .execute();
    }

    private static Body getBody(BodyContentType bodyContentType, Map<String, ?> inputParameters) {
        Body body = null;

        if (bodyContentType != null) {
            if (bodyContentType == BodyContentType.RAW) {
                body = Body.of(MapUtils.getRequiredString(inputParameters, BODY_CONTENT));
            } else {
                Body.of(MapUtils.getRequiredMap(inputParameters, BODY_CONTENT));
            }
        }

        return body;
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }
}
