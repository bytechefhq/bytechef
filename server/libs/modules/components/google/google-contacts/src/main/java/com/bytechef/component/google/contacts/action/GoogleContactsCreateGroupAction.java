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

package com.bytechef.component.google.contacts.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.google.contacts.constant.GoogleContactsConstants.NAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
public class GoogleContactsCreateGroupAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createGroup")
        .title("Create Group")
        .description("Creates a new group.")
        .properties(
            string(NAME)
                .label("Group Name")
                .description("The name of the group.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("resourceName")
                            .description(
                                "The resource name for the contact group, assigned by the server. An ASCII string, " +
                                    "in the form of contactGroups/{contactGroupId}."),
                        string("etag")
                            .description("The HTTP entity tag of the resource. Used for web cache validation."),
                        string(NAME)
                            .description(
                                "The contact group name set by the group owner or a system provided name for " +
                                    "system groups."),
                        string("formattedName")
                            .description(
                                "The name translated and formatted in the viewer's account locale or the Accept-" +
                                    "Language HTTP header locale for system groups names. Group names set by the " +
                                    "owner are the same as name."))))
        .perform(GoogleContactsCreateGroupAction::perform);

    private GoogleContactsCreateGroupAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/contactGroups"))
            .configuration(responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(
                "contactGroup", Map.of(
                    NAME, inputParameters.getRequiredString(NAME)),
                "readGroupFields", "name"))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
