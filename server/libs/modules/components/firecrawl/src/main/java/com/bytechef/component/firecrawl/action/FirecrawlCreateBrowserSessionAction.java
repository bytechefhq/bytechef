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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ACTIVITY_TTL;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.INCLUDE_PROFILE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.NAME;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PROFILE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SAVE_CHANGES;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.STREAM_WEB_VIEW;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TTL;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Ivica Cardic
 */
public class FirecrawlCreateBrowserSessionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createBrowserSession")
        .title("Create Browser Session")
        .description(
            "Launch a cloud browser session that you can drive with code, independently of any scrape. Use the " +
                "returned session ID with the Execute Code in Browser Session action.")
        .properties(
            integer(TTL)
                .label("TTL")
                .description("Total time-to-live of the session in seconds. Default is 300, maximum is 3600.")
                .minValue(30)
                .maxValue(3600)
                .required(false),
            integer(ACTIVITY_TTL)
                .label("Activity TTL")
                .description("Time in seconds without activity after which the session is destroyed.")
                .minValue(10)
                .maxValue(3600)
                .required(false),
            bool(STREAM_WEB_VIEW)
                .label("Stream Web View")
                .description("Whether to stream a live view of the browser. Default is true.")
                .advancedOption(true)
                .required(false),
            bool(INCLUDE_PROFILE)
                .label("Include Profile")
                .description("Enable persistent browser storage across browser sessions.")
                .defaultValue(false)
                .advancedOption(true),
            object(PROFILE)
                .label("Profile")
                .description(
                    "Persist cookies, localStorage and session data across browser sessions. Sessions with the " +
                        "same profile name share browser state.")
                .properties(
                    string(NAME)
                        .label("Name")
                        .description("A name for the profile. Sessions with the same name share storage.")
                        .minLength(1)
                        .maxLength(128)
                        .required(true),
                    bool(SAVE_CHANGES)
                        .label("Save Changes")
                        .description(
                            "When true, browser state is saved back to the profile when the session is deleted. " +
                                "Set to false to load existing data without writing. Only one saving session is " +
                                "allowed at a time.")
                        .defaultValue(true)
                        .required(false))
                .displayCondition("%s == true".formatted(INCLUDE_PROFILE))
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success")
                            .description("Whether the session was created."),
                        string("id")
                            .description("The unique session identifier."),
                        string("cdpUrl")
                            .description("Chrome DevTools Protocol WebSocket URL of the browser session."),
                        string("liveViewUrl")
                            .description("Read-only live view URL of the browser session."),
                        string("interactiveLiveViewUrl")
                            .description("Live view URL that lets the viewer control the browser session."),
                        string("expiresAt")
                            .description("When the session expires based on its TTL."))))
        .help("", "https://docs.bytechef.io/reference/components/firecrawl_v1#create-browser-session")
        .perform(FirecrawlCreateBrowserSessionAction::perform);

    private FirecrawlCreateBrowserSessionAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        boolean includeProfile = inputParameters.getBoolean(INCLUDE_PROFILE, false);

        return context
            .http(http -> http.post("/interact"))
            .body(
                Http.Body.of(
                    TTL, inputParameters.getInteger(TTL),
                    ACTIVITY_TTL, inputParameters.getInteger(ACTIVITY_TTL),
                    STREAM_WEB_VIEW, inputParameters.getBoolean(STREAM_WEB_VIEW),
                    PROFILE, includeProfile ? inputParameters.getMap(PROFILE) : null))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
