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
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.dynamics.crm.util.MicrosoftDynamicsCrmUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftDynamicsCrmDeleteRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteRecord")
        .title("Delete Record")
        .description("Creates a new record.")
        .properties(
            string(ENTITY_TYPE)
                .label("Entity Type")
                .description("Select or map the entity name whose records you want to delete.")
                .options((OptionsFunction<String>) MicrosoftDynamicsCrmUtils::getEntityTypeOptions)
                .required(true),
            RECORD_ID_PROPERTY)
        .help("", "https://docs.bytechef.io/reference/components/microsoft-dynamics-crm_v1#delete-record")
        .perform(MicrosoftDynamicsCrmDeleteRecordAction::perform);

    private MicrosoftDynamicsCrmDeleteRecordAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(
            http -> http.delete("/%s(%s)".formatted(
                inputParameters.getRequiredString(ENTITY_TYPE), inputParameters.getRequiredString(RECORD_ID))))
            .execute();

        return null;
    }
}
