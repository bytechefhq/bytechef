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
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.PAGE_SIZE;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.PAGE_TOKEN;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.google.meet.util.GoogleMeetUtils;
import com.bytechef.google.commons.GoogleUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 * @author Monika Kušter
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
                array()
                    .description("List of participants.")
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
                                            "can be null if it's an active meeting.")))))
        .perform(GoogleMeetListParticipantsAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleMeetListParticipantsAction() {
    }

    public static List<?> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Object> participants = new ArrayList<>();
        String nextPageToken = null;

        do {
            Map<String, ?> body = context
                .http(http -> http.get(
                    "https://meet.googleapis.com/v2/%s/participants".formatted(
                        inputParameters.getRequiredString(CONFERENCE_RECORDS))))
                .queryParameters(PAGE_SIZE, 250, PAGE_TOKEN, nextPageToken)
                .configuration(responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get("participants") instanceof List<?> list) {
                participants.addAll(list);
            }

            nextPageToken = (String) body.get(NEXT_PAGE_TOKEN);
        } while (nextPageToken != null);

        return participants;
    }
}
