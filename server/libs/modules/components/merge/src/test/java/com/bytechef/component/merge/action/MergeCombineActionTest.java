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

package com.bytechef.component.merge.action;

import static com.bytechef.component.merge.constant.MergeConstants.COMBINE_BY;
import static com.bytechef.component.merge.constant.MergeConstants.FIELD_TO_MATCH;
import static com.bytechef.component.merge.constant.MergeConstants.INPUT_1;
import static com.bytechef.component.merge.constant.MergeConstants.INPUT_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivona Pavela
 */
class MergeCombineActionTest {

    private final Context context = mock(Context.class);

    private List<Map<String, Object>> input1() {
        return List.of(
            Map.of("name", "Alice", "userId", 1),
            Map.of("name", "Bob", "userId", 2));
    }

    private List<Map<String, Object>> input2() {
        return List.of(
            Map.of("product", "Book", "orderId", 101, "userId", 1),
            Map.of("product", "Pen", "orderId", 102, "userId", 2));
    }

    @Test
    void testPerformMatchingFields() {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                COMBINE_BY, "matchingFields",
                FIELD_TO_MATCH, "userId",
                INPUT_1, input1(),
                INPUT_2, input2()));

        List<Map<String, Object>> result =
            MergeCombineAction.perform(mockedParameters, mockedParameters, context);

        assertEquals(List.of(
            Map.of("name", "Alice", "userId", 1, "product", "Book", "orderId", 101),
            Map.of("name", "Bob", "userId", 2, "product", "Pen", "orderId", 102)), result);
    }

    @Test
    void testPerformPosition() {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                COMBINE_BY, "position",
                INPUT_1, input1(),
                INPUT_2, input2()));

        List<Map<String, Object>> result =
            MergeCombineAction.perform(mockedParameters, mockedParameters, context);

        assertEquals(List.of(
            Map.of("name", "Alice", "userId", 1, "product", "Book", "orderId", 101),
            Map.of("name", "Bob", "userId", 2, "product", "Pen", "orderId", 102)), result);
    }

    @Test
    void testPerformAllPossibleCombinations() {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                COMBINE_BY, "allPossibleCombinations",
                INPUT_1, input1(),
                INPUT_2, input2()));

        List<Map<String, Object>> result =
            MergeCombineAction.perform(mockedParameters, mockedParameters, context);

        List<Map<String, Object>> expected = List.of(
            Map.of("name", "Alice", "userId", 1, "product", "Book", "orderId", 101),
            Map.of("name", "Alice", "userId", 2, "product", "Pen", "orderId", 102),
            Map.of("name", "Bob", "userId", 1, "product", "Book", "orderId", 101),
            Map.of("name", "Bob", "userId", 2, "product", "Pen", "orderId", 102));

        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }
}
