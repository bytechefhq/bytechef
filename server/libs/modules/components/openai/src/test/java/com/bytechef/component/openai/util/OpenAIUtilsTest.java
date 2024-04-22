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

package com.bytechef.component.openai.util;

import static com.bytechef.component.openai.constant.OpenAIConstants.DALL_E_2;
import static com.bytechef.component.openai.constant.OpenAIConstants.DALL_E_3;
import static com.bytechef.component.openai.constant.OpenAIConstants.DEFAULT_SIZE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.openai.action.AbstractOpenAIActionTest;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

/**
 * @author Monika Domiter
 */
class OpenAIUtilsTest extends AbstractOpenAIActionTest {

    @Test
     void testGetSizeOptionsForDallE2() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_2);

        List<Option<String>> sizeOptions = OpenAIUtils.getSizeOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        Assertions.assertEquals(3, sizeOptions.size());
        Assertions.assertEquals("256x256", sizeOptions.get(0).getLabel());
        Assertions.assertEquals("256x256", sizeOptions.get(0).getValue());
        Assertions.assertEquals("512x512", sizeOptions.get(1).getLabel());
        Assertions.assertEquals("512x512", sizeOptions.get(1).getValue());
        Assertions.assertEquals(DEFAULT_SIZE, sizeOptions.get(2).getLabel());
        Assertions.assertEquals(DEFAULT_SIZE, sizeOptions.get(2).getValue());
    }

    @Test
     void testGetSizeOptionsForDallE3() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_3);

        List<Option<String>> sizeOptions = OpenAIUtils.getSizeOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        Assertions.assertEquals(3, sizeOptions.size());
        Assertions.assertEquals(DEFAULT_SIZE, sizeOptions.get(0).getLabel());
        Assertions.assertEquals(DEFAULT_SIZE, sizeOptions.get(0).getValue());
        Assertions.assertEquals("1792x1024", sizeOptions.get(1).getLabel());
        Assertions.assertEquals("1792x1024", sizeOptions.get(1).getValue());
        Assertions.assertEquals("1024x1792", sizeOptions.get(2).getLabel());
        Assertions.assertEquals("1024x1792", sizeOptions.get(2).getValue());
    }

    @Test
    void testGetModelPropertiesForDallE2() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_2);

        List<ValueProperty<?>> modelProperties = OpenAIUtils.getModelProperties(
            mockedParameters, mockedParameters, Map.of(), mockedContext);

        Assertions.assertEquals(2, modelProperties.size());

        ValueProperty<?> property = modelProperties.getFirst();

        Assertions.assertEquals("Prompt", property.getLabel().get());
        Assertions.assertEquals("A text description of the desired image(s).", property.getDescription().get());
        Assertions.assertEquals(true, property.getRequired().get());
        Assertions.assertEquals(1000, ((ModifiableStringProperty) property).getMaxLength().get());

        property = modelProperties.get(1);

        Assertions.assertEquals("n", property.getLabel().get());
        Assertions.assertEquals(
            "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported.",
            property.getDescription().get());
        Assertions.assertEquals(1L, property.getDefaultValue().get());
        Assertions.assertEquals(false, property.getRequired().get());
        Assertions.assertEquals(10, ((ComponentDSL.ModifiableIntegerProperty) property).getMaxValue().get());
        Assertions.assertEquals(1, ((ComponentDSL.ModifiableIntegerProperty) property).getMinValue().get());
    }

    @Test
    void testGetModelPropertiesForDallE3() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_3);

        List<ValueProperty<?>> modelProperties = OpenAIUtils.getModelProperties(
            mockedParameters, mockedParameters, Map.of(), mockedContext);

        Assertions.assertEquals(2, modelProperties.size());

        ValueProperty<?> property = modelProperties.getFirst();

        Assertions.assertEquals("Prompt", property.getLabel().get());
        Assertions.assertEquals("A text description of the desired image(s).", property.getDescription().get());
        Assertions.assertEquals(true, property.getRequired().get());
        Assertions.assertEquals(4000, ((ModifiableStringProperty) property).getMaxLength().get());

        property = modelProperties.get(1);

        Assertions.assertEquals("n", property.getLabel().get());
        Assertions.assertEquals(
            "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported.",
            property.getDescription().get());
        Assertions.assertEquals(1L, property.getDefaultValue().get());
        Assertions.assertEquals(false, property.getRequired().get());
        Assertions.assertEquals(Optional.empty(), ((ComponentDSL.ModifiableIntegerProperty) property).getMaxValue());
        Assertions.assertEquals(Optional.empty(), ((ComponentDSL.ModifiableIntegerProperty) property).getMinValue());
    }

    @Test
    void testGetModelOptions() {
        Model model1 = new Model();

        model1.setId("gpt-4");

        Model model2 = new Model();

        model2.setId("other-model");

        List<Model> models = List.of(model1, model2);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction = mockConstruction(
            OpenAiService.class,
            (openAiService, context) -> when(openAiService.listModels()).thenReturn(models))) {

            List<Option<String>> result =
                OpenAIUtils.getModelOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext);

            assertEquals(1, result.size());

            Option<String> option = result.getFirst();

            assertEquals("gpt-4", option.getLabel());
            assertEquals("gpt-4", option.getValue());
        }
    }
}
