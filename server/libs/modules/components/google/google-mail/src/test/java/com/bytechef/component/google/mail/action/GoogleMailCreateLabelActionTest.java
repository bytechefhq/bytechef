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

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users;
import com.google.api.services.gmail.Gmail.Users.Labels;
import com.google.api.services.gmail.model.Label;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Ivona Pavela
 */
class GoogleMailCreateLabelActionTest {

    private final ArgumentCaptor<Label> labelArgumentCaptor = forClass(Label.class);
    private final Labels.Create mockedCreate = mock(Labels.Create.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Label mockedLabel = mock(Label.class);
    private final Labels mockedLabels = mock(Labels.class);
    private final Users mockedUsers = mock(Users.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() throws IOException {
        Parameters parameters = MockParametersFactory.create(Map.of(NAME, "test"));

        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic.when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.labels())
                .thenReturn(mockedLabels);
            when(mockedLabels.create(
                stringArgumentCaptor.capture(), labelArgumentCaptor.capture()))
                    .thenReturn(mockedCreate);
            when(mockedCreate.execute())
                .thenReturn(mockedLabel);

            Label result = GoogleMailCreateLabelAction.perform(parameters, parameters, mock(ActionContext.class));

            assertEquals(mockedLabel, result);
            assertEquals(parameters, parametersArgumentCaptor.getValue());
            assertEquals(List.of(ME), stringArgumentCaptor.getAllValues());

            Label label = new Label().setName("test")
                .setLabelListVisibility("labelShow")
                .setMessageListVisibility("show");

            assertEquals(label, labelArgumentCaptor.getValue());
        }
    }
}
