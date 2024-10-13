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

package com.bytechef.component.nifty.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.nifty.constant.NiftyConstants.ACCESS_TYPE;
import static com.bytechef.component.nifty.constant.NiftyConstants.ACCESS_TYPES;
import static com.bytechef.component.nifty.constant.NiftyConstants.DESCRIPTION;
import static com.bytechef.component.nifty.constant.NiftyConstants.NAME;
import static com.bytechef.component.nifty.constant.NiftyConstants.PROJECT_TYPES;
import static com.bytechef.component.nifty.constant.NiftyConstants.TEMPLATE_TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.nifty.AbstractNiftyComponentHandler;
import com.bytechef.component.nifty.util.NiftyOptionUtils;
import java.util.Map;

/**
 * @author Mayank Madan
 */
public class NiftyCreateProjectAction extends AbstractNiftyComponentHandler {

    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createProject")
        .title("Create project")
        .description("Creates a new project")
        .properties(string(NAME)
            .label("Name")
            .description("Name of the project to be created.")
            .required(true),
            string(DESCRIPTION)
                .label("Description")
                .description("Description of the project to be created.")
                .required(true),
            string(ACCESS_TYPE)
                .label("Access")
                .description("Access type for the project to be created.")
                .options(ACCESS_TYPES)
                .required(true),
            string("type")
                .label("Type")
                .description("Type of project to be created.")
                .options(PROJECT_TYPES)
                .required(true),
            string(TEMPLATE_TYPE)
                .label("Template")
                .description("Template to pre-configure the project to be created.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) NiftyOptionUtils::getTemplateIdOptions)
                .required(false))
        .output(outputSchema(object().properties(object("body")
            .properties(string("id").required(false), string("name").required(false), string("project").required(false),
                string("description").required(false))
            .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))))
        .perform(NiftyCreateProjectAction::perform);

    private NiftyCreateProjectAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post("/projects/"))
            .body(Context.Http.Body.of(inputParameters))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
