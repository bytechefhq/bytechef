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

package com.bytechef.component.x.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.x.constant.XConstants.DATA;
import static com.bytechef.component.x.constant.XConstants.MEDIA;
import static com.bytechef.component.x.constant.XConstants.TEXT;
import static com.bytechef.component.x.constant.XConstants.USERNAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.x.util.XUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class XSendDirectMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendDirectMessage")
        .title("Send Direct Message")
        .description("Sends a direct message to a specified user.")
        .properties(
            string(USERNAME)
                .label("Username")
                .description("The username of the user to send a direct message to.")
                .required(true),
            string(TEXT)
                .label("Text")
                .description("The text of the message.")
                .controlType(ControlType.TEXT_AREA)
                .required(true),
            array(MEDIA)
                .label("Images")
                .description("The images to attach to the direct message.")
                .items(fileEntry())
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object(DATA)
                            .properties(
                                string("dm_conversation_id")
                                    .description("ID of the direct message conversation."),
                                string("dm_event_id")
                                    .description("ID of the direct message event.")))))
        .perform(XSendDirectMessageAction::perform);

    private XSendDirectMessageAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> bodyMap = new HashMap<>();

        bodyMap.put(TEXT, inputParameters.getRequiredString(TEXT));

        List<FileEntry> images = inputParameters.getList(MEDIA, FileEntry.class);
        List<String> mediaIds = new ArrayList<>();

        if (images != null && !images.isEmpty()) {
            for (FileEntry fileEntry : images) {
                String uploadedImageId = XUtils.uploadMedia(context, fileEntry, "dm_image");

                mediaIds.add(uploadedImageId);
            }

            bodyMap.put(MEDIA, Map.of("media_ids", mediaIds));
        }

        String username = inputParameters.getRequiredString(USERNAME);
        String userId = XUtils.getUserIdByUsername(context, username);

        return context.http(http -> http.post("/dm_conversations/with/" + userId + "/messages"))
            .body(Http.Body.of(bodyMap))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
