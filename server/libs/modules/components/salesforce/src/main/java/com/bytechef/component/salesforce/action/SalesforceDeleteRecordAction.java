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

package com.bytechef.component.salesforce.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.ID;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.OBJECT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.salesforce.util.SalesforceUtils;

/**
 * @author Monika Kušter
 */
public class SalesforceDeleteRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRecord")
        .title("Delete Record")
        .description("Deletes an existing record of a specified Salesforce object.")
        .properties(
            string(OBJECT)
                .label("Salesforce Object")
                .options((OptionsFunction<String>) SalesforceUtils::getSalesforceObjectOptions)
                .required(true),
            string(ID)
                .label("Record ID")
                .description("ID of the object to delete.")
                .options((OptionsFunction<String>) SalesforceUtils::getRecordIdOptions)
                .optionsLookupDependsOn(OBJECT)
                .required(true))
        .perform(SalesforceDeleteRecordAction::perform);

    private SalesforceDeleteRecordAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        actionContext.http(http -> http.delete(
            "/sobjects/%s/%s".formatted(
                inputParameters.getRequiredString(OBJECT), inputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }
}
