/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.box.action;

import static com.bytechef.component.box.constant.BoxConstants.CREATE_FOLDER;
import static com.bytechef.component.box.constant.BoxConstants.ID;
import static com.bytechef.component.box.constant.BoxConstants.NAME;
import static com.bytechef.component.box.constant.BoxConstants.PARENT;
import static com.bytechef.component.box.constant.BoxConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.box.util.BoxUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class BoxCreateFolderAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_FOLDER)
        .title("Create folder")
        .description("Creates a new empty folder within the specified parent folder.")
        .properties(
            string(NAME)
                .label("Folder name")
                .description("The name for the new folder.")
                .minLength(1)
                .maxLength(255)
                .required(true),
            string(ID)
                .label("Parent folder")
                .description(
                    "Folder where the new folder will be created; if no folder is selected, the folder will be " +
                        "created in the root folder.")
                .options((ActionOptionsFunction<String>) BoxUtils::getRootFolderOptions)
                .defaultValue("0")
                .required(true))
        .outputSchema(
            object()
                .properties(
                    string(TYPE),
                    string(ID),
                    object(PARENT)
                        .properties(
                            string(TYPE),
                            string(ID),
                            string(NAME))))
        .perform(BoxCreateFolderAction::perform);

    private BoxCreateFolderAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post("https://api.box.com/2.0/folders"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(
                Http.Body.of(
                    NAME, inputParameters.getRequiredString(NAME),
                    PARENT, Map.of(ID, inputParameters.getRequiredString(ID))))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
