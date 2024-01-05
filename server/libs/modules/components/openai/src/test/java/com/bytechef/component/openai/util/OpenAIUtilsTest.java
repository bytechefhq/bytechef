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
import static com.bytechef.component.openai.constant.OpenAIConstants.STREAM;
import static org.mockito.Mockito.when;

import com.bytechef.component.openai.action.AbstractOpenAIActionTest;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.OptionsDataSource;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource;
import com.bytechef.hermes.component.definition.PropertiesDataSource;
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

        OptionsDataSource.OptionsResponse sizeOptions =
            OpenAIUtils.getSizeOptions(mockedParameters, mockedParameters, "", mockedContext);

        Assertions.assertEquals(3, sizeOptions.options()
            .size());
        Assertions.assertEquals("256x256", sizeOptions.options()
            .get(0)
            .getLabel());
        Assertions.assertEquals("256x256", sizeOptions.options()
            .get(0)
            .getValue());
        Assertions.assertEquals("512x512", sizeOptions.options()
            .get(1)
            .getLabel());
        Assertions.assertEquals("512x512", sizeOptions.options()
            .get(1)
            .getValue());
        Assertions.assertEquals(DEFAULT_SIZE, sizeOptions.options()
            .get(2)
            .getLabel());
        Assertions.assertEquals(DEFAULT_SIZE, sizeOptions.options()
            .get(2)
            .getValue());
    }

    @Test
    public void testGetSizeOptionsForDallE3() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_3);

        OptionsDataSource.OptionsResponse sizeOptions =
            OpenAIUtils.getSizeOptions(mockedParameters, mockedParameters, "", mockedContext);

        Assertions.assertEquals(3, sizeOptions.options()
            .size());
        Assertions.assertEquals(DEFAULT_SIZE, sizeOptions.options()
            .get(0)
            .getLabel());
        Assertions.assertEquals(DEFAULT_SIZE, sizeOptions.options()
            .get(0)
            .getValue());
        Assertions.assertEquals("1792x1024", sizeOptions.options()
            .get(1)
            .getLabel());
        Assertions.assertEquals("1792x1024", sizeOptions.options()
            .get(1)
            .getValue());
        Assertions.assertEquals("1024x1792", sizeOptions.options()
            .get(2)
            .getLabel());
        Assertions.assertEquals("1024x1792", sizeOptions.options()
            .get(2)
            .getValue());
    }

    @Test
    public void testGetModelPropertiesForDallE2() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_2);

        PropertiesDataSource.PropertiesResponse modelProperties =
            OpenAIUtils.getModelProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(2, modelProperties.properties()
            .size());
        Assertions.assertEquals("Prompt", modelProperties.properties()
            .getFirst()
            .getLabel()
            .get());
        Assertions.assertEquals("A text description of the desired image(s).", modelProperties.properties()
            .getFirst()
            .getDescription()
            .get());
        Assertions.assertEquals(true, modelProperties.properties()
            .getFirst()
            .getRequired()
            .get());
        Assertions.assertEquals(1000, ((ComponentDSL.ModifiableStringProperty) modelProperties.properties()
            .getFirst()).getMaxLength()
            .get());
        Assertions.assertEquals("n", modelProperties.properties()
            .get(1)
            .getLabel()
            .get());
        Assertions.assertEquals(
            "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported.",
            modelProperties.properties()
                .get(1)
                .getDescription()
                .get());
        Assertions.assertEquals(1L, modelProperties.properties()
            .get(1)
            .getDefaultValue()
            .get());
        Assertions.assertEquals(false, modelProperties.properties()
            .get(1)
            .getRequired()
            .get());
        Assertions.assertEquals(10, ((ComponentDSL.ModifiableIntegerProperty) modelProperties.properties()
            .get(1)).getMaxValue()
            .get());
        Assertions.assertEquals(1, ((ComponentDSL.ModifiableIntegerProperty) modelProperties.properties()
            .get(1)).getMinValue()
            .get());
    }

    @Test
    public void testGetModelPropertiesForDallE3() {
        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn(DALL_E_3);

        PropertiesDataSource.PropertiesResponse modelProperties =
            OpenAIUtils.getModelProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(2, modelProperties.properties()
            .size());
        Assertions.assertEquals("Prompt", modelProperties.properties()
            .getFirst()
            .getLabel()
            .get());
        Assertions.assertEquals("A text description of the desired image(s).", modelProperties.properties()
            .getFirst()
            .getDescription()
            .get());
        Assertions.assertEquals(true, modelProperties.properties()
            .getFirst()
            .getRequired()
            .get());
        Assertions.assertEquals(4000, ((ComponentDSL.ModifiableStringProperty) modelProperties.properties()
            .getFirst()).getMaxLength()
            .get());
        Assertions.assertEquals("n", modelProperties.properties()
            .get(1)
            .getLabel()
            .get());
        Assertions.assertEquals(
            "The number of images to generate. Must be between 1 and 10. For dall-e-3, only n=1 is supported.",
            modelProperties.properties()
                .get(1)
                .getDescription()
                .get());
        Assertions.assertEquals(1L, modelProperties.properties()
            .get(1)
            .getDefaultValue()
            .get());
        Assertions.assertEquals(false, modelProperties.properties()
            .get(1)
            .getRequired()
            .get());
        Assertions.assertEquals(Optional.empty(),
            ((ComponentDSL.ModifiableIntegerProperty) modelProperties.properties()
                .get(1)).getMaxValue());
        Assertions.assertEquals(Optional.empty(),
            ((ComponentDSL.ModifiableIntegerProperty) modelProperties.properties()
                .get(1)).getMinValue());
    }

    @Test
    public void testGetOutputSchemaFunctionForStream() {
        when(mockedParameters.getRequiredBoolean(STREAM)).thenReturn(true);

        OutputSchemaDataSource.OutputSchemaResponse outputSchemaFunction =
            OpenAIUtils.getOutputSchemaResponse(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(OpenAIUtils.outputSchemaResponseForStream, outputSchemaFunction);
    }

    @Test
    public void testGetOutputSchemaFunction() {
        when(mockedParameters.getRequiredBoolean(STREAM)).thenReturn(false);

        OutputSchemaDataSource.OutputSchemaResponse outputSchemaFunction =
            OpenAIUtils.getOutputSchemaResponse(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(OpenAIUtils.outputSchemaResponse, outputSchemaFunction);
    }
}
