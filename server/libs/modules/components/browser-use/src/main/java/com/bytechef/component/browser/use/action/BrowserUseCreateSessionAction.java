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

package com.bytechef.component.browser.use.action;

import static com.bytechef.component.browser.use.constant.BrowserUseConstants.ENABLE_SCHEDULED_TASKS;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.KEEP_ALIVE;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.MODEL;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.OUTPUT_PROPERTY;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.OUTPUT_SCHEMA;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.SESSION_ID;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.SESSION_RESPONSE_PROPERTY;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.SKILLS;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.TASK;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.browser.use.util.BrowserUseUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BrowserUseCreateSessionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSession")
        .title("Create Session")
        .description("Create a session and/or dispatch a task.")
        .help("", "https://docs.bytechef.io/reference/components/browser-use_v1#create-session")
        .properties(
            string(TASK)
                .label("Task")
                .description("The natural-language instruction for the agent to execute.")
                .required(true),
            string(MODEL)
                .label("Model")
                .description("The model to use.")
                .options(
                    option("Claude Sonnet 4.6", "claude-sonnet-4.6"),
                    option("Claude Opus 4.6", "claude-opus-4.6"),
                    option("GPT-5.4 mini", "gpt-5.4-mini"))
                .required(true),
            OUTPUT_PROPERTY,
            string(SESSION_ID)
                .label("Session ID")
                .description("ID of an existing idle session to dispatch the task to.")
                .options((OptionsFunction<String>) BrowserUseUtils::getSessionIdOptions)
                .required(false),
            bool(KEEP_ALIVE)
                .label("Keep Alive")
                .description(
                    "If true, the session stays alive in idle state after the task completes instead of " +
                        "automatically stopping.")
                .required(false),
            bool(ENABLE_SCHEDULED_TASKS)
                .label("Enable Scheduled Tasks")
                .description("If true, the agent can create scheduled tasks that run on a recurring basis.")
                .required(false),
            bool(SKILLS)
                .label("Skills")
                .description("If true, enables built-in agent skills.")
                .required(false))
        .output(outputSchema(SESSION_RESPONSE_PROPERTY))
        .perform(BrowserUseCreateSessionAction::perform);

    private BrowserUseCreateSessionAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        Object outputSchema = null;
        Map<String, String> output = inputParameters.getRequiredMap(OUTPUT_SCHEMA, String.class);
        String format = output.get("format");

        if ("JSON".equals(format)) {
            outputSchema = context.json(json -> json.read(output.get("schema")));
        }

        return context.http(http -> http.post("/sessions"))
            .body(
                Body.of(
                    TASK, inputParameters.getRequiredString(TASK),
                    MODEL, inputParameters.getRequiredString(MODEL),
                    SESSION_ID, inputParameters.getString(SESSION_ID),
                    KEEP_ALIVE, inputParameters.getBoolean(KEEP_ALIVE),
                    OUTPUT_SCHEMA, outputSchema,
                    ENABLE_SCHEDULED_TASKS, inputParameters.getBoolean(ENABLE_SCHEDULED_TASKS),
                    SKILLS, inputParameters.getBoolean(SKILLS)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
