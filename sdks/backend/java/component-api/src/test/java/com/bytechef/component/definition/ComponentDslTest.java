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

import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.ActionDefinition.PropertiesFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableDynamicPropertiesProperty;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentDslTest {

    private static final PropertiesFunction PROPERTIES_FUNCTION =
        (inputParameters, connectionParameters, lookupDependsOnPaths, context) -> List.of();

    @Test
    void testDynamicPropertiesDataSourceCarriesPropertiesFunction() {
        ModifiableDynamicPropertiesProperty dynamicPropertiesProperty =
            dynamicProperties("inputs", PROPERTIES_FUNCTION)
                .propertiesLookupDependsOn("workflowUuid");

        PropertiesDataSource<?> propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        assertSame(PROPERTIES_FUNCTION, propertiesDataSource.getProperties());
        assertEquals(List.of("workflowUuid"), propertiesDataSource.getPropertiesLookupDependsOn());
    }

    @Test
    void testDynamicPropertiesDataSourceWithoutLookupDependenciesHasEmptyDependencies() {
        ModifiableDynamicPropertiesProperty dynamicPropertiesProperty =
            dynamicProperties("inputs", PROPERTIES_FUNCTION);

        PropertiesDataSource<?> propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        assertEquals(List.of(), propertiesDataSource.getPropertiesLookupDependsOn());
    }

    @Test
    void testDynamicPropertiesRejectsNullPropertiesFunction() {
        NullPointerException exception = assertThrows(
            NullPointerException.class,
            () -> dynamicProperties("inputs", (PropertiesFunction) null));

        assertTrue(exception.getMessage()
            .contains("propertiesFunction"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDeprecatedDynamicPropertiesKeepsPropertiesSetter() {
        ModifiableDynamicPropertiesProperty dynamicPropertiesProperty = dynamicProperties("inputs")
            .properties(PROPERTIES_FUNCTION);

        PropertiesDataSource<?> propertiesDataSource = dynamicPropertiesProperty.getDynamicPropertiesDataSource();

        assertSame(PROPERTIES_FUNCTION, propertiesDataSource.getProperties());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDeprecatedDynamicPropertiesWithoutPropertiesFunctionFailsOnRead() {
        ModifiableDynamicPropertiesProperty dynamicPropertiesProperty = dynamicProperties("inputs")
            .propertiesLookupDependsOn("workflowUuid");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class, dynamicPropertiesProperty::getDynamicPropertiesDataSource);

        assertTrue(exception.getMessage()
            .contains("inputs"));
    }
}
