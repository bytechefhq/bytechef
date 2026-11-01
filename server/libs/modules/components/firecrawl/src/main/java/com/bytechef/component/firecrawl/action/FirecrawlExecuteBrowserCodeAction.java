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
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LANGUAGE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SESSION_ID;
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
public class FirecrawlExecuteBrowserCodeAction {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final int REQUEST_TIMEOUT_MARGIN_SECONDS = 60;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("executeBrowserCode")
        .title("Execute Code in Browser Session")
        .description("Run Node.js, Python or Bash code in a browser session created by Create Browser Session.")
        .properties(
            string(SESSION_ID)
                .label("Session ID")
                .description("The ID of the browser session, returned by the Create Browser Session action.")
                .required(true),
            string(CODE)
                .label("Code")
                .description(
                    "Code to execute in the browser sandbox. In Node.js the Playwright page object is available " +
                        "as 'page'; in Bash, agent-browser CLI commands are available.")
                .controlType(ControlType.TEXT_AREA)
                .minLength(1)
                .maxLength(100000)
                .required(true),
            string(LANGUAGE)
                .label("Language")
                .description("Language of the code to execute.")
                .options(
                    option("Node.js", "node"),
                    option("Python", "python"),
                    option("Bash", "bash"))
                .defaultValue("node")
                .required(false),
            integer(TIMEOUT)
                .label("Timeout")
                .description("Maximum execution time in seconds, up to 300.")
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
                        string("stdout")
                            .description("Standard output of the execution."),
                        string("result")
                            .description("Standard output of the execution (alias for stdout)."),
                        string("stderr")
                            .description("Standard error output of the execution."),
                        integer("exitCode")
                            .description("Exit code of the executed process; 0 means success."),
                        bool("killed")
                            .description("Whether the process was killed because it hit the timeout."),
                        string("error")
                            .description("Error message if the code raised an exception."))))
        .help("", "https://docs.bytechef.io/reference/components/firecrawl_v1#execute-code-in-browser-session")
        .perform(FirecrawlExecuteBrowserCodeAction::perform);

    private FirecrawlExecuteBrowserCodeAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Integer timeout = inputParameters.getInteger(TIMEOUT);

        int timeoutSeconds = timeout == null ? DEFAULT_TIMEOUT_SECONDS : timeout;

        return context
            .http(http -> http.post("/interact/" + inputParameters.getRequiredString(SESSION_ID) + "/execute"))
            .body(
                Http.Body.of(
                    CODE, inputParameters.getRequiredString(CODE),
                    LANGUAGE, inputParameters.getString(LANGUAGE),
                    TIMEOUT, timeout))
            .configuration(
                Http.responseType(ResponseType.JSON)
                    .requestTimeout(Duration.ofSeconds(timeoutSeconds + REQUEST_TIMEOUT_MARGIN_SECONDS)))
            .execute()
            .getBody();
    }
}
