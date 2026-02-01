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

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class FieldDefinitionTest {

    @Test
    void testCanonicalConstructor() {
        FieldDefinition fieldDefinition = new FieldDefinition("firstName", "First Name", Integer.class);

        assertEquals("firstName", fieldDefinition.name());
        assertEquals("First Name", fieldDefinition.label());
        assertEquals(Integer.class, fieldDefinition.type());
    }

    @Test
    void testCompactConstructorWithNameAndLabel() {
        FieldDefinition fieldDefinition = new FieldDefinition("firstName", "First Name");

        assertEquals("firstName", fieldDefinition.name());
        assertEquals("First Name", fieldDefinition.label());
        assertEquals(String.class, fieldDefinition.type());
    }

    @Test
    void testCompactConstructorWithNameOnly() {
        FieldDefinition fieldDefinition = new FieldDefinition("firstName");

        assertEquals("firstName", fieldDefinition.name());
        assertEquals("firstName", fieldDefinition.label());
        assertEquals(String.class, fieldDefinition.type());
    }

    @Test
    void testEquality() {
        FieldDefinition fieldDefinition1 = new FieldDefinition("name", "label", String.class);
        FieldDefinition fieldDefinition2 = new FieldDefinition("name", "label", String.class);
        FieldDefinition fieldDefinition3 = new FieldDefinition("name", "label", Integer.class);

        assertEquals(fieldDefinition1, fieldDefinition2);
        assertEquals(fieldDefinition1.hashCode(), fieldDefinition2.hashCode());
        assertNotEquals(fieldDefinition1, fieldDefinition3);
    }

    @Test
    void testToString() {
        FieldDefinition fieldDefinition = new FieldDefinition("name", "label", String.class);

        String expected = "FieldDefinition[name=name, label=label, type=class java.lang.String]";

        assertEquals(expected, fieldDefinition.toString());
    }
}
