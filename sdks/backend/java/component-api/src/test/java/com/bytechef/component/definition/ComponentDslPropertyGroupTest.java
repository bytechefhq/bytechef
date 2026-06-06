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

package com.bytechef.component.definition;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.propertyGroup;
import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentDslPropertyGroupTest {

    @Test
    void testComponentLevelInputsWrapsPropertyInSinglePropertyGroup() {
        ComponentDefinition componentDefinition = component("sample")
            .inputs(string("spreadsheetId").label("Spreadsheet"));

        Optional<List<? extends PropertyGroup>> inputs = componentDefinition.getInputs();

        assertTrue(inputs.isPresent());

        List<? extends PropertyGroup> inputList = inputs.get();

        assertEquals(1, inputList.size());

        PropertyGroup propertyGroup = inputList.getFirst();

        assertEquals("spreadsheetId", propertyGroup.getName());
        assertEquals(1, propertyGroup.getProperties()
            .size());
        assertEquals("spreadsheetId", propertyGroup.getProperties()
            .getFirst()
            .getName());
    }

    @Test
    void testComponentLevelInputsWithExplicitGroups() {
        ComponentDefinition componentDefinition = component("sample")
            .inputs(
                propertyGroup("sheetSelection")
                    .label("Sheet")
                    .properties(
                        string("spreadsheetId").label("Spreadsheet"),
                        string("sheetName").label("Sheet")
                            .optionsLookupDependsOn("spreadsheetId")));

        Optional<List<? extends PropertyGroup>> inputs = componentDefinition.getInputs();

        assertTrue(inputs.isPresent());

        List<? extends PropertyGroup> inputList = inputs.get();

        assertEquals(1, inputList.size());

        PropertyGroup propertyGroup = inputList.getFirst();

        assertEquals("sheetSelection", propertyGroup.getName());
        assertEquals(Optional.of("Sheet"), propertyGroup.getLabel());
        assertEquals(2, propertyGroup.getProperties()
            .size());
    }
}
