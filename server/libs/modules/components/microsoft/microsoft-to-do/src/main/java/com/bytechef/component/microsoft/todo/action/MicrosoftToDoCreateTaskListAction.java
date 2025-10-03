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

package com.bytechef.component.microsoft.todo.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.DISPLAY_NAME;
import static com.bytechef.component.microsoft.todo.constant.MicrosoftToDoConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftToDoCreateTaskListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTaskList")
        .title("Create Task List")
        .description("Creates a new task list.")
        .properties(
            string(DISPLAY_NAME)
                .label("Title")
                .description("Title of the task list.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("@odata.context"),
                        string("@odata.etag"),
                        string(ID),
                        string(DISPLAY_NAME),
                        bool("isOwner"),
                        bool("isShared"),
                        string("wellKnownListName"))))
        .perform(MicrosoftToDoCreateTaskListAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftToDoCreateTaskListAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context
            .http(http -> http.post("/me/todo/lists"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(Map.of(DISPLAY_NAME, inputParameters.getRequiredString(DISPLAY_NAME))))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
