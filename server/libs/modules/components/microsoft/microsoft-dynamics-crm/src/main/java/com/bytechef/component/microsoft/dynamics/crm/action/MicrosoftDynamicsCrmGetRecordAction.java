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

package com.bytechef.component.microsoft.dynamics.crm.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.ENTITY_TYPE;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.RECORD_ID;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.RECORD_ID_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.dynamics.crm.util.MicrosoftDynamicsCrmUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftDynamicsCrmGetRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getRecord")
        .title("Get Record")
        .description("Retrieves an existing record.")
        .properties(
            string(ENTITY_TYPE)
                .label("Entity Type")
                .description("Select or map the entity name whose records you want to retrieve.")
                .options((OptionsFunction<String>) MicrosoftDynamicsCrmUtils::getEntityTypeOptions)
                .required(true),
            RECORD_ID_PROPERTY)
        .output()
        .help("", "https://docs.bytechef.io/reference/components/microsoft-dynamics-crm_v1#get-record")
        .perform(MicrosoftDynamicsCrmGetRecordAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftDynamicsCrmGetRecordAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.get("/%s(%s)".formatted(
                inputParameters.getRequiredString(ENTITY_TYPE), inputParameters.getRequiredString(RECORD_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
