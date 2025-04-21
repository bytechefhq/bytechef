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
import static com.bytechef.component.salesforce.constant.SalesforceConstants.Q;
import static com.bytechef.component.salesforce.util.SalesforceUtils.executeSOQLQuery;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Monika Kušter
 */
public class SalesforceSOQLQueryAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("soqlQuery")
        .title("SOQL Query")
        .description("Executes a raw SOQL query to  extract data from Salesforce.")
        .properties(
            string(Q)
                .label("Query")
                .description("SOQL query to execute.")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output()
        .perform(SalesforceSOQLQueryAction::perform);

    private SalesforceSOQLQueryAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return executeSOQLQuery(actionContext, inputParameters.getRequiredString(Q));
    }
}
