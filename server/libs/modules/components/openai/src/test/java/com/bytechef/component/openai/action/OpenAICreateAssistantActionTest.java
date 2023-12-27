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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.component.definition.Context.TypeReference;
import com.theokanning.openai.assistants.Assistant;
import com.theokanning.openai.assistants.AssistantRequest;
import com.theokanning.openai.assistants.Tool;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
public class OpenAICreateAssistantActionTest extends AbstractOpenAIActionTest {

    @Test
    public void testPerform() {
        Assistant mockedAssistant = Mockito.mock(Assistant.class);
        ArgumentCaptor<AssistantRequest> assistantRequestArgumentCaptor = ArgumentCaptor.forClass(
            AssistantRequest.class);
        List<Tool> tools = List.of(new Tool());
        List<String> strings = List.of("a");

        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn("MODEL");
        when(mockedParameters.getString(NAME))
            .thenReturn("NAME");
        when(mockedParameters.getString(DESCRIPTION))
            .thenReturn("DESCRIPTION");
        when(mockedParameters.getString(INSTRUCTIONS))
            .thenReturn("INSTRUCTIONS");
        when(mockedParameters.getList(eq(TOOLS), any(TypeReference.class)))
            .thenReturn(tools);
        when(mockedParameters.getList(eq(FILE_IDS), any(TypeReference.class)))
            .thenReturn(strings);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction = Mockito.mockConstruction(
            OpenAiService.class,
            (mock, context) -> when(mock.createAssistant(assistantRequestArgumentCaptor.capture()))
                .thenReturn(mockedAssistant))) {

            Assistant assistant = OpenAICreateAssistantAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            List<OpenAiService> openAiServices = openAiServiceMockedConstruction.constructed();

            assertEquals(1, openAiServices.size());
            assertEquals(mockedAssistant, assistant);

            OpenAiService openAiService = openAiServices.getFirst();

            verify(openAiService, times(1)).createAssistant(assistantRequestArgumentCaptor.capture());

            AssistantRequest assistantRequest = assistantRequestArgumentCaptor.getValue();

            assertEquals("MODEL", assistantRequest.getModel());
            assertEquals("NAME", assistantRequest.getName());
            assertEquals("DESCRIPTION", assistantRequest.getDescription());
            assertEquals("INSTRUCTIONS", assistantRequest.getInstructions());
            assertEquals(tools, assistantRequest.getTools());
            assertEquals(strings, assistantRequest.getFileIds());
        }
    }
}
