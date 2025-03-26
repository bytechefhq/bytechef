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

package com.bytechef.component.snowflake.action;

import static com.bytechef.component.snowflake.constant.SnowflakeConstants.COLUMN;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.CONDITION;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.DATABASE;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.SCHEMA;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.STATEMENT;
import static com.bytechef.component.snowflake.constant.SnowflakeConstants.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class SnowflakeDeleteRowActionTest {
    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            DATABASE, "database",
            SCHEMA, "schema",
            TABLE, "table",
            COLUMN, "col1",
            CONDITION, "2"));
    private final Response mockedResponse = mock(Response.class);
    private final Map<String, Object> reponseMap = Map.of();

    @Test
    void perform() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(reponseMap);

        Map<String, Object> result = SnowflakeDeleteRowAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of(), result);

        Body body = bodyArgumentCaptor.getValue();

        String expectedStatement = "DELETE FROM database.schema.table WHERE col1 = 2";

        assertEquals(Map.of(STATEMENT, expectedStatement), body.getContent());
    }
}
