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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PropertyFactoryTest {

    @Test
    void testJsonSchemaPropertyMarksFieldsListedAsRequired() {
        String jsonSchema = """
            {
                "type": "object",
                "required": ["customer"],
                "properties": {
                    "customer": {
                        "type": "object",
                        "required": ["email"],
                        "properties": {
                            "email": {"type": "string"},
                            "phone": {"type": "string"}
                        }
                    },
                    "note": {"type": "string"}
                }
            }""";

        BaseObjectProperty<?> objectProperty = (BaseObjectProperty<?>) Objects.requireNonNull(
            SchemaUtils.getJsonSchemaProperty(jsonSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY));

        BaseObjectProperty<?> customerProperty = (BaseObjectProperty<?>) getChildProperty(objectProperty, "customer");

        assertThat(customerProperty.getRequired()).isTrue();
        assertThat(getChildProperty(objectProperty, "note").getRequired()).isFalse();
        assertThat(getChildProperty(customerProperty, "email").getRequired()).isTrue();
        assertThat(getChildProperty(customerProperty, "phone").getRequired()).isFalse();
    }

    private static BaseProperty getChildProperty(BaseObjectProperty<?> objectProperty, String name) {
        return objectProperty.getProperties()
            .stream()
            .filter(property -> name.equals(property.getName()))
            .findFirst()
            .orElseThrow();
    }
}
