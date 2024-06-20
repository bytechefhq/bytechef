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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class DataMapperRenameKeysAction {

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
                                .label("From")
                                .description(
                                    "Defines the name of the input property key you want to change the name of."),
                            string(TO)
                                .label("To")
                                .description("Defines what you want to change the name of the input property key to.")))
                .required(true))
        .output()
        .perform(DataMapperRenameKeysAction::perform);

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        Map<String, Object> input = inputParameters.getMap(INPUT, Object.class, Map.of());
        List<StringMapping> mappingList = inputParameters.getList(MAPPINGS, StringMapping.class, List.of());
        Map<String, String> mappings = mappingList.stream().collect(Collectors.toMap(Mapping::getFrom, Mapping::getTo));

        Map<String, Object> output = new HashMap<>();
        for (Map.Entry<String, Object> enrty : input.entrySet()) {
            if (mappings.containsKey(enrty.getKey()))
                output.put(mappings.get(enrty.getKey()), enrty.getValue());
            else
                output.put(enrty.getKey(), enrty.getValue());
        }

        return output;
    }
}
