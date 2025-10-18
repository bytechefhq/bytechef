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

package com.bytechef.component.figma.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class FigmaNewCommentTrigger {
    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newComment")
        .title("New Comment")
        .description("Trigger when a new comment is added to a Figma file.")
        .type(TriggerType.POLLING)
        .output(
            outputSchema(
                object().properties(
                    string("file_key").description("Key of the Figma file."),
                    string("file_name").description("Name of the Figma file."),
                    array("comment").items(string())
                        .description("List of comment messages."),
                    string("comment_id").description("ID of the comment."),
                    string("parent_id").description("Parent comment ID, if any."),
                    string("created_at").description("Timestamp when the comment was created."),
                    string("resolved_at").description("Timestamp when the comment was resolved."),
                    string("triggered_by").description("User who added the comment."),
                    string("timestamp").description("Event timestamp."))))
        .poll(FigmaNewCommentTrigger::poll);

    protected static PollOutput poll(
        Parameters inputParameters,
        Parameters connectionParameters,
        Parameters closureParameters,
        TriggerContext triggerContext) {
        String fileKey = inputParameters.getRequiredString("file_key");
        String token = connectionParameters.getRequiredString("access_token");

        Map<String, Object> body = triggerContext
            .http(http -> http.get("https://api.figma.com/v1/files/" + fileKey + "/comments"))
            .header("Authorization", "Bearer " + token)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<String, Object>> comments = (List<Map<String, Object>>) body.get("comments");

        List<Map<String, Object>> output = comments.stream()
            .map(comment -> Map.of(
                "file_key", fileKey,
                "file_name", body.get("fileName"),
                "comment", List.of(comment.get("message")),
                "comment_id", comment.get("id"),
                "parent_id", comment.get("parent_id"),
                "created_at", comment.get("created_at"),
                "resolved_at", comment.get("resolved_at"),
                "triggered_by", comment.get("user"),
                "timestamp", OffsetDateTime.now()
                    .toString()))
            .toList();
        return new PollOutput(output, Map.of(), false);
    }
}
