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

package com.bytechef.component.airtable.trigger;

import static com.bytechef.component.airtable.constant.AirtableConstants.BASE_ID;
import static com.bytechef.component.airtable.constant.AirtableConstants.TABLE_ID;
import static com.bytechef.component.airtable.trigger.AirtableNewRecordTrigger.LAST_TIME_CHECKED;
import static com.bytechef.component.airtable.trigger.AirtableNewRecordTrigger.TRIGGER_FIELD;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.airtable.util.AirtableUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Monika Kušter
 */
class AirtableNewRecordTriggerTest {

    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<Boolean> booleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Object[]> objectArgumentCaptor = ArgumentCaptor.forClass(Object[].class);

    @Test
    void testPoll() {
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0, 0);

        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(BASE_ID, "base", TABLE_ID, "abc", TRIGGER_FIELD, "field"));
        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        try (MockedStatic<AirtableUtils> airtableUtilsMockedStatic = mockStatic(AirtableUtils.class);
            MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
                LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(any(ZoneId.class)))
                .thenReturn(endDate);

            airtableUtilsMockedStatic.when(() -> AirtableUtils.getAllRecords(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                booleanArgumentCaptor.capture(), objectArgumentCaptor.capture()))
                .thenReturn(List.of());

            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(false);

            PollOutput pollOutput = AirtableNewRecordTrigger.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(new PollOutput(List.of(), Map.of(LAST_TIME_CHECKED, endDate), false), pollOutput);
            assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
            assertFalse(booleanArgumentCaptor.getValue());
            assertEquals(List.of("base", "abc"), stringArgumentCaptor.getAllValues());

            Object[] objects = {
                "filterByFormula",
                "IS_AFTER({field}, DATETIME_PARSE('%s', 'YYYY-MM-DD HH:mm:ss'))"
                    .formatted(startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            };

            assertArrayEquals(objects, objectArgumentCaptor.getValue());
        }
    }
}
