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

package com.bytechef.component.attio.action;

import static com.bytechef.component.attio.constant.AttioConstants.COMPANIES;
import static com.bytechef.component.attio.constant.AttioConstants.COMPANY_RECORD;
import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.DEALS;
import static com.bytechef.component.attio.constant.AttioConstants.PEOPLE;
import static com.bytechef.component.attio.constant.AttioConstants.PERSON_RECORD;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_TYPE;
import static com.bytechef.component.attio.constant.AttioConstants.USERS;
import static com.bytechef.component.attio.constant.AttioConstants.VALUES;
import static com.bytechef.component.attio.constant.AttioConstants.WORKSPACES;
import static com.bytechef.component.attio.util.AttioUtils.getDealRecord;
import static com.bytechef.component.attio.util.AttioUtils.getRecordValues;
import static com.bytechef.component.attio.util.AttioUtils.getUserRecord;
import static com.bytechef.component.attio.util.AttioUtils.getWorkspaceRecord;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AttioCreateRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createRecord")
        .title("Create Record")
        .description("Creates a new record.")
        .properties(
            string(RECORD_TYPE)
                .label("Record Type")
                .description("Type of record that will be created.")
                .options((OptionsFunction<String>) AttioUtils::getTargetObjectOptions)
                .required(true),
            PERSON_RECORD
                .displayCondition("%s == '%s'".formatted(RECORD_TYPE, PEOPLE))
                .required(true),
            COMPANY_RECORD
                .displayCondition("%s == '%s'".formatted(RECORD_TYPE, COMPANIES))
                .required(true),
            getUserRecord(true)
                .displayCondition("%s == '%s'".formatted(RECORD_TYPE, USERS))
                .required(true),
            getDealRecord(true)
                .displayCondition("%s == '%s'".formatted(RECORD_TYPE, DEALS))
                .required(true),
            getWorkspaceRecord(true)
                .displayCondition("%s == '%s'".formatted(RECORD_TYPE, WORKSPACES))
                .required(true))
        .output()
        .perform(AttioCreateRecordAction::perform);

    private AttioCreateRecordAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String recordType = inputParameters.getRequiredString(RECORD_TYPE);
        Map<String, Object> recordMap = inputParameters.getMap(recordType, Object.class, Map.of());

        Map<String, Object> values = getRecordValues(recordMap, recordType);

        return context.http(http -> http.post("/objects/%s/records".formatted(recordType)))
            .body(Body.of(DATA, Map.of(VALUES, values)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
