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
import static com.bytechef.component.copper.constant.CopperConstants.BASE_URL;
import static com.bytechef.component.copper.constant.CopperConstants.CATEGORY;
import static com.bytechef.component.copper.constant.CopperConstants.COMPANY;
import static com.bytechef.component.copper.constant.CopperConstants.CREATE_ACTIVITY;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.LEAD;
import static com.bytechef.component.copper.constant.CopperConstants.OPPORTUNITY;
import static com.bytechef.component.copper.constant.CopperConstants.PARENT;
import static com.bytechef.component.copper.constant.CopperConstants.PERSON;
import static com.bytechef.component.copper.constant.CopperConstants.TYPE;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.copper.util.CopperOptionUtils;
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
public class CopperCreateActivityAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_ACTIVITY)
        .title("Create activity")
        .description("Creates a new Activity")
        .properties(
            string(ACTIVITY_TYPE)
                .label("Activity type")
                .description("The Activity Type of this Activity.")
                .options((ActionOptionsFunction<String>) CopperOptionUtils::getActivityTypeOptions)
                .required(true),
            string(DETAILS)
                .label("Details")
                .description("Text body of this Activity.")
                .required(true),
            string(TYPE)
                .label("Parent type")
                .description("Parent type to associate this Activity with.")
                .options(
                    option("Lead", LEAD),
                    option("Person", PERSON),
                    option("Company", COMPANY),
                    option("Opportunity", OPPORTUNITY))
                .defaultValue(PERSON)
                .required(true),
            string(ID)
                .label("Parent name")
                .description("Parent this Activity will be associated with.")
                .options((ActionOptionsFunction<String>) CopperOptionUtils::getParentOptions)
                .optionsLookupDependsOn(TYPE)
                .required(true))
        .outputSchema(
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
                            string(ID))))
        .perform(CopperCreateActivityAction::perform);

    private CopperCreateActivityAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/activities"))
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
