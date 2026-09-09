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

package com.bytechef.platform.component.domain;

import static com.bytechef.component.definition.ComponentDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.definition.ComponentDsl.ModifiableStringProperty;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.ResourceType;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PropertyResourceTypeTest {

    @Test
    void resourceTypeIsReadFromMetadata() {
        StringProperty stringProperty = new StringProperty(string("table").resourceReference(ResourceType.DATA_TABLE));

        assertEquals(ResourceType.DATA_TABLE, stringProperty.getResourceType());
    }

    @Test
    void resourceTypeIsNullWithoutMarker() {
        StringProperty stringProperty = new StringProperty(string("table"));

        assertNull(stringProperty.getResourceType());
    }

    @Test
    void unknownResourceTypeIsReadAsNoResourceType() {
        ModifiableStringProperty modifiableStringProperty = string("table");

        modifiableStringProperty.metadata(BaseProperty.RESOURCE_REFERENCE_METADATA_KEY, "SPREADSHEET");

        StringProperty stringProperty = new StringProperty(modifiableStringProperty);

        assertNull(stringProperty.getResourceType());
    }
}
