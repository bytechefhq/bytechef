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

package com.bytechef.component.mergehelper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.mergehelper.constant.MergeHelperConstants.INPUTS;
import static com.bytechef.component.mergehelper.util.MergeHelperUtils.flatten;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class MergeHelperAppendAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("append")
        .title("Append")
        .description(
            "Takes multiple input items and combines them into a single array by appending all entries, keeping all keys.")
        .properties(
            array(INPUTS)
                .label("Inputs")
                .description("A collection of objects, arrays, or nested structures to be merged.")
                .minItems(2)
                .required(true))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/mergeHelper_v1#mergeHelper-append")
        .perform(MergeHelperAppendAction::perform);

    private MergeHelperAppendAction() {
    }

    public static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Object[] inputs = inputParameters.getArray(INPUTS);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object input : inputs) {
            result.addAll(flatten(input));
        }

        return result;
    }
}
