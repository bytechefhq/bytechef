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

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.dynamics.crm.util.MicrosoftDynamicsCrmUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftDynamicsCrmCreateRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRecord")
        .title("Create Record")
        .description("Creates a new record.")
        .properties(
            string(ENTITY_TYPE)
                .label("Entity Type")
                .description("Select or map the entity for which you want to create the record.")
                .options((OptionsFunction<String>) MicrosoftDynamicsCrmUtils::getEntityTypeOptions)
                .required(true),
            dynamicProperties(FIELDS)
                .properties(MicrosoftDynamicsCrmUtils.getEntityFieldsProperties(true))
                .propertiesLookupDependsOn(ENTITY_TYPE)
                .required(true))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/microsoft-dynamics-crm_v1#create-record")
        .perform(MicrosoftDynamicsCrmCreateRecordAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftDynamicsCrmCreateRecordAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/%s".formatted(inputParameters.getRequiredString(ENTITY_TYPE))))
            .header("Prefer", "return=representation")
            .body(Http.Body.of(inputParameters.getMap(FIELDS, Map.of())))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
