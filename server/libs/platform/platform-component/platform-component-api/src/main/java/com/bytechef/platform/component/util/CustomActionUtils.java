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

package com.bytechef.platform.component.util;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.nullable;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.time;
import static com.bytechef.component.definition.Context.Http;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.RequestMethod;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Help;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class CustomActionUtils {

    private static final String BODY_CONTENT = "bodyContent";
    private static final String BODY_CONTENT_MIME_TYPE = "bodyContentMimeType";
    private static final String BODY_CONTENT_TYPE = "bodyContentType";
    private static final String HEADERS = "headers";
    private static final String METHOD = "method";
    private static final String QUERY_PARAMETERS = "queryParameters";
    private static final String PATH = "path";
    public static final String RESPONSE_FILENAME = "responseFilename";
    public static final String RESPONSE_FORMAT = "responseType";

    public static ActionDefinition getCustomActionDefinition(ComponentDefinition componentDefinition) {
        ModifiableActionDefinition customActionDefinition = action("customAction")
            .title("Custom Action")
            .description(
                "By using custom actions, you can take advantage of the existing connector platform to create new actions.")
            .properties(
                string(PATH)
                    .label("Path")
                    .description("The relative URI that will be appended to the end of the base URI.")
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
                object(HEADERS)
                    .label("Headers")
                    .description("Headers to send.")
                    .placeholder("Add header")
                    .additionalProperties(
                        array()
                            .items(
                                string())),
                object(QUERY_PARAMETERS)
                    .label("Query Parameters")
                    .description("Query parameters to send.")
                    .placeholder("Add parameter")
                    .additionalProperties(
                        array()
                            .items(
                                string())),
                string(BODY_CONTENT_TYPE)
                    .label("Body Content Type")
                    .description("Content-Type to use when sending body parameters.")
                    .displayCondition(
                        "contains({'%s','%s','%s'}, %s)".formatted(
                            RequestMethod.PATCH.name(), RequestMethod.POST.name(),
                            RequestMethod.PUT.name(), METHOD))
                    .options(
                        option("JSON", BodyContentType.JSON.name()),
                        option("XML", BodyContentType.XML.name()),
                        option("Form-Data", BodyContentType.FORM_DATA.name()),
                        option("Form-Urlencoded", BodyContentType.FORM_URL_ENCODED.name()),
                        option("Raw", BodyContentType.RAW.name()),
                        option("Binary", BodyContentType.BINARY.name())),
                object(BODY_CONTENT)
                    .label("Body Content - JSON")
                    .description("Body Parameters to send.")
                    .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.JSON.name()))
                    .additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                        time())
                    .placeholder("Add Parameter"),
                object(BODY_CONTENT)
                    .label("Body Content - XML")
                    .description("XML content to send.")
                    .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.XML.name()))
                    .additionalProperties(
                        array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(),
                        time())
                    .placeholder("Add Parameter"),
                object(BODY_CONTENT)
                    .label("Body Content - Form Data")
                    .description("Body parameters to send.")
                    .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.FORM_DATA.name()))
                    .placeholder("Add Parameter")
                    .additionalProperties(string(), fileEntry()),
                object(BODY_CONTENT)
                    .label("Body Content - Form URL-Encoded")
                    .description("Body parameters to send.")
                    .displayCondition(
                        "%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.FORM_URL_ENCODED.name()))
                    .placeholder("Add Parameter")
                    .additionalProperties(string()),
                string(BODY_CONTENT)
                    .label("Body Content - Raw")
                    .description("The raw text to send.")
                    .displayCondition("%s == '%s'".formatted(BODY_CONTENT_TYPE, BodyContentType.RAW.name()))
                    .controlType(Property.ControlType.TEXT_AREA),
                string(BODY_CONTENT_MIME_TYPE)
                    .label("Content Type")
                    .description("Mime-Type to use when sending raw body content.")
                    .displayCondition(
                        "'%s' == %s or '%s' == %s".formatted(
                            BodyContentType.BINARY.name(), BODY_CONTENT_TYPE,
                            BodyContentType.RAW.name(), BODY_CONTENT_TYPE))
                    .defaultValue("text/plain")
                    .placeholder("text/plain"),
                string(RESPONSE_FORMAT)
                    .label("Response Format")
                    .description("The format in which the data gets returned from the URL.")
                    .options(
                        option(
                            "JSON", String.valueOf(ResponseType.JSON.getType()),
                            "The response is automatically converted to object/array."),
                        option(
                            "XML", String.valueOf(ResponseType.XML.getType()),
                            "The response is automatically converted to object/array."),
                        option(
                            "Text",
                            String.valueOf(ResponseType.TEXT.getType()), "The response is returned as a text."),
                        option(
                            "File",
                            String.valueOf(ResponseType.BINARY.getType()),
                            "The response is returned as a file object."))
                    .defaultValue(String.valueOf(ResponseType.JSON.getType())),
                string(RESPONSE_FILENAME)
                    .label("Response Filename")
                    .description("The name of the file if the response is returned as a file object.")
                    .displayCondition("%s == '%s'".formatted(RESPONSE_FORMAT, ResponseType.BINARY.getType())))
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
            .configuration(
                Http.responseType(ResponseType.JSON)
                    .responseType(getResponseType(inputParameters))
                    .filename(inputParameters.getString(RESPONSE_FILENAME)))
            .headers(MapUtils.toMap(headers, Map.Entry::getKey, entry -> List.of((String) entry.getValue())))
            .queryParameters(
                MapUtils.toMap(queryParameters, Map.Entry::getKey, entry -> List.of((String) entry.getValue())))
            .body(
                getBody(MapUtils.get(inputParameters, BODY_CONTENT_TYPE, BodyContentType.class, null), inputParameters))
            .execute();
    }

    private static Body getBody(BodyContentType bodyContentType, Map<String, ?> inputParameters) {
        Body body = null;

        if (bodyContentType != null) {
            if (bodyContentType == BodyContentType.BINARY) {
                body = Body.of(
                    MapUtils.getRequired(inputParameters, BODY_CONTENT, FileEntry.class),
                    MapUtils.getString(inputParameters, BODY_CONTENT_MIME_TYPE));
            } else if (bodyContentType == BodyContentType.FORM_DATA) {
                body = Body.of(
                    MapUtils.getMap(inputParameters, BODY_CONTENT, List.of(FileEntry.class), Map.of()),
                    bodyContentType);
            } else if (bodyContentType == BodyContentType.FORM_URL_ENCODED) {
                body = Body.of(MapUtils.getMap(inputParameters, BODY_CONTENT, Map.of()), bodyContentType);
            } else if (bodyContentType == BodyContentType.JSON || bodyContentType == BodyContentType.XML) {
                body = Body.of(MapUtils.getMap(inputParameters, BODY_CONTENT, Map.of()), bodyContentType);
            } else if (bodyContentType == BodyContentType.RAW) {
                body = Body.of(
                    MapUtils.getString(inputParameters, BODY_CONTENT),
                    MapUtils.getString(inputParameters, BODY_CONTENT_MIME_TYPE, "text/plain"));
            }
        }

        return body;
    }

    private static ResponseType getResponseType(Parameters inputParameters) {
        return inputParameters.containsKey(RESPONSE_FORMAT)
            ? ResponseType.valueOf(inputParameters.getString(RESPONSE_FORMAT)) : ResponseType.JSON;
    }
}
