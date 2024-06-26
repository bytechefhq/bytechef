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

package com.bytechef.component.data.mapper.action;

import static com.bytechef.component.data.mapper.constant.DataMapperConstants.FROM;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.INPUT;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.MAPPINGS;
import static com.bytechef.component.data.mapper.constant.DataMapperConstants.TO;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.data.mapper.util.mapping.Mapping;
import com.bytechef.component.data.mapper.util.mapping.StringMapping;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class DataMapperRenameKeysAction {
    private DataMapperRenameKeysAction() {
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = ComponentDSL.action("renameKeys")
        .title("Rename keys")
        .description(
            "The action renames keys of an input object defined by mappings.")
        .properties(
            object(INPUT)
                .label("Input")
                .description("The input object that contains property keys and values.")
                .required(true),
            array(MAPPINGS)
                .label("Mappings")
                .description(
                    "An array of objects that contains properties 'from' and 'to'.")
                .items(
                    object()
                        .properties(
                            string(FROM)
                                .label("From Path")
                                .description(
                                    "Defines the path of the input property key you want to change the name of. Dot notation."),
                            string(TO)
                                .label("To")
                                .description("Defines what you want to change the name of the input property key to.")))
                .required(true))
        .output()
        .perform(DataMapperRenameKeysAction::perform);

    protected static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        List<StringMapping> mappingList = inputParameters.getList(MAPPINGS, StringMapping.class, List.of());
        Map<String, String> mappings = mappingList.stream()
            .collect(Collectors.toMap(Mapping::getFrom, Mapping::getTo));

        DocumentContext input = JsonPath.parse(inputParameters.get(INPUT));
        for (Map.Entry<String, String> entry : mappings.entrySet()) {

            String[] split = entry.getKey()
                .split("\\.(?=[^\\.]+$)");
            if (split.length > 1)
                input.renameKey(split[0], split[1], entry.getValue());
            else
                input.renameKey("$", entry.getKey(), entry.getValue());
        }

        return input.read("$");
    }
}
