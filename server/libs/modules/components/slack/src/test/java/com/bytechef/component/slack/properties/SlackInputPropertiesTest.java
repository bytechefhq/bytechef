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

package com.bytechef.component.slack.properties;

import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.slack.properties.SlackInputProperties.ATTACHMENTS;
import static com.bytechef.component.slack.properties.SlackInputProperties.BLOCKS;
import static com.bytechef.component.slack.properties.SlackInputProperties.CONTENT_TYPE;
import static com.bytechef.component.slack.properties.SlackInputProperties.TEXT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 */
public final class SlackInputPropertiesTest {
    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Parameters mockedParameters = mock(Parameters.class);

    @Test
    public void testGetContentTypeProperties_attachments() {
        when(mockedParameters.getRequiredString(CONTENT_TYPE))
            .thenReturn(ATTACHMENTS);

        List<? extends Property.ValueProperty<?>> propertyList =
            SlackInputProperties.getContentTypeProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(1, propertyList.size());
        Assertions.assertEquals(
            string(ATTACHMENTS)
                .label("Attachments")
                .description(
                    "A JSON-based array of structured attachments, presented as a URL-encoded string.")
                .required(true),
            propertyList.getFirst());
    }

    @Test
    public void testGetContentTypeProperties_blocks() {
        when(mockedParameters.getRequiredString(CONTENT_TYPE))
            .thenReturn(BLOCKS);

        List<? extends Property.ValueProperty<?>> propertyList =
            SlackInputProperties.getContentTypeProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(1, propertyList.size());
        Assertions.assertEquals(
            string(BLOCKS)
                .label("Blocks")
                .description(
                    "A JSON-based array of structured blocks, presented as a URL-encoded string.")
                .required(true),
            propertyList.getFirst());
    }

    @Test
    public void testGetContentTypeProperties_text() {
        when(mockedParameters.getRequiredString(CONTENT_TYPE))
            .thenReturn(TEXT);

        List<? extends Property.ValueProperty<?>> propertyList =
            SlackInputProperties.getContentTypeProperties(mockedParameters, mockedParameters, mockedContext);

        Assertions.assertEquals(1, propertyList.size());
        Assertions.assertEquals(
            string(TEXT)
                .label("Text")
                .description(
                    "How this field works and whether it is required depends on other fields you use in your API call.")
                .required(true),
            propertyList.getFirst());
    }
}
