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

package com.bytechef.platform.ai.tool.util;

import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.component.domain.Property;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ToolPropertyUtilsTest {

    private static final Property TEXT_PROPERTY = Property.toProperty(
        string("text")
            .label("Text")
            .required(true));

    @Test
    void testIsToolOverrideProperty() {
        assertTrue(ToolPropertyUtils.isToolOverrideProperty(ToolPropertyUtils.toolNameProperty("tool")));
        assertTrue(ToolPropertyUtils.isToolOverrideProperty(ToolPropertyUtils.toolDescriptionProperty("tool")));
        assertFalse(ToolPropertyUtils.isToolOverrideProperty(TEXT_PROPERTY));
    }

    @Test
    void testWithoutToolOverridePropertiesKeepsOnlyInputProperties() {
        List<? extends Property> properties = ToolPropertyUtils.withoutToolOverrideProperties(
            List.of(
                ToolPropertyUtils.toolNameProperty("tool"), ToolPropertyUtils.toolDescriptionProperty("tool"),
                TEXT_PROPERTY));

        assertEquals(1, properties.size());

        Property property = properties.getFirst();

        assertEquals("text", property.getName());
    }

    @Test
    void testWithoutToolOverridePropertiesLeavesUnrelatedPropertiesUntouched() {
        List<? extends Property> properties = ToolPropertyUtils.withoutToolOverrideProperties(List.of(TEXT_PROPERTY));

        assertEquals(List.of(TEXT_PROPERTY), properties);
    }
}
