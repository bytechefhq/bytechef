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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.CONFERENCE_RECORDS;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.NAME;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.meet.util.GoogleMeetUtils;
import com.bytechef.google.commons.GoogleUtils;

/**
 * @author Marija Horvat
 */
public class GoogleMeetListParticipantsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listParticipants")
        .title("List Participants")
        .description(
            "Lists the participants in a conference record. By default, ordered by join time and in descending order.")
        .properties(
            string(CONFERENCE_RECORDS)
                .label("Conference Records")
                .description("Conference Records")
                .options((OptionsFunction<String>) GoogleMeetUtils::getConferenceRecordsOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("participants")
                            .description("List of participants in one page.")
                            .items(
                                object()
                                    .properties(
                                        string(NAME)
                                            .description(
                                                "Resource name of the participant.Format: " +
                                                    "conferenceRecords/{conferenceRecord}/participants/{participant}"),
                                        object("user")
                                            .description("User can be of type: signedinUser, anonymousUser, phoneUser"),
                                        string("earliestStartTime")
                                            .description("Time when the participant first joined the meeting."),
                                        string("latestEndTime")
                                            .description(
                                                "Time when the participant left the meeting for the last time. This " +
                                                    "can be null if it's an active meeting."))))))
        .perform(GoogleMeetListParticipantsAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleMeetListParticipantsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.get(
                "https://meet.googleapis.com/v2/%s/participants".formatted(
                    inputParameters.getRequiredString(CONFERENCE_RECORDS))))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
