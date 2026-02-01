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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class FieldsProviderTest {

    @Test
    void testDefaultGetFieldsReturnsEmptyList() {
        FieldsProvider fieldsProvider = new FieldsProvider() {};

        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);
        ClusterElementContext context = mock(ClusterElementContext.class);

        List<FieldDefinition> fields = fieldsProvider.getFields(inputParameters, connectionParameters, context);

        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }

    @Test
    void testCustomImplementation() {
        FieldsProvider fieldsProvider = new FieldsProvider() {

            @Override
            public List<FieldDefinition> getFields(
                Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context) {

                return List.of(
                    new FieldDefinition("id", "ID", Long.class),
                    new FieldDefinition("name", "Name", String.class),
                    new FieldDefinition("age", "Age", Integer.class));
            }
        };

        Parameters inputParameters = mock(Parameters.class);
        Parameters connectionParameters = mock(Parameters.class);
        ClusterElementContext context = mock(ClusterElementContext.class);

        List<FieldDefinition> fields = fieldsProvider.getFields(inputParameters, connectionParameters, context);

        assertEquals(3, fields.size());
        assertEquals("id", fields.get(0)
            .name());
        assertEquals(Long.class, fields.get(0)
            .type());
        assertEquals("name", fields.get(1)
            .name());
        assertEquals(String.class, fields.get(1)
            .type());
        assertEquals("age", fields.get(2)
            .name());
        assertEquals(Integer.class, fields.get(2)
            .type());
    }

    @Test
    void testGetFieldsWithNullParameters() {
        FieldsProvider fieldsProvider = new FieldsProvider() {};

        List<FieldDefinition> fields = fieldsProvider.getFields(null, null, null);

        assertNotNull(fields);
        assertTrue(fields.isEmpty());
    }
}
