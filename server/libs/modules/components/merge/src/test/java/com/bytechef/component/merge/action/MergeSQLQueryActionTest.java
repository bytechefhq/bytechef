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
import static com.bytechef.component.merge.constant.MergeConstants.SQL_QUERY;
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
class MergeSQLQueryActionTest {

    private final Context context = mock(Context.class);

    @Test
    void testPerformJoinTwoInputs() {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                INPUTS, List.of(
                    Map.of("tableName", "input1", "value", List.of(Map.of("id", 1, "name", "Alice"))),
                    Map.of("tableName", "input2", "value", List.of(Map.of("user_id", 1, "amount", 100)))),
                SQL_QUERY,
                "SELECT i1.name, i2.amount FROM input1 i1 JOIN input2 i2 ON i1.id = i2.user_id"));

        List<Map<String, Object>> result =
            MergeSQLQueryAction.perform(mockedParameters, mockedParameters, context);

        assertEquals(List.of(Map.of("name", "Alice", "amount", 100)), result);
    }

    @Test
    void testPerformSingleInput() {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                INPUTS, List.of(
                    Map.of("tableName", "input1", "value", List.of(Map.of("id", 1, "value", "test")))),
                SQL_QUERY,
                "SELECT * FROM input1"));

        List<Map<String, Object>> result =
            MergeSQLQueryAction.perform(mockedParameters, mockedParameters, context);

        assertEquals(List.of(Map.of("value", "test", "id", 1)), result);
    }

    @Test
    void testPerformMultipleRows() {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                INPUTS, List.of(
                    Map.of("tableName", "input1", "value", List.of(
                        Map.of("id", 1, "name", "Alice"),
                        Map.of("id", 2, "name", "Bob"))),
                    Map.of("tableName", "input2", "value", List.of(
                        Map.of("user_id", 1, "amount", 100),
                        Map.of("user_id", 2, "amount", 200)))),
                SQL_QUERY,
                "SELECT i1.name, i2.amount FROM input1 i1 JOIN input2 i2 ON i1.id = i2.user_id ORDER BY i1.id"));

        List<Map<String, Object>> result =
            MergeSQLQueryAction.perform(mockedParameters, mockedParameters, context);

        assertEquals(List.of(
            Map.of("name", "Alice", "amount", 100),
            Map.of("name", "Bob", "amount", 200)), result);
    }

    @Test
    void testPerformUnionQuery() {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                INPUTS, List.of(
                    Map.of("tableName", "input1", "value", List.of(Map.of("id", 1))),
                    Map.of("tableName", "input2", "value", List.of(Map.of("id", 2)))),
                SQL_QUERY,
                "SELECT * FROM input1 UNION ALL SELECT * FROM input2"));

        List<Map<String, Object>> result =
            MergeSQLQueryAction.perform(mockedParameters, mockedParameters, context);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0)
            .get("id"));
        assertEquals(2, result.get(1)
            .get("id"));
    }

    @Test
    void testInvalidSqlThrowsException() {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(
                INPUTS, List.of(
                    Map.of("tableName", "input1", "value", List.of(Map.of("id", 1)))),
                SQL_QUERY,
                "SELECT * FROM non_existing_table"));

        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> MergeSQLQueryAction.perform(mockedParameters, mockedParameters, context));
    }
}
