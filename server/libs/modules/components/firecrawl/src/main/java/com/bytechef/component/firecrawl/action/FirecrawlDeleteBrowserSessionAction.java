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
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SESSION_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class FirecrawlDeleteBrowserSessionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteBrowserSession")
        .title("Delete Browser Session")
        .description(
            "Destroy a browser session. Browser state is saved back to the session's profile when Save Changes is " +
                "enabled, and billing for the session ends.")
        .properties(
            string(SESSION_ID)
                .label("Session ID")
                .description("The ID of the browser session to destroy.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success")
                            .description("Whether the session was destroyed."),
                        integer("sessionDurationMs")
                            .description("Total duration of the session in milliseconds."),
                        number("creditsBilled")
                            .description("The number of credits billed for the session."),
                        string("error")
                            .description("Error message when the session could not be destroyed."))))
        .help("", "https://docs.bytechef.io/reference/components/firecrawl_v1#delete-browser-session")
        .perform(FirecrawlDeleteBrowserSessionAction::perform);

    private FirecrawlDeleteBrowserSessionAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.delete("/interact/" + inputParameters.getRequiredString(SESSION_ID)))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
