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

package com.bytechef.component.vbout.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vbout.constant.VboutConstants.EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.TAGNAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class VboutAddTagToContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addTagToContact")
        .title("Add Tag To Contact")
        .description("Adds the tag to the contact.")
        .properties(
            string(EMAIL)
                .label("Email")
                .description("The email of the contact.")
                .required(true),
            array(TAGNAME)
                .label("Tag Name")
                .description("Tag(s) to be added.")
                .items(string())
                .required(true))
        .perform(VboutAddTagToContactAction::perform);

    private VboutAddTagToContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        context
            .http(http -> http.post("/emailMarketing/AddTag"))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(
                EMAIL, inputParameters.getRequiredString(EMAIL),
                TAGNAME, inputParameters.getList(TAGNAME))
            .execute();

        return null;
    }
}
