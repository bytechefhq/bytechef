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

package com.bytechef.component.myob.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.myob.constant.MyobConstants.ADDRESSES;
import static com.bytechef.component.myob.constant.MyobConstants.ADDRESSES_PROPERTY;
import static com.bytechef.component.myob.constant.MyobConstants.COMPANY_FILE;
import static com.bytechef.component.myob.constant.MyobConstants.COMPANY_FILE_PROPERTY;
import static com.bytechef.component.myob.constant.MyobConstants.COMPANY_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.FIRST_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.FIRST_NAME_PROPERTY;
import static com.bytechef.component.myob.constant.MyobConstants.IS_ACTIVE;
import static com.bytechef.component.myob.constant.MyobConstants.IS_INDIVIDUAL;
import static com.bytechef.component.myob.constant.MyobConstants.LAST_NAME;
import static com.bytechef.component.myob.constant.MyobConstants.LAST_NAME_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class MyobCreateSupplierAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createSupplier")
        .title("Create Supplier")
        .description("Creates a new supplier.")
        .properties(
            COMPANY_FILE_PROPERTY,
            bool(IS_INDIVIDUAL)
                .label("Is Individual?")
                .description("Does supplier contact represent an individual or a company?")
                .required(true),
            FIRST_NAME_PROPERTY,
            LAST_NAME_PROPERTY,
            string(COMPANY_NAME)
                .label("Company Name")
                .description("Company name of the supplier contact.")
                .maxLength(50)
                .displayCondition("%s == false".formatted(IS_INDIVIDUAL))
                .required(true),
            bool(IS_ACTIVE)
                .label("Is Active?")
                .description("Is supplier contact active?")
                .defaultValue(true)
                .required(false),
            ADDRESSES_PROPERTY)
        .perform(MyobCreateSupplierAction::perform);

    private MyobCreateSupplierAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        actionContext
            .http(http -> http.post(inputParameters.getRequiredString(COMPANY_FILE) + "/Contact/Supplier"))
            .body(
                Http.Body.of(
                    FIRST_NAME, inputParameters.getString(FIRST_NAME),
                    LAST_NAME, inputParameters.getString(LAST_NAME),
                    IS_INDIVIDUAL, inputParameters.getRequiredBoolean(IS_INDIVIDUAL),
                    COMPANY_NAME, inputParameters.getString(COMPANY_NAME),
                    IS_ACTIVE, inputParameters.getBoolean(IS_ACTIVE),
                    ADDRESSES, inputParameters.getList(ADDRESSES)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }
}
