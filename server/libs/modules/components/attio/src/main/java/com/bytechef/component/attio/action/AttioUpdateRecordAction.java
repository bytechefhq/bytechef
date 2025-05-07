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

import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_ID;
import static com.bytechef.component.attio.constant.AttioConstants.RECORD_TYPE;
import static com.bytechef.component.attio.constant.AttioConstants.VALUE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.PropertiesDataSource;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AttioUpdateRecordAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateRecord")
        .title("Update Record")
        .description("Updates a record.")
        .properties(
            string(RECORD_TYPE)
                .label("Record Type")
                .description("Type of record that will be created.")
                .options((ActionOptionsFunction<String>) AttioUtils::getTargetObjectOptions)
                .required(true),
            string(RECORD_ID)
                .label("Record ID")
                .description("ID of the record that will be updated.")
                .optionsLookupDependsOn(RECORD_TYPE)
                .options((ActionOptionsFunction<String>) AttioUtils::getRecordIdOptions)
                .required(true),
            dynamicProperties(VALUE)
                .propertiesLookupDependsOn(RECORD_TYPE)
                .properties((PropertiesDataSource.ActionPropertiesFunction) AttioUtils::getRecordAttributes)
                .required(true))
        .output()
        .perform(AttioUpdateRecordAction::perform);

    private AttioUpdateRecordAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.patch(
            "/objects/%s/records/%s".formatted(
                inputParameters.getRequiredString(RECORD_TYPE),
                inputParameters.getRequiredString(RECORD_ID))))
            .body(
                Body.of(
                    DATA, Map.of(
                        "values", inputParameters.getMap(VALUE)
                            .get(VALUE))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
