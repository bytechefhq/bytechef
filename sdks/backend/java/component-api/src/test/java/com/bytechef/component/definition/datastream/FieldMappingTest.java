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

package com.bytechef.component.definition.datastream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class FieldMappingTest {

    @Test
    void testCanonicalConstructor() {
        FieldMapping fieldMapping = new FieldMapping("sourceField", "destinationField", "defaultValue");

        assertEquals("sourceField", fieldMapping.sourceField());
        assertEquals("destinationField", fieldMapping.destinationField());
        assertEquals("defaultValue", fieldMapping.defaultValue());
    }

    @Test
    void testCanonicalConstructorWithNullDefaultValue() {
        FieldMapping fieldMapping = new FieldMapping("sourceField", "destinationField", null);

        assertEquals("sourceField", fieldMapping.sourceField());
        assertEquals("destinationField", fieldMapping.destinationField());
        assertNull(fieldMapping.defaultValue());
    }

    @Test
    void testCompactConstructor() {
        FieldMapping fieldMapping = new FieldMapping("sourceField", "destinationField");

        assertEquals("sourceField", fieldMapping.sourceField());
        assertEquals("destinationField", fieldMapping.destinationField());
        assertNull(fieldMapping.defaultValue());
    }

    @Test
    void testDefaultValueWithDifferentTypes() {
        FieldMapping fieldMappingWithInt = new FieldMapping("source", "dest", 42);

        assertEquals(42, fieldMappingWithInt.defaultValue());

        FieldMapping fieldMappingWithDouble = new FieldMapping("source", "dest", 2.5);

        assertEquals(2.5, fieldMappingWithDouble.defaultValue());

        FieldMapping fieldMappingWithBoolean = new FieldMapping("source", "dest", true);

        assertEquals(true, fieldMappingWithBoolean.defaultValue());
    }

    @Test
    void testEquality() {
        FieldMapping fieldMapping1 = new FieldMapping("source", "dest", "default");
        FieldMapping fieldMapping2 = new FieldMapping("source", "dest", "default");
        FieldMapping fieldMapping3 = new FieldMapping("source", "dest", "other");

        assertEquals(fieldMapping1, fieldMapping2);
        assertEquals(fieldMapping1.hashCode(), fieldMapping2.hashCode());
        assertNotEquals(fieldMapping1, fieldMapping3);
    }

    @Test
    void testToString() {
        FieldMapping fieldMapping = new FieldMapping("source", "dest", "default");

        String expected = "FieldMapping[sourceField=source, destinationField=dest, defaultValue=default]";

        assertEquals(expected, fieldMapping.toString());
    }
}
