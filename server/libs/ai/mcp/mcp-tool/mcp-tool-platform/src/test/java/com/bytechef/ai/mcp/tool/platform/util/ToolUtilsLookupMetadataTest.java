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

package com.bytechef.ai.mcp.tool.platform.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.domain.OptionsDataSource;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.domain.BaseProperty;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ToolUtilsLookupMetadataTest {

    @Test
    void emitsLookupRequiredWhenPropertyHasOptionsDataSource() {
        OptionsDataSource optionsDataSource = mock(OptionsDataSource.class);

        when(optionsDataSource.getOptionsLookupDependsOn()).thenReturn(List.of());

        StringProperty property = mock(StringProperty.class);

        when(property.getName()).thenReturn("channel");
        when(property.getRequired()).thenReturn(true);
        when(property.getOptionsDataSource()).thenReturn(optionsDataSource);

        String json = ToolUtils.generateParametersJson(List.<BaseProperty>of(property));

        assertTrue(
            json.contains("\"lookupRequired\": true"),
            "Expected lookupRequired:true for property with OptionsDataSource. Got: " + json);
        assertTrue(
            json.contains("\"lookupDependsOn\": []"),
            "Expected lookupDependsOn:[] when no dependencies declared. Got: " + json);
    }

    @Test
    void omitsLookupFieldsWhenPropertyHasNoOptionsDataSource() {
        StringProperty property = mock(StringProperty.class);

        when(property.getName()).thenReturn("text");
        when(property.getRequired()).thenReturn(false);
        when(property.getOptionsDataSource()).thenReturn(null);

        String json = ToolUtils.generateParametersJson(List.<BaseProperty>of(property));

        assertFalse(
            json.contains("lookupRequired"),
            "Expected no lookupRequired field for plain property. Got: " + json);
        assertFalse(
            json.contains("lookupDependsOn"),
            "Expected no lookupDependsOn field for plain property. Got: " + json);
    }

    @Test
    void emitsLookupDependsOnWhenDataSourceDeclaresDependencies() {
        OptionsDataSource optionsDataSource = mock(OptionsDataSource.class);

        when(optionsDataSource.getOptionsLookupDependsOn()).thenReturn(List.of("spreadsheetId", "sheetTab"));

        StringProperty property = mock(StringProperty.class);

        when(property.getName()).thenReturn("sheet");
        when(property.getRequired()).thenReturn(true);
        when(property.getOptionsDataSource()).thenReturn(optionsDataSource);

        String json = ToolUtils.generateParametersJson(List.<BaseProperty>of(property));

        assertTrue(
            json.contains("\"lookupDependsOn\": [\"spreadsheetId\", \"sheetTab\"]"),
            "Expected lookupDependsOn list with both dependencies. Got: " + json);
        assertTrue(
            json.contains("\"lookupRequired\": true"),
            "Expected lookupRequired:true alongside dependencies. Got: " + json);
    }
}
