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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.CUSTOM_FIELDS;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.FIELDS;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.ID;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.OBJECT;
import static com.bytechef.component.salesforce.util.SalesforceUtils.combineFieldsAndCreateJsonFile;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.salesforce.util.SalesforceUtils;

/**
 * @author Monika Kušter
 */
public class SalesforceUpdateRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateRecord")
        .title("Update Record")
        .description("Updates an existing record for a specified Salesforce object.")
        .properties(
            string(OBJECT)
                .label("Salesforce Object")
                .options((OptionsFunction<String>) SalesforceUtils::getSalesforceObjectOptions)
                .required(true),
            string(ID)
                .label("Record ID")
                .description("ID of the record to update.")
                .options((OptionsFunction<String>) SalesforceUtils::getRecordIdOptions)
                .optionsLookupDependsOn(OBJECT)
                .required(true),
            dynamicProperties(FIELDS)
                .propertiesLookupDependsOn(OBJECT)
                .properties(SalesforceUtils::createPropertiesForObject)
                .required(true),
            object(CUSTOM_FIELDS)
                .label("Custom Fields")
                .additionalProperties(bool(), integer(), string(), number())
                .required(false))
        .perform(SalesforceUpdateRecordAction::perform);

    private SalesforceUpdateRecordAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        FileEntry fileEntry = combineFieldsAndCreateJsonFile(inputParameters, actionContext);

        actionContext.http(http -> http.patch(
            "/sobjects/%s/%s".formatted(
                inputParameters.getRequiredString(OBJECT), inputParameters.getRequiredString(ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(fileEntry))
            .execute();

        return null;
    }
}
