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

package com.bytechef.component.neon.trigger;

import static com.bytechef.component.neon.constant.NeonConstants.ORDER_BY_COLUMN;
import static com.bytechef.component.neon.constant.NeonConstants.ORDER_DIRECTION;
import static com.bytechef.component.neon.constant.NeonConstants.TABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockContextSetupExtension.class)
class NeonNewRowTriggerTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Map<String, List<String>>> queryParametersArgumentCaptor = forClass(Map.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            TABLE, "posts",
            ORDER_BY_COLUMN, "id",
            ORDER_DIRECTION, "ASC"));

    @Test
    void testPollSeedsWithoutEmittingRows(
        TriggerContext mockedTriggerContext, Http.Response mockedResponse, Http.Executor mockedExecutor,
        Http mockedHttp, ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(List.of(Map.of("id", 5)));

        Parameters closureParameters = MockParametersFactory.create(Map.of());

        PollOutput pollOutput = NeonNewRowTrigger.poll(
            mockedParameters, null, closureParameters, mockedTriggerContext);

        assertEquals(List.of(), pollOutput.records());
        assertEquals(Map.of("lastValue", 5), pollOutput.closureParameters());
        assertEquals("/posts", stringArgumentCaptor.getValue());
        assertEquals(
            Map.of("order", List.of("id.asc"), "limit", List.of("1")),
            queryParametersArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        assertEquals(
            Http.ResponseType.Type.JSON, configurationBuilder.build()
                .getResponseType()
                .getType());
    }

    @Test
    void testPollEmitsNewRowsAscending(
        TriggerContext mockedTriggerContext, Http.Response mockedResponse, Http.Executor mockedExecutor,
        Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(List.of(Map.of("id", 6), Map.of("id", 7)));

        Parameters closureParameters = MockParametersFactory.create(Map.of("lastValue", 5));

        PollOutput pollOutput = NeonNewRowTrigger.poll(
            mockedParameters, null, closureParameters, mockedTriggerContext);

        assertEquals(List.of(Map.of("id", 6), Map.of("id", 7)), pollOutput.records());
        assertEquals(Map.of("lastValue", 7), pollOutput.closureParameters());
        assertEquals(Map.of("order", List.of("id.asc"), "id", List.of("gt.5")), queryParametersArgumentCaptor
            .getValue());
    }

    @Test
    void testPollWithNoNewRowsKeepsLastValue(
        TriggerContext mockedTriggerContext, Http.Response mockedResponse, Http.Executor mockedExecutor,
        Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(List.of());

        Parameters closureParameters = MockParametersFactory.create(Map.of("lastValue", 5));

        PollOutput pollOutput = NeonNewRowTrigger.poll(
            mockedParameters, null, closureParameters, mockedTriggerContext);

        assertTrue(pollOutput.records()
            .isEmpty());
        assertEquals(Map.of("lastValue", 5), pollOutput.closureParameters());
    }

    @Test
    void testPollThrowsOnInvalidOrderDirection(TriggerContext mockedTriggerContext) {
        Parameters invalidParameters = MockParametersFactory.create(
            Map.of(TABLE, "posts", ORDER_BY_COLUMN, "id", ORDER_DIRECTION, "SIDEWAYS"));
        Parameters closureParameters = MockParametersFactory.create(Map.of());

        assertThrows(
            IllegalArgumentException.class,
            () -> NeonNewRowTrigger.poll(invalidParameters, null, closureParameters, mockedTriggerContext));
    }
}
