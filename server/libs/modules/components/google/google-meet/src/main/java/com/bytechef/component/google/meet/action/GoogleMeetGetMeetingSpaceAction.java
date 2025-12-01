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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.MEETING_SPACE_OUTPUT_PROPERTY;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.NAME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleUtils;

/**
 * @author Marija Horvat
 */
public class GoogleMeetGetMeetingSpaceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getMeetingSpace")
        .title("Get Meeting Space")
        .description("Gets details about a meeting space.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the meeting space or meeting code in format spaces/{meetingCode}.")
                .required(true))
        .output(outputSchema(MEETING_SPACE_OUTPUT_PROPERTY))
        .perform(GoogleMeetGetMeetingSpaceAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleMeetGetMeetingSpaceAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get("/" + inputParameters.getRequiredString(NAME)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
