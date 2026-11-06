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

package com.bytechef.component.scrapingbee.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.SCREENSHOT;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.SCREENSHOT_FULL_PAGE;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.SCREENSHOT_SELECTOR;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.WINDOW_HEIGHT;
import static com.bytechef.component.scrapingbee.constant.ScrapingBeeConstants.WINDOW_WIDTH;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.scrapingbee.util.ScrapingBeeUtils;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class ScrapingBeeTakeScreenshotAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("takeScreenshot")
        .title("Take Screenshot")
        .description("Render a web page in a headless browser and return a PNG screenshot of it.")
        .properties(
            ScrapingBeeUtils.withRequestProperties(
                bool(SCREENSHOT_FULL_PAGE)
                    .label("Full Page")
                    .description("Capture the full height of the page instead of only the visible viewport.")
                    .defaultValue(false)
                    .required(false),
                string(SCREENSHOT_SELECTOR)
                    .label("Selector")
                    .description("CSS selector of a single element to capture instead of the whole page.")
                    .required(false),
                integer(WINDOW_WIDTH)
                    .label("Window Width")
                    .description("Width of the browser viewport in pixels. Defaults to 1920.")
                    .minValue(1)
                    .required(false),
                integer(WINDOW_HEIGHT)
                    .label("Window Height")
                    .description("Height of the browser viewport in pixels. Defaults to 1080.")
                    .minValue(1)
                    .required(false)))
        .output(
            outputSchema(
                fileEntry()
                    .description("The PNG screenshot of the page.")))
        .help("", "https://docs.bytechef.io/reference/components/scrapingbee_v1#take-screenshot")
        .perform(ScrapingBeeTakeScreenshotAction::perform);

    private ScrapingBeeTakeScreenshotAction() {
    }

    public static FileEntry perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Object> queryParameters = ScrapingBeeUtils.requestQueryParameters(inputParameters);

        queryParameters.addAll(
            Arrays.asList(
                SCREENSHOT, true,
                SCREENSHOT_FULL_PAGE, inputParameters.getBoolean(SCREENSHOT_FULL_PAGE),
                SCREENSHOT_SELECTOR, inputParameters.getString(SCREENSHOT_SELECTOR),
                WINDOW_WIDTH, inputParameters.getInteger(WINDOW_WIDTH),
                WINDOW_HEIGHT, inputParameters.getInteger(WINDOW_HEIGHT)));

        return context.http(http -> http.get("/"))
            .configuration(
                responseType(ResponseType.binary("image/png"))
                    .filename("screenshot.png"))
            .queryParameters(queryParameters.toArray())
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
