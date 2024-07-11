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

package com.bytechef.component.quickbooks.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.BASE_URL;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.COMPANY_ID;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.CREATE_CATEGORY;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.NAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

public class QuickbooksCreateCategoryAction {

    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_CATEGORY)
        .title("Create a category")
        .description("Creates a new category.")
        .properties(
            string(NAME)
                .label("Nane")
                .description("Name of the category")
                .maxLength(100)
                .required(true))
        .description("Has conditionally required parameters.")
        .outputSchema(
            object()
                .properties(
                    object("item")
                        .properties(
                            string("id"),
                            string("domain"),
                            string("Name"),
                            string("Level"),
                            string("Subitem"),
                            string("FullyQualifiedName"))))
        .perform(QuickbooksCreateCategoryAction::perform);

    private QuickbooksCreateCategoryAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post(
                BASE_URL + "/v3/company/" + getCompanyId(connectionParameters) + "/item?minorversion=4"))
            .body(Context.Http.Body.of("Type", "Category", NAME, inputParameters.getRequiredString(NAME)))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new Context.TypeReference<>() {});
    }

    private static String getCompanyId(Parameters connectionParameters) {
        String companyId = connectionParameters.getRequiredString(COMPANY_ID);

        return companyId.replace(" ", "");
    }
}
