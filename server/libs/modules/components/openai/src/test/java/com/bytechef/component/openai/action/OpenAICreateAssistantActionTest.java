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

package com.bytechef.component.openai.action;

import static com.bytechef.component.openai.constant.OpenAIConstants.DESCRIPTION;
import static com.bytechef.component.openai.constant.OpenAIConstants.FILE_IDS;
import static com.bytechef.component.openai.constant.OpenAIConstants.INSTRUCTIONS;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.NAME;
import static com.bytechef.component.openai.constant.OpenAIConstants.TOOLS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.assistants.AssistantRequest;
import com.theokanning.openai.assistants.Tool;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
 class OpenAICreateAssistantActionTest extends AbstractOpenAIActionTest {

    @Test
     void testPerform() {
        Assistant assistant = Mockito.mock(Assistant.class);
        ArgumentCaptor<AssistantRequest> assistantRequestArgumentCaptor =
            ArgumentCaptor.forClass(AssistantRequest.class);

        List<Tool> toolList = List.of(new Tool());
        List<String> stringList = List.of("a");

        Mockito.when(parameterMap.getRequiredString(MODEL))
            .thenReturn("MODEL");
        Mockito.when(parameterMap.getString(NAME))
            .thenReturn("NAME");
        Mockito.when(parameterMap.getString(DESCRIPTION))
            .thenReturn("DESCRIPTION");
        Mockito.when(parameterMap.getString(INSTRUCTIONS))
            .thenReturn("INSTRUCTIONS");
        Mockito.when((List<Tool>) parameterMap.getList(TOOLS))
            .thenReturn(toolList);
        Mockito.when((List<String>) parameterMap.getList(FILE_IDS))
            .thenReturn(stringList);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction =
            Mockito.mockConstruction(OpenAiService.class,
                (mock, context) -> when(mock.createAssistant(assistantRequestArgumentCaptor.capture()))
                    .thenReturn(assistant))) {

            Assistant perform = OpenAICreateAssistantAction.perform(parameterMap, parameterMap, context);

            Assertions.assertEquals(1, openAiServiceMockedConstruction.constructed()
                .size());
            Assertions.assertEquals(assistant, perform);

            OpenAiService mock = openAiServiceMockedConstruction.constructed()
                .getFirst();
            verify(mock).createAssistant(assistantRequestArgumentCaptor.capture());
            verify(mock, times(1)).createAssistant(assistantRequestArgumentCaptor.capture());

            Assertions.assertEquals("MODEL", assistantRequestArgumentCaptor.getValue()
                .getModel());
            Assertions.assertEquals("NAME", assistantRequestArgumentCaptor.getValue()
                .getName());
            Assertions.assertEquals("DESCRIPTION", assistantRequestArgumentCaptor.getValue()
                .getDescription());
            Assertions.assertEquals("INSTRUCTIONS", assistantRequestArgumentCaptor.getValue()
                .getInstructions());
            Assertions.assertEquals(toolList, assistantRequestArgumentCaptor.getValue()
                .getTools());
            Assertions.assertEquals(stringList, assistantRequestArgumentCaptor.getValue()
                .getFileIds());
        }
    }

}
