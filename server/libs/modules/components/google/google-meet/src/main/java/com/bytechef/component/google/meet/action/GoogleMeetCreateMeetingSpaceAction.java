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

package com.bytechef.component.google.meet.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.ACCESS_TYPE;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.MEETING_SPACE_OUTPUT_PROPERTY;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleUtils;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class GoogleMeetCreateMeetingSpaceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createMeetingSpace")
        .title("Create Meeting Space")
        .description("Creates a meeting space.")
        .properties(
            string(ACCESS_TYPE)
                .label("Access Type")
                .description("Access type of the meeting space that determines who can join without knocking.")
                .defaultValue("TRUSTED")
                .options(
                    option("Access Type Unspecified", "ACCESS_TYPE_UNSPECIFIED",
                        "Default value specified by the user's organization."),
                    option("Open", "OPEN",
                        "Anyone with the join information can join without knocking."),
                    option("Trusted", "TRUSTED",
                        "Members of the host's organization, invited external users, and dial-in users can join " +
                            "without knocking. Everyone else must knock."),
                    option("Restricted", "RESTRICTED",
                        "Only invitees can join without knocking. Everyone else must knock."))
                .required(false))
        .output(outputSchema(MEETING_SPACE_OUTPUT_PROPERTY))
        .perform(GoogleMeetCreateMeetingSpaceAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleMeetCreateMeetingSpaceAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("https://meet.googleapis.com/v2/spaces"))
            .body(Http.Body.of("config", Map.of(ACCESS_TYPE, inputParameters.getString(ACCESS_TYPE))))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
