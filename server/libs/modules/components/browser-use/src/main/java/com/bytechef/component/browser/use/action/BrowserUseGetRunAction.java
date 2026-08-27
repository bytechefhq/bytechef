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

import static com.bytechef.component.browser.use.constant.BrowserUseConstants.RUN_ID;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.RUN_RESPONSE_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Magnus Müller
 */
public class BrowserUseGetRunAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getRun")
        .title("Get Run")
        .description("Get a Browser Use V4 run and its final result.")
        .help("", "https://docs.bytechef.io/reference/components/browser-use_v1#get-run")
        .properties(
            string(RUN_ID)
                .label("Run ID")
                .description("ID returned by Create Run.")
                .required(true))
        .output(outputSchema(RUN_RESPONSE_PROPERTY))
        .perform(BrowserUseGetRunAction::perform);

    private BrowserUseGetRunAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context
            .http(http -> http.get("/api/v4/runs/%s".formatted(inputParameters.getRequiredString(RUN_ID))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
