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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.x.constant.XConstants.DATA;
import static com.bytechef.component.x.constant.XConstants.ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class XDeletePostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deletePost")
        .title("Delete Post")
        .description("Deletes a specific post.")
        .properties(
            string(ID)
                .label("Post ID")
                .description("The ID of the post to be deleted.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        object(DATA)
                            .properties(
                                bool("deleted")
                                    .description("Indicates whether the post has been deleted.")))))
        .perform(XDeletePostAction::perform);

    private XDeletePostAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.delete("/tweets/" + inputParameters.getRequiredString(ID)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
