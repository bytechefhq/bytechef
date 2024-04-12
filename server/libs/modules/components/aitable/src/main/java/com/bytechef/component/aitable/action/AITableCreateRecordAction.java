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

package com.bytechef.component.aitable.action;

import static com.bytechef.component.aitable.constant.AITableConstants.BASE_URL;
import static com.bytechef.component.aitable.constant.AITableConstants.CREATE_RECORD;
import static com.bytechef.component.aitable.constant.AITableConstants.DATASHEET_ID;
import static com.bytechef.component.aitable.constant.AITableConstants.DATASHEET_ID_PROPERTY;
import static com.bytechef.component.aitable.constant.AITableConstants.FIELDS;
import static com.bytechef.component.aitable.constant.AITableConstants.FIELDS_DYNAMIC_PROPERTY;
import static com.bytechef.component.aitable.constant.AITableConstants.OUTPUT_PROPERTY;
import static com.bytechef.component.aitable.constant.AITableConstants.RECORDS;
import static com.bytechef.component.aitable.constant.AITableConstants.SPACE_ID_PROPERTY;
import static com.bytechef.component.definition.ComponentDSL.action;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class AITableCreateRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_RECORD)
        .title("Create record")
        .description("Creates a new record in datasheet.")
        .properties(
            SPACE_ID_PROPERTY,
            DATASHEET_ID_PROPERTY,
            FIELDS_DYNAMIC_PROPERTY)
        .outputSchema(OUTPUT_PROPERTY)
        .perform(AITableCreateRecordAction::perform);

    private AITableCreateRecordAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(
            BASE_URL + "/datasheets/" + inputParameters.getRequiredString(DATASHEET_ID) + "/records"))
            .body(
                Http.Body.of(
                    RECORDS, List.of(Map.of(FIELDS, inputParameters.get(FIELDS)))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
