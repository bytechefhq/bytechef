/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.util;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.RequestMethod;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

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
    private static final String PATH = "path";

    public static ActionDefinition getCustomActionDefinition(ComponentDefinition componentDefinition) {
        ModifiableActionDefinition customActionDefinition = ComponentDSL.action(CUSTOM, componentDefinition)
            .title("Custom Action")
            .description(
                "By using custom actions, you can take advantage of the existing connector platform to create new actions.")
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
                        option(Http.RequestMethod.DELETE.name(), Http.RequestMethod.DELETE.name()),
                        option(Http.RequestMethod.GET.name(), Http.RequestMethod.GET.name()),
                        option(Http.RequestMethod.PATCH.name(), Http.RequestMethod.PATCH.name()),
                        option(Http.RequestMethod.POST.name(), Http.RequestMethod.POST.name()),
                        option(Http.RequestMethod.PUT.name(), Http.RequestMethod.PUT.name()))
                    .required(true)
                    .defaultValue(Http.RequestMethod.GET.name()),

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
                            Http.RequestMethod.PATCH.name(), Http.RequestMethod.POST.name(),
                            Http.RequestMethod.PUT.name(), METHOD))
                    .options(
                        option("None", ""),
                        option("JSON", Http.BodyContentType.JSON.name()),
                        option("Form-Data", Http.BodyContentType.FORM_DATA.name()),
                        option("Form-Urlencoded", Http.BodyContentType.FORM_URL_ENCODED.name()),
                        option("Raw", Http.BodyContentType.RAW.name()))
                    .defaultValue(""),

                object(BODY_CONTENT)
                    .label("Body Content - JSON")
                    .description("Body Parameters to send.")
                    .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, Http.BodyContentType.JSON.name()))
                    .additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                        time())
                    .placeholder("Add Parameter"),
                object(BODY_CONTENT)
                    .label("Body Content - XML")
                    .description("XML content to send.")
                    .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, Http.BodyContentType.XML.name()))
                    .additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                        time())
                    .placeholder("Add Parameter"),
                object(BODY_CONTENT)
                    .label("Body Content - Form Data")
                    .description("Body parameters to send.")
                    .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, Http.BodyContentType.FORM_DATA.name()))
                    .placeholder("Add Parameter")
                    .additionalProperties(string(), fileEntry()),
                object(BODY_CONTENT)
                    .label("Body Content - Form URL-Encoded")
                    .description("Body parameters to send.")
                    .displayCondition(
                        "%s == '%s'".formatted(BODY_CONTENT_TYPE, Http.BodyContentType.FORM_URL_ENCODED.name()))
                    .placeholder("Add Parameter")
                    .additionalProperties(string()),
                string(BODY_CONTENT)
                    .label("Body Content - Raw")
                    .description("The raw text to send.")
                    .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, Http.BodyContentType.RAW.name())),

                string(BODY_CONTENT_MIME_TYPE)
                    .label("Content Type")
                    .description("Mime-Type to use when sending raw body content.")
                    .displayCondition("'%s' == '%s'".formatted(Http.BodyContentType.RAW.name(), BODY_CONTENT_TYPE))
                    .defaultValue("text/plain")
                    .placeholder("text/plain"))
            .output()
            .perform(CustomActionUtils::perform);

        return customActionDefinition.help(
            OptionalUtils.mapOrElse(componentDefinition.getCustomActionHelp(), Help::getBody, null),
            OptionalUtils.orElse(
                OptionalUtils.mapOptional(
                    componentDefinition.getCustomActionHelp(),
                    help -> OptionalUtils.orElse(help.getLearnMoreUrl(), null)),
                null));
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, ?> headers = MapUtils.getMap(inputParameters, HEADERS, Map.of());
        Map<String, ?> queryParameters = MapUtils.getMap(inputParameters, QUERY_PARAMETERS, Map.of());

        return context.http(
            http -> http.exchange(
                MapUtils.getRequiredString(inputParameters, PATH),
                MapUtils.getRequired(inputParameters, METHOD, RequestMethod.class)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(getBody(MapUtils.get(inputParameters, BODY_CONTENT_TYPE, BodyContentType.class), inputParameters))
            .headers(MapUtils.toMap(headers, Map.Entry::getKey, entry -> List.of((String) entry.getValue())))
            .queryParameters(
                MapUtils.toMap(queryParameters, Map.Entry::getKey, entry -> List.of((String) entry.getValue())))
            .execute();
    }

    private static Body getBody(BodyContentType bodyContentType, Map<String, ?> inputParameters) {
        Body body = null;

        if (bodyContentType != null) {
            if (bodyContentType == Http.BodyContentType.RAW) {
                body = Http.Body.of(MapUtils.getRequiredString(inputParameters, BODY_CONTENT));
            } else {
                Http.Body.of(MapUtils.getRequiredMap(inputParameters, BODY_CONTENT));
            }
        }

        return body;
    }
}
