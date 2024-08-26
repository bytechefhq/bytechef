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

package com.bytechef.component.monday.util;

import static com.bytechef.component.monday.constant.MondayConstants.BOARDS;
import static com.bytechef.component.monday.constant.MondayConstants.DATA;
import static com.bytechef.component.monday.constant.MondayConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class MondayUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetBoardColumns(){
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(DATA, Map.of(BOARDS, List.of(Map.of("columns", List.of(Map.of(ID, "abc")))))));

        List<?> boardColumns = MondayUtils.getBoardColumns("board", mockedActionContext);

        assertEquals(List.of(Map.of(ID, "abc")), boardColumns);


        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(
            Map.of("query", "query{boards(ids: board){columns{id title type settings_str description}}}"),
            body.getContent());
    }

    @Test
    void testExecuteGraphQLQuery() {
        Map<String, Object> responseMap = Map.of("id", "123");

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = MondayUtils.executeGraphQLQuery(mockedContext, "test query");

        assertEquals(responseMap, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(Map.of("query", "test query"), body.getContent());
    }
}
