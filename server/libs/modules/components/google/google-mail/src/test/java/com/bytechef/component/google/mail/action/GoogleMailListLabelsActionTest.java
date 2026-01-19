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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class GoogleMailListLabelsActionTest {

    private final ArgumentCaptor<ActionContext> actionContextArgumentCaptor = ArgumentCaptor.forClass(
        ActionContext.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass(Object.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {

        List<Option<String>> mockedLabelOptions = List.of(
            option("label1", "1"),
            option("label2", "2"),
            option("label3", "3"));

        try (MockedStatic<GoogleMailUtils> googleServicesMockedStatic = mockStatic(GoogleMailUtils.class)) {
            googleServicesMockedStatic.when(() -> GoogleMailUtils.getLabelOptions(
                parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(),
                (Map<String, String>) objectArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                actionContextArgumentCaptor.capture()))
                .thenReturn(mockedLabelOptions);

            List<Map<String, String>> result = GoogleMailListLabelsAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            List<Map<String, String>> expected = List.of(
                Map.of(NAME, "label1", ID, "1"),
                Map.of(NAME, "label2", ID, "2"),
                Map.of(NAME, "label3", ID, "3"));

            assertEquals(expected, result);

            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
            assertNotNull(stringArgumentCaptor.getValue());
            assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
            assertEquals(Map.of(), objectArgumentCaptor.getValue());
        }
    }
}
