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

package com.bytechef.component.google.chat.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.chat.constant.GoogleChatConstants.DISPLAY_NAME;
import static com.bytechef.component.google.chat.constant.GoogleChatConstants.SPACE;
import static com.bytechef.component.google.chat.constant.GoogleChatConstants.SPACE_TYPE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.google.commons.GoogleUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class GoogleChatCreateSpaceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSpace")
        .title("Create Space")
        .description("Creates space in Google Chat.")
        .properties(
            string(DISPLAY_NAME)
                .description("Name of the space to create.")
                .label("Space Name")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("name")
                            .description("Name of the space that was created."),
                        string("type")
                            .description("Type of the space."),
                        string(DISPLAY_NAME)
                            .description("Name of the space that will be displayed."),
                        string("spaceThreadingState"),
                        string(SPACE_TYPE),
                        string("spaceHistoryState"),
                        string("createTime"),
                        string("lastActiveTime"),
                        object("membershipCount"),
                        object("accessSettings")
                            .properties(
                                string("accessSettings")
                                    .description("Access settings for the space.")),
                        string("spaceUri"))))
        .perform(GoogleChatCreateSpaceAction::perform)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleChatCreateSpaceAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/spaces"))
            .configuration(responseType(Http.ResponseType.JSON))
            .body(
                Body.of(
                    Map.of(
                        SPACE_TYPE, SPACE,
                        DISPLAY_NAME, inputParameters.getRequiredString(DISPLAY_NAME))))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
