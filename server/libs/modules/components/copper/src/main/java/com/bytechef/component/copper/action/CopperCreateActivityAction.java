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

package com.bytechef.component.copper.action;

import static com.bytechef.component.copper.constant.CopperConstants.ACTIVITY_TYPE;
import static com.bytechef.component.copper.constant.CopperConstants.CATEGORY;
import static com.bytechef.component.copper.constant.CopperConstants.COMPANY;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.LEAD;
import static com.bytechef.component.copper.constant.CopperConstants.PARENT;
import static com.bytechef.component.copper.constant.CopperConstants.PERSON;
import static com.bytechef.component.copper.constant.CopperConstants.TYPE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.copper.util.CopperOptionUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class CopperCreateActivityAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createActivity")
        .title("Create Activity")
        .description("Creates a new activity.")
        .properties(
            string(ACTIVITY_TYPE)
                .label("Activity Type")
                .description("The activity type of this activity.")
                .options((ActionOptionsFunction<String>) CopperOptionUtils::getActivityTypeOptions)
                .required(true),
            string(DETAILS)
                .label("Details")
                .description("Text body of this activity.")
                .required(true),
            string(TYPE)
                .label("Parent Type")
                .description("Parent type to associate this activity with.")
                .options(
                    option("Lead", LEAD),
                    option("Person", PERSON),
                    option("Company", COMPANY),
                    option("Opportunity", "opportunity"))
                .defaultValue(PERSON)
                .required(true),
            string(ID)
                .label("Parent Name")
                .description("Parent this activity will be associated with.")
                .options((ActionOptionsFunction<String>) CopperOptionUtils::getParentOptions)
                .optionsLookupDependsOn(TYPE)
                .required(true))
        .output(outputSchema(
            object()
                .properties(
                    string(ID),
                    object(TYPE)
                        .properties(
                            string(CATEGORY),
                            string(ID)),
                    string(DETAILS),
                    object(PARENT)
                        .properties(
                            string(TYPE),
                            string(ID)))))
        .perform(CopperCreateActivityAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_ACTIVITIES_CONTEXT_FUNCTION =
        http -> http.post("/activities");

    private CopperCreateActivityAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(POST_ACTIVITIES_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                    TYPE, Map.of("category", "user", ID, inputParameters.getRequiredString(ACTIVITY_TYPE)),
                    DETAILS, inputParameters.getRequiredString(DETAILS),
                    PARENT,
                    Map.of(TYPE, inputParameters.getRequiredString(TYPE), ID, inputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
