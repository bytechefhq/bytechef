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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.typeform.constant.TypeformConstants.HREF;
import static com.bytechef.component.typeform.constant.TypeformConstants.ID;
import static com.bytechef.component.typeform.constant.TypeformConstants.TITLE;
import static com.bytechef.component.typeform.constant.TypeformConstants.TYPE;
import static com.bytechef.component.typeform.constant.TypeformConstants.WORKSPACE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.typeform.util.TypeformUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class TypeformCreateEmptyFormAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEmptyForm")
        .title("Create Empty Form")
        .description("Creates a new empty form.")
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
                .label("Workspace URL")
                .description("URL of the workspace to use for the typeform.")
                .options((OptionsFunction<String>) TypeformUtils::getWorkspaceUrlOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID),
                        string(TYPE),
                        string(TITLE),
                        object(WORKSPACE)
                            .properties(string(HREF)),
                        object("theme")
                            .properties(string("href")),
                        object("settings")
                            .properties(
                                string("language"),
                                string("progress_bar"),
                                object("meta")
                                    .properties(bool("allow_indexing")),
                                bool("hide_navigation"),
                                bool("is_public"),
                                bool("is_trial"),
                                bool("show_progress_bar"),
                                bool("show_typeform_branding"),
                                bool("are_uploads_public"),
                                bool("show_time_to_complete"),
                                bool("show_number_of_submissions"),
                                bool("show_cookie_consent"),
                                bool("show_question_number"),
                                bool("show_key_hint_on_choices"),
                                bool("autosave_progress"),
                                bool("free_form_navigation"),
                                bool("use_lead_qualification"),
                                bool("pro_subdomain_enabled"),
                                bool("auto_translate"),
                                bool("partial_responses_to_all_integrations")),
                        object("_links")
                            .properties(
                                string("display"),
                                string("responses")))))
        .perform(TypeformCreateEmptyFormAction::perform);

    private TypeformCreateEmptyFormAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/forms"))
            .body(
                Http.Body.of(
                    TITLE, inputParameters.getRequiredString(TITLE),
                    TYPE, inputParameters.getString(TYPE),
                    WORKSPACE, Map.of(HREF, inputParameters.getRequiredString(WORKSPACE))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
