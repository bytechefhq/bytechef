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
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.ENTITY_TYPE;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.FIELDS;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.RECORD_ID;
import static com.bytechef.component.microsoft.dynamics.crm.constant.MicrosoftDynamicsCrmConstants.RECORD_ID_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.dynamics.crm.util.MicrosoftDynamicsCrmUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftDynamicsCrmUpdateRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateRecord")
        .title("Update Record")
        .description("Updates an existing record.")
        .properties(
            string(ENTITY_TYPE)
                .label("Entity Type")
                .description("Select or map the entity for which you want to update the record.")
                .options((OptionsFunction<String>) MicrosoftDynamicsCrmUtils::getEntityTypeOptions)
                .required(true),
            RECORD_ID_PROPERTY,
            dynamicProperties(FIELDS)
                .properties(MicrosoftDynamicsCrmUtils.getEntityFieldsProperties(false))
                .propertiesLookupDependsOn(ENTITY_TYPE)
                .required(true))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/microsoft-dynamics-crm_v1#update-record")
        .perform(MicrosoftDynamicsCrmUpdateRecordAction::perform);

    private MicrosoftDynamicsCrmUpdateRecordAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.patch("/%s(%s)".formatted(
                inputParameters.getRequiredString(ENTITY_TYPE), inputParameters.getRequiredString(RECORD_ID))))
            .header("Prefer", "return=representation")
            .body(Http.Body.of(inputParameters.getMap(FIELDS, Map.of())))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
