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
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mergehelper.constant.MergeHelperConstants.COMBINE_BY;
import static com.bytechef.component.mergehelper.constant.MergeHelperConstants.FIELD_TO_MATCH;
import static com.bytechef.component.mergehelper.constant.MergeHelperConstants.INPUT_1;
import static com.bytechef.component.mergehelper.constant.MergeHelperConstants.INPUT_2;
import static com.bytechef.component.mergehelper.util.MergeHelperUtils.flatten;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class MergeHelperCombineAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("combine")
        .title("Combine")
        .description("Combine data from two inputs.")
        .properties(
            string(COMBINE_BY)
                .label("Combine By")
                .description("Determines how input items are combined.")
                .options(
                    List.of(
                        option("Matching Fields", "matchingFields",
                            "Merge items that have the same values for the specified key."),
                        option("Position", "position", "Combine items based on their order in each input list."),
                        option("All Possible Combinations", "allPossibleCombinations",
                            "Merge every item from one input with every item from the others.")))
                .required(true),
            string(FIELD_TO_MATCH)
                .label("Field to Match")
                .description("The field to match for combining items.")
                .displayCondition("%s == '%s'".formatted(COMBINE_BY, "matchingFields"))
                .required(true),
            array(INPUT_1)
                .label("Input 1")
                .description("The first input to combine.")
                .required(true),
            array(INPUT_2)
                .label("Input 2")
                .description("The second input to combine.")
                .required(true))
        .output()
        .help("", "https://docs.bytechef.io/reference/components/mergeHelper_v1#mergeHelper-combine")
        .perform(MergeHelperCombineAction::perform);

    private MergeHelperCombineAction() {
    }

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Map<String, Object>> list1 = flatten(inputParameters.get(INPUT_1));
        List<Map<String, Object>> list2 = flatten(inputParameters.get(INPUT_2));

        return switch (inputParameters.getString(COMBINE_BY)) {
            case "position" -> combineByPosition(list1, list2);
            case "allPossibleCombinations" -> combineAllPossible(list1, list2);
            case "matchingFields" -> combineByMatchingFields(list1, list2, inputParameters.getString(FIELD_TO_MATCH));
            default -> throw new IllegalArgumentException("Unsupported combine mode");
        };
    }

    private static List<Map<String, Object>> combineByPosition(
        List<Map<String, Object>> list1, List<Map<String, Object>> list2) {

        int size = Math.min(list1.size(), list2.size());

        List<Map<String, Object>> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            Map<String, Object> mergedRows = new HashMap<>();

            Map<String, Object> row1 = list1.get(i);
            Map<String, Object> row2 = list2.get(i);

            if (row1 != null)
                mergedRows.putAll(row1);
            if (row2 != null)
                mergedRows.putAll(row2);

            result.add(mergedRows);
        }

        return result;
    }

    private static List<Map<String, Object>> combineByMatchingFields(
        List<Map<String, Object>> list1, List<Map<String, Object>> list2, String fieldToMatch) {

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> item1 : list1) {
            Object value1 = item1.get(fieldToMatch);
            boolean matched = false;

            for (Map<String, Object> item2 : list2) {
                Object value2 = item2.get(fieldToMatch);

                if (value1 != null && value1.equals(value2)) {
                    Map<String, Object> mergedItem = new HashMap<>(item1);

                    mergedItem.putAll(item2);

                    result.add(mergedItem);

                    matched = true;
                }
            }

            if (!matched) {
                result.add(new HashMap<>(item1));
            }
        }

        return result;
    }

    private static List<Map<String, Object>> combineAllPossible(
        List<Map<String, Object>> list1, List<Map<String, Object>> list2) {

        List<Map<String, Object>> result = new ArrayList<>(list1.size() * list2.size());

        for (Map<String, Object> item1 : list1) {
            for (Map<String, Object> item2 : list2) {
                Map<String, Object> mergedItem = new HashMap<>();

                if (item1 != null) {
                    mergedItem.putAll(item1);
                }

                if (item2 != null) {
                    mergedItem.putAll(item2);
                }

                result.add(mergedItem);
            }
        }

        return result;
    }
}
