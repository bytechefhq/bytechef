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

package com.bytechef.component.one.simple.api.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.CUSTOM_CSS;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.CUSTOM_SIZE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.ELAPSED;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FORCE_REFRESH;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.FULL_PAGE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.HEIGHT;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.HTML;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.SCREEN_SIZE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.SCREEN_SIZE_OPTIONS;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.SOURCE;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.TRANSPARENT_BACKGROUND;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.URL;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.WAIT;
import static com.bytechef.component.one.simple.api.constants.OneSimpleAPIConstants.WIDTH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class OneSimpleAPIAddScreenshotAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addScreenshot")
        .title("Add Screenshot")
        .description("Turn a URL into a screenshot.")
        .properties(
            string(SOURCE)
                .label("Source")
                .description("Provide either a URL to capture or raw HTML content.")
                .options(
                    option("URL", "URL"),
                    option("HTML", "HTML"))
                .required(true),
            string(URL)
                .label("URL")
                .description("Place the URL you want to turn into screenshot.")
                .displayCondition("%s == '%s'".formatted(SOURCE, "URL"))
                .required(true),
            string(HTML)
                .label("HTML")
                .description("Place the raw HTML to render.")
                .displayCondition("%s == '%s'".formatted(SOURCE, "HTML"))
                .required(true),
            string(CUSTOM_CSS)
                .label("Custom CSS")
                .description("Custom CSS to inject into the page.")
                .required(false),
            integer(WAIT)
                .label("Wait")
                .description("Time to wait before capturing (milliseconds).")
                .required(false),
            string(SCREEN_SIZE)
                .label("Screen size")
                .description("Predefined screen size or custom dimensions.")
                .placeholder("Default (1920x1080)")
                .options(SCREEN_SIZE_OPTIONS)
                .required(false),
            integer(WIDTH)
                .label("Width (px)")
                .displayCondition("%s == '%s'".formatted(SCREEN_SIZE, CUSTOM_SIZE))
                .placeholder("1920")
                .required(true),
            integer(HEIGHT)
                .label("Height (px)")
                .displayCondition("%s == '%s'".formatted(SCREEN_SIZE, CUSTOM_SIZE))
                .placeholder("1080")
                .required(true),
            bool(TRANSPARENT_BACKGROUND)
                .label("Transparent background")
                .description("Make the background transparent (PNG only).")
                .required(false),
            bool(FULL_PAGE)
                .label("Full Page Screenshot")
                .description("Capture the entire scrollable page.")
                .required(false),
            bool(FORCE_REFRESH)
                .label("Force refresh")
                .description("Force a new screenshot even if cached.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer(WIDTH)
                            .description("Screenshot width."),
                        integer(HEIGHT)
                            .description("Screenshot height."),
                        string(FULL_PAGE)
                            .description("Whether the screenshot was captured as full page."),
                        string(URL)
                            .description("The URL that was captured."),
                        integer(ELAPSED)
                            .description("The total time taken to generate the screenshot."))))
        .perform(OneSimpleAPIAddScreenshotAction::perform);

    public static Object
        perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> body = getBody(inputParameters);

        return context.http(http -> http.post("/screenshot"))
            .body(Http.Body.of(body))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> getBody(Parameters inputParameters) {

        Map<String, Object> body = new HashMap<>();

        addFields(body, URL, inputParameters.getString(URL));
        addFields(body, HTML, inputParameters.getString(HTML));
        addFields(body, CUSTOM_CSS, inputParameters.getString(CUSTOM_CSS));
        addFields(body, WAIT, inputParameters.getInteger(WAIT));
        addScreenSize(body, inputParameters);
        addFields(body, TRANSPARENT_BACKGROUND, inputParameters.getBoolean(TRANSPARENT_BACKGROUND));
        addFields(body, FULL_PAGE, inputParameters.getBoolean(FULL_PAGE));
        addFields(body, FORCE_REFRESH, inputParameters.getBoolean(FORCE_REFRESH));

        return body;
    }

    private static void addFields(Map<String, Object> body, String fieldName, Object value) {
        if (value != null) {
            body.put(fieldName, value);
        }
    }

    private static void addScreenSize(Map<String, Object> body, Parameters inputParameters) {
        String screen = inputParameters.getString(SCREEN_SIZE);

        if (CUSTOM_SIZE.equals(screen)) {
            body.put(SCREEN_SIZE,
                inputParameters.getInteger(WIDTH) + "x" +
                    inputParameters.getInteger(HEIGHT));
        } else if (!"default".equals(screen) && screen != null) {
            body.put(SCREEN_SIZE, screen);
        }
    }
}
