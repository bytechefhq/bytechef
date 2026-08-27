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

import static com.bytechef.component.browser.use.constant.BrowserUseConstants.MODEL;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.RUN_CREATE_RESPONSE_PROPERTY;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.SESSION_ID;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.TASK;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Magnus Müller
 */
public class BrowserUseCreateRunAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRun")
        .title("Create Run")
        .description("Create a Browser Use V4 run.")
        .help("", "https://docs.bytechef.io/reference/components/browser-use_v1#create-run")
        .properties(
            string(TASK)
                .label("Task")
                .description("The natural-language instruction for the agent to execute.")
                .controlType(TEXT_AREA)
                .required(true),
            string(MODEL)
                .label("Model")
                .description("The V4 model to use.")
                .options(
                    option("GLM 5.2", "glm-5.2"),
                    option("Grok 4.5", "grok-4.5"),
                    option("Kimi K3", "kimi-k3"),
                    option("MiniMax M3", "minimax-m3"),
                    option("Claude Opus 4.7", "claude-opus-4.7"),
                    option("Claude Opus 4.8", "claude-opus-4.8"),
                    option("Claude Opus 5", "claude-opus-5"),
                    option("Claude Fable 5", "claude-fable-5"),
                    option("Claude Sonnet 5", "claude-sonnet-5"),
                    option("GPT-5.5", "gpt-5.5"),
                    option("GPT-5.6", "gpt-5.6"),
                    option("GPT-5.6 Sol", "gpt-5.6-sol"),
                    option("GPT-5.6 Terra", "gpt-5.6-terra"),
                    option("GPT-5.6 Luna", "gpt-5.6-luna"),
                    option("Gemini 3.6 Flash", "gemini-3.6-flash"),
                    option("Gemini 3.5 Flash", "gemini-3.5-flash"),
                    option("Gemini 3.1 Pro", "gemini-3.1-pro"),
                    option("Gemini 3 Flash", "gemini-3-flash"))
                .defaultValue("gpt-5.6-luna")
                .required(true),
            string(SESSION_ID)
                .label("Session ID")
                .description("Optional V4 conversation session to continue."))
        .output(outputSchema(RUN_CREATE_RESPONSE_PROPERTY))
        .perform(BrowserUseCreateRunAction::perform);

    private BrowserUseCreateRunAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context.http(http -> http.post("/api/v4/runs"))
            .body(
                Body.of(
                    TASK, inputParameters.getRequiredString(TASK),
                    MODEL, inputParameters.getRequiredString(MODEL),
                    SESSION_ID, inputParameters.getString(SESSION_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
