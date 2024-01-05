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
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.STREAM;
import static org.mockito.Mockito.when;

import com.bytechef.component.openai.action.AbstractOpenAIActionTest;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableStringProperty;
import com.bytechef.hermes.component.definition.Property.OutputProperty;
import com.bytechef.hermes.definition.Option;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
public class OpenAIUtilsTest extends AbstractOpenAIActionTest {

    @Test
    public void testGetSizeOptionsForDallE2() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_2);

        List<Option<String>> sizeOptions =
            OpenAIUtils.getSizeOptions(mockedParameters, mockedParameters, "", mockedContext);

        Assertions.assertEquals(3, sizeOptions
            .size());
        Assertions.assertEquals("256x256", sizeOptions
            .get(0)
            .getLabel());
        Assertions.assertEquals("256x256", sizeOptions
            .get(0)
            .getValue());
        Assertions.assertEquals("512x512", sizeOptions
            .get(1)
            .getLabel());
        Assertions.assertEquals("512x512", sizeOptions
            .get(1)
            .getValue());
        Assertions.assertEquals("1024x1024", sizeOptions
            .get(2)
            .getLabel());
        Assertions.assertEquals("1024x1024", sizeOptions
            .get(2)
            .getValue());
    }

    @Test
    public void testGetSizeOptionsForDallE3() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_3);

        List<Option<String>> sizeOptions =
            OpenAIUtils.getSizeOptions(mockedParameters, mockedParameters, "", mockedContext);

        Assertions.assertEquals(3, sizeOptions
            .size());
        Assertions.assertEquals("1024x1024", sizeOptions
            .get(0)
            .getLabel());
        Assertions.assertEquals("1024x1024", sizeOptions
            .get(0)
            .getValue());
        Assertions.assertEquals("1792x1024", sizeOptions
            .get(1)
            .getLabel());
        Assertions.assertEquals("1792x1024", sizeOptions
            .get(1)
            .getValue());
        Assertions.assertEquals("1024x1792", sizeOptions
            .get(2)
            .getLabel());
        Assertions.assertEquals("1024x1792", sizeOptions
            .get(2)
            .getValue());
    }

    @Test
    public void testGetModelPropertiesForDallE2() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_2);

        List<ModifiableStringProperty> modelProperties =
            OpenAIUtils.getModelProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(1, modelProperties.size());
        Assertions.assertEquals("Prompt", modelProperties.getFirst()
            .getLabel()
            .get());
        Assertions.assertEquals("A text description of the desired image(s).", modelProperties.getFirst()
            .getDescription()
            .get());
        Assertions.assertEquals(true, modelProperties.getFirst()
            .getRequired()
            .get());
        Assertions.assertEquals(1000, ((ModifiableStringProperty) modelProperties.getFirst())
            .getMaxLength()
            .get());
    }

    @Test
    public void testGetModelPropertiesForDallE3() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_3);

        List<ModifiableStringProperty> modelProperties =
            OpenAIUtils.getModelProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(1, modelProperties
            .size());
        Assertions.assertEquals("Prompt", modelProperties.getFirst()
            .getLabel()
            .get());
        Assertions.assertEquals("A text description of the desired image(s).", modelProperties.getFirst()
            .getDescription()
            .get());
        Assertions.assertEquals(true, modelProperties.getFirst()
            .getRequired()
            .get());
        Assertions.assertEquals(4000, ((ModifiableStringProperty) modelProperties.getFirst())
            .getMaxLength()
            .get());
    }

    @Test
    public void testGetNumberOfImagesPropertiesForDallE2() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn("dall-e-2");

        List<ComponentDSL.ModifiableIntegerProperty> numberOfImagesProperties =
            OpenAIUtils.getNumberOfImagesProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(1, numberOfImagesProperties.size());
        Assertions.assertEquals("n", numberOfImagesProperties.getFirst()
            .getLabel()
            .get());
        Assertions.assertEquals(
            "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported.",
            numberOfImagesProperties.getFirst()
                .getDescription()
                .get());
        Assertions.assertEquals(1L, numberOfImagesProperties.getFirst()
            .getDefaultValue()
            .get());
        Assertions.assertEquals(false, numberOfImagesProperties.getFirst()
            .getRequired()
            .get());
        Assertions.assertEquals(10, ((ComponentDSL.ModifiableIntegerProperty) numberOfImagesProperties.getFirst())
            .getMaxValue()
            .get());
        Assertions.assertEquals(1, ((ComponentDSL.ModifiableIntegerProperty) numberOfImagesProperties.getFirst())
            .getMinValue()
            .get());
    }

    @Test
    public void testGetNumberOfImagesPropertiesForDallE3() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn("dall-e-3");

        List<ComponentDSL.ModifiableIntegerProperty> numberOfImagesProperties =
            OpenAIUtils.getNumberOfImagesProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(1, numberOfImagesProperties.size());
        Assertions.assertEquals("n", numberOfImagesProperties.getFirst()
            .getLabel()
            .get());
        Assertions.assertEquals(
            "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported.",
            numberOfImagesProperties.getFirst()
                .getDescription()
                .get());
        Assertions.assertEquals(1L, numberOfImagesProperties .getFirst()
            .getDefaultValue()
            .get());
        Assertions.assertEquals(false, numberOfImagesProperties.getFirst()
            .getRequired()
            .get());
        Assertions.assertEquals(Optional.empty(),
            ((ComponentDSL.ModifiableIntegerProperty) numberOfImagesProperties.getFirst()).getMaxValue());
        Assertions.assertEquals(Optional.empty(),
            ((ComponentDSL.ModifiableIntegerProperty) numberOfImagesProperties.getFirst()).getMinValue());
    }

    @Test
    public void testGetOutputSchemaFunctionForStream() {
        when(mockedParameters.getBoolean(STREAM)).thenReturn(true);

        OutputProperty<?> outputProperty =
            OpenAIUtils.getOutputSchemaResponse(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(OpenAIUtils.outputSchemaResponseForStream, outputProperty);
    }

    @Test
    public void testGetOutputSchemaFunction() {
        when(mockedParameters.getBoolean(STREAM)).thenReturn(false);

        OutputProperty<?> outputProperty =
            OpenAIUtils.getOutputSchemaResponse(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(OpenAIUtils.outputSchemaResponse, outputProperty);
    }
}
