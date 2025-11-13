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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.data.mapper.model.Mapping;
import com.bytechef.component.data.mapper.model.StringMapping;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 * @author Marko Kriskovic
 */
public class DataMapperRenameKeysAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("renameKeys")
        .title("Rename Keys")
        .description("The action renames keys of an input object defined by mappings.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("The input object that contains property keys and values.")
                .required(true),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "An array of objects that contains properties 'From Path' and 'To'.")
                .items(
                    object()
                        .properties(
                            string(FROM)
                                .label("From Path")
                                .description(
                                    "Defines the path of the input property key you want to change the name of, " +
                                        "using dot notation."),
                            string(TO)
                                .label("To")
                                .description("Defines what you want to change the name of the input property key to.")))
                .required(true))
        .output()
        .perform(DataMapperRenameKeysAction::perform);

    private DataMapperRenameKeysAction() {
    }

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<StringMapping> mappings = inputParameters.getList(MAPPINGS, StringMapping.class, List.of());

        Map<String, String> mappingMap = mappings.stream()
            .collect(Collectors.toMap(Mapping::getFrom, Mapping::getTo, (x, y) -> y, LinkedHashMap::new));

        DocumentContext input = JsonPath.parse(inputParameters.get(INPUT));

        for (Map.Entry<String, String> entry : mappingMap.entrySet()) {
            String key = entry.getKey();

            String[] split = key.split("\\.(?=[^\\.]+$)");

            if (split.length > 1) {
                input.renameKey(split[0], split[1], entry.getValue());
            } else {
                input.renameKey("$", key, entry.getValue());
            }
        }

        return input.read("$");
    }
}
