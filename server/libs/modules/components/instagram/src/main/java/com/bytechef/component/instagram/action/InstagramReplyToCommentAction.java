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

package com.bytechef.component.instagram.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.instagram.constant.InstagramConstants.COMMENT_ID;
import static com.bytechef.component.instagram.constant.InstagramConstants.MESSAGE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class InstagramReplyToCommentAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replyToComment")
        .title("Reply to Comment")
        .description("Replies to a comment on a media item of the connected Instagram professional account.")
        .help("", "https://docs.bytechef.io/reference/components/instagram_v1#reply-to-comment")
        .properties(
            string(COMMENT_ID)
                .label("Comment ID")
                .description("ID of the comment to reply to.")
                .required(true),
            string(MESSAGE)
                .label("Message")
                .description("Text of the reply.")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("ID of the reply comment."))))
        .perform(InstagramReplyToCommentAction::perform);

    private InstagramReplyToCommentAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String commentId = inputParameters.getRequiredString(COMMENT_ID);

        return context.http(http -> http.post("/" + commentId + "/replies"))
            .body(Body.of(Map.of("message", inputParameters.getRequiredString(MESSAGE))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
