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

import static com.bytechef.component.merge.constant.MergeConstants.INPUTS;
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
class MergeAppendActionTest {

    private final Context context = mock(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(INPUTS, List.of(
            List.of(
                Map.of("name", "Alice", "userId", 1)),
            List.of(
                Map.of("product", "Book", "orderId", 101, "userId", 1)))));

    @Test
    void testPerform() {

        List<Map<String, Object>> result = MergeAppendAction.perform(mockedParameters, mockedParameters, context);

        assertEquals(List.of(
            Map.of("name", "Alice", "userId", 1),
            Map.of("product", "Book", "orderId", 101, "userId", 1)), result);

    }
}
