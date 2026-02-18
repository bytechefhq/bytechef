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

package com.bytechef.component.google.meet;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.meet.action.GoogleMeetCreateMeetingSpaceAction;
import com.bytechef.component.google.meet.action.GoogleMeetGetMeetingSpaceAction;
import com.bytechef.component.google.meet.action.GoogleMeetListParticipantsAction;
import com.bytechef.component.google.meet.connection.GoogleMeetConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class GoogleMeetComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleMeet")
        .title("Google Meet")
        .description(
            "Google Meet is a communication service designed to help you have interactions with your friends, " +
                "family, colleagues and classmates.")
        .icon("path:assets/google-meet.svg")
        .categories(ComponentCategory.COMMUNICATION)
        .connection(GoogleMeetConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp(
            "Google Meet REST API documentation",
            "https://developers.google.com/workspace/meet/api/guides/overview")
        .actions(
            GoogleMeetCreateMeetingSpaceAction.ACTION_DEFINITION,
            GoogleMeetGetMeetingSpaceAction.ACTION_DEFINITION,
            GoogleMeetListParticipantsAction.ACTION_DEFINITION)
        .clusterElements(
            tool(GoogleMeetCreateMeetingSpaceAction.ACTION_DEFINITION),
            tool(GoogleMeetGetMeetingSpaceAction.ACTION_DEFINITION),
            tool(GoogleMeetListParticipantsAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
