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

package com.bytechef.component.google.meet.constant;

import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Marija Horvat
 */
public class GoogleMeetConstants {

    public static final String ACCESS_TYPE = "accessType";
    public static final String CONFERENCE_RECORDS = "conferenceRecords";
    public static final String NAME = "name";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_TOKEN = "pageToken";

    public static final ModifiableObjectProperty MEETING_SPACE_OUTPUT_PROPERTY =
        object()
            .properties(
                string(NAME)
                    .description("The name of the meeting space."),
                string("meetingUri")
                    .description("URI used to join meetings."),
                string("meetingCode")
                    .description("Type friendly unique string used to join the meeting."),
                object("config")
                    .properties(
                        string(ACCESS_TYPE)
                            .description(
                                "Access type of the meeting space that determines who can join without knocking."),
                        string("entryPointAccess")
                            .description(
                                "Defines the entry points that can be used to join meetings hosted in this meeting " +
                                    "space. Default: EntryPointAccess.ALL")));

    private GoogleMeetConstants() {
    }
}
