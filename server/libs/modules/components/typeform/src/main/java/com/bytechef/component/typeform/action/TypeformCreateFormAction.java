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

package com.bytechef.component.typeform.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.typeform.constant.TypeformConstants.HREF;
import static com.bytechef.component.typeform.constant.TypeformConstants.TITLE;
import static com.bytechef.component.typeform.constant.TypeformConstants.TYPE;
import static com.bytechef.component.typeform.constant.TypeformConstants.WORKSPACE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.typeform.util.TypeformUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class TypeformCreateFormAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createForm")
        .title("Create Form")
        .description("Creates a new form")
        .properties(
            string(TITLE)
                .label("Title")
                .description("Title to use for the form.")
                .required(true),
            string(TYPE)
                .label("Type")
                .description("Form type for the typeform.")
                .options(
                    option("Quiz", "quiz"),
                    option("Classification", "classification"),
                    option("Score", "score"),
                    option("Branching", "branching"),
                    option("Classification branching", "classification_branching"),
                    option("Score branching", "score_branching"))
                .defaultValue("quiz")
                .required(false),
            string(WORKSPACE)
                .label("Workspace")
                .description("Workspace where the form will be created.")
                .options((OptionsFunction<String>) TypeformUtils::getWorkspaceOptions)
                .required(false))
        .output(
            outputSchema(
                // TODO
                object()))
        .perform(TypeformCreateFormAction::perform);

    private TypeformCreateFormAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        // TODO

        Http.Response execute = actionContext.http(http -> http.post("/forms"))
            .body(
                Http.Body.of(
                    TITLE, inputParameters.getRequiredString(TITLE),
                    TYPE, inputParameters.getString(TYPE),
                    WORKSPACE, Map.of(HREF, inputParameters.getString(WORKSPACE))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return execute.getBody(new TypeReference<>() {});
    }
}
