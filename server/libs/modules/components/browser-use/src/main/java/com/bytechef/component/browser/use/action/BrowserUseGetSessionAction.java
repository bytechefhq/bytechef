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

import static com.bytechef.component.browser.use.constant.BrowserUseConstants.SESSION_ID;
import static com.bytechef.component.browser.use.constant.BrowserUseConstants.SESSION_RESPONSE_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.browser.use.util.BrowserUseUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class BrowserUseGetSessionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getSession")
        .title("Get Session")
        .description("Get session details.")
        .help("", "https://docs.bytechef.io/reference/components/browser-use_v1#get-session")
        .properties(
            string(SESSION_ID)
                .label("Session ID")
                .description("ID of an existing idle session.")
                .options((OptionsFunction<String>) BrowserUseUtils::getSessionIdOptions)
                .required(true))
        .output(outputSchema(SESSION_RESPONSE_PROPERTY))
        .perform(BrowserUseGetSessionAction::perform);

    private BrowserUseGetSessionAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        return context
            .http(http -> http.get("/sessions/%s".formatted(inputParameters.getRequiredString(SESSION_ID))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
