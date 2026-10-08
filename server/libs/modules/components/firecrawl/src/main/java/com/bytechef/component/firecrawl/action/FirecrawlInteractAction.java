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

package com.bytechef.component.firecrawl.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.CODE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.INTERACTION_TYPE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LANGUAGE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PROMPT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SCRAPE_ID;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TIMEOUT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.time.Duration;

/**
 * @author Ivica Cardic
 */
public class FirecrawlInteractAction {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int REQUEST_TIMEOUT_MARGIN_SECONDS = 60;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("interact")
        .title("Interact with Page")
        .description(
            "Continue working with a scraped page in a live browser session: click, fill forms, navigate and " +
                "extract dynamic content with a natural language prompt or code.")
        .properties(
            string(SCRAPE_ID)
                .label("Scrape ID")
                .description(
                    "The ID of the scrape job whose browser session to interact with, returned by the Scrape URL " +
                        "action in data.metadata.scrapeId.")
                .required(true),
            string(INTERACTION_TYPE)
                .label("Interaction Type")
                .description(
                    "Describe the task in natural language for the AI agent, or run code directly in the browser " +
                        "sandbox.")
                .options(
                    option("Prompt", PROMPT),
                    option("Code", CODE))
                .defaultValue(PROMPT)
                .required(true),
            string(PROMPT)
                .label("Prompt")
                .description("Natural language task for the AI agent, e.g. 'Search for iPhone 16 Pro Max'.")
                .controlType(ControlType.TEXT_AREA)
                .maxLength(10000)
                .displayCondition("%s == '%s'".formatted(INTERACTION_TYPE, PROMPT))
                .required(true),
            string(CODE)
                .label("Code")
                .description(
                    "Code to execute in the browser sandbox. In Node.js the Playwright page object is available " +
                        "as 'page'.")
                .controlType(ControlType.TEXT_AREA)
                .maxLength(100000)
                .displayCondition("%s == '%s'".formatted(INTERACTION_TYPE, CODE))
                .required(true),
            string(LANGUAGE)
                .label("Language")
                .description("Language of the code to execute.")
                .options(
                    option("Node.js", "node"),
                    option("Python", "python"),
                    option("Bash", "bash"))
                .defaultValue("node")
                .displayCondition("%s == '%s'".formatted(INTERACTION_TYPE, CODE))
                .required(false),
            integer(TIMEOUT)
                .label("Timeout")
                .description("Maximum execution time in seconds. Default is 30, maximum is 300.")
                .minValue(1)
                .maxValue(300)
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success")
                            .description("Whether the execution completed without errors."),
                        string("output")
                            .description("The AI agent's final answer. Only present when a prompt is used."),
                        string("result")
                            .description(
                                "The value of the last evaluated expression when running code, or the final page " +
                                    "snapshot when using a prompt."),
                        string("stdout")
                            .description("Standard output of the execution."),
                        string("stderr")
                            .description("Standard error output of the execution."),
                        integer("exitCode")
                            .description("Exit code of the execution; 0 means success."),
                        bool("killed")
                            .description("Whether the execution was terminated because it hit the timeout."),
                        string("cdpUrl")
                            .description("Chrome DevTools Protocol WebSocket URL of the browser session."),
                        string("liveViewUrl")
                            .description("Read-only live view URL of the browser session."),
                        string("interactiveLiveViewUrl")
                            .description("Live view URL that lets the viewer control the browser session."),
                        string("error")
                            .description("Error message when the execution failed."))))
        .help("", "https://docs.bytechef.io/reference/components/firecrawl_v1#interact-with-page")
        .perform(FirecrawlInteractAction::perform);

    private FirecrawlInteractAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        boolean codeInteraction = CODE.equals(inputParameters.getRequiredString(INTERACTION_TYPE));
        Integer timeout = inputParameters.getInteger(TIMEOUT);

        int timeoutSeconds = timeout == null ? DEFAULT_TIMEOUT_SECONDS : timeout;

        return context
            .http(http -> http.post("/scrape/" + inputParameters.getRequiredString(SCRAPE_ID) + "/interact"))
            .body(
                Http.Body.of(
                    PROMPT, codeInteraction ? null : inputParameters.getRequiredString(PROMPT),
                    CODE, codeInteraction ? inputParameters.getRequiredString(CODE) : null,
                    LANGUAGE, codeInteraction ? inputParameters.getString(LANGUAGE) : null,
                    TIMEOUT, timeout))
            .configuration(
                Http.responseType(ResponseType.JSON)
                    .requestTimeout(Duration.ofSeconds(timeoutSeconds + REQUEST_TIMEOUT_MARGIN_SECONDS)))
            .execute()
            .getBody();
    }
}
