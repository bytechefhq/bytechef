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
import static com.bytechef.component.copper.constant.CopperConstants.CREATE_ACTIVITY;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS;
import static com.bytechef.component.copper.constant.CopperConstants.DETAILS_PROPERTY;
import static com.bytechef.component.copper.constant.CopperConstants.ID;
import static com.bytechef.component.copper.constant.CopperConstants.PARENT;
import static com.bytechef.component.copper.constant.CopperConstants.TYPE;
import static com.bytechef.component.copper.util.CopperUtils.getHeaders;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.component.copper.util.CopperOptionUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource;
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
                .options((OptionsDataSource.ActionOptionsFunction<String>) CopperOptionUtils::getActivityTypeOptions)
                .required(true),
            DETAILS_PROPERTY
                .description("Text body of this Activity.")
                .required(true),
            object(PARENT)
                .label("Parent")
                .description("Resource to which this Activity belongs.")
                .properties(
                    string(ID)
                        .label("Parent ID")
                        .required(true),
                    string(TYPE)
                        .label("Parent type")
                        .options(
                            option("Lead", "lead"),
                            option("Person", "person"),
                            option("Company", "company"),
                            option("Opportunity", "opportunity"),
                            option("Project", "project"),
                            option("Task", "task"))
                        .required(true))
                .required(true))
        .outputSchema(
            object()
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()))
        .perform(CopperCreateActivityAction::perform);

    private CopperCreateActivityAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/activities"))
            .headers(getHeaders(connectionParameters))
            .body(
                Http.Body.of(
                    TYPE, Map.of("category", "user", ID, inputParameters.getRequiredString(ACTIVITY_TYPE)),
                    DETAILS, inputParameters.getRequiredString(DETAILS),
                    PARENT, inputParameters.getRequired(PARENT)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});
    }
}
