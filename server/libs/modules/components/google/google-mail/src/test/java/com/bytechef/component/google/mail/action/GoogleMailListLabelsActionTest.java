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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.google.api.services.gmail.model.Label;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class GoogleMailListLabelsActionTest {

    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);

    @Test
    void testPerform() {
        List<Label> labels = List.of(
            new Label().setId("1")
                .setName("label1"),
            new Label().setId("2")
                .setName("label2"),
            new Label().setId("3")
                .setName("label3"));

        try (MockedStatic<GoogleMailUtils> googleServicesMockedStatic = mockStatic(GoogleMailUtils.class)) {
            googleServicesMockedStatic.when(() -> GoogleMailUtils.getLabels(parametersArgumentCaptor.capture()))
                .thenReturn(labels);

            List<Label> result = GoogleMailListLabelsAction.perform(
                mockedParameters, mockedParameters, mock(ActionContext.class));

            assertEquals(labels, result);
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
        }
    }
}
