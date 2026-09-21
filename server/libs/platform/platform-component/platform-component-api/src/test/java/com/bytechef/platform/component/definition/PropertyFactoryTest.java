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

package com.bytechef.platform.component.definition;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.FileEntryProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class PropertyFactoryTest {

    private static final Map<String, Object> FILE_ENTRY_MAP = Map.of(
        "extension", "pdf", "mimeType", "application/pdf", "name", "file.pdf", "url", "file:/editor/temp/file.pdf");

    @Test
    void testToOutputTypesWebhookFileBodyAsFileEntry() {
        OutputResponse outputResponse = SchemaUtils.toOutput(
            Map.of("method", "POST", "headers", Map.of(), "parameters", Map.of(), "body", FILE_ENTRY_MAP),
            PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);

        assertThat(outputResponse.outputSchema()).isInstanceOf(ObjectProperty.class);

        Property bodyProperty = getProperty((ObjectProperty) outputResponse.outputSchema(), "body");

        assertThat(bodyProperty).isInstanceOf(FileEntryProperty.class);
        assertThat(((FileEntryProperty) bodyProperty).getProperties())
            .extracting(Property::getName)
            .containsExactlyInAnyOrder("extension", "mimeType", "name", "url");
    }

    @Test
    void testToOutputTypesWebhookMethodAsString() {
        OutputResponse outputResponse = SchemaUtils.toOutput(
            Map.of("method", WebhookMethod.POST, "headers", Map.of(), "body", FILE_ENTRY_MAP),
            PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);

        ObjectProperty outputSchema = (ObjectProperty) outputResponse.outputSchema();

        assertThat(getProperty(outputSchema, "method")).isInstanceOf(StringProperty.class);
        assertThat(getProperty(outputSchema, "body")).isInstanceOf(FileEntryProperty.class);
        assertThat(getProperty(outputSchema, "headers")).isInstanceOf(ObjectProperty.class);
    }

    @Test
    void testToOutputTypesTopLevelEnumAsString() {
        OutputResponse outputResponse = SchemaUtils.toOutput(
            WebhookMethod.GET, PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);

        assertThat(outputResponse.outputSchema()).isInstanceOf(StringProperty.class);
    }

    @Test
    void testToOutputTypesFileEntryListItemsAsFileEntry() {
        OutputResponse outputResponse = SchemaUtils.toOutput(
            Map.of("files", List.of(FILE_ENTRY_MAP)), PropertyFactory.OUTPUT_FACTORY_FUNCTION,
            PropertyFactory.PROPERTY_FACTORY);

        Property filesProperty = getProperty((ObjectProperty) outputResponse.outputSchema(), "files");

        assertThat(filesProperty).isInstanceOf(ArrayProperty.class);
        assertThat(((ArrayProperty) filesProperty).getItems())
            .singleElement()
            .isInstanceOf(FileEntryProperty.class);
    }

    @Test
    void testToOutputKeepsJsonBodyAsObject() {
        OutputResponse outputResponse = SchemaUtils.toOutput(
            Map.of("method", "POST", "body", Map.of("invoiceId", "INV-1", "name", "Invoice")),
            PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);

        Property bodyProperty = getProperty((ObjectProperty) outputResponse.outputSchema(), "body");

        assertThat(bodyProperty).isInstanceOf(ObjectProperty.class);
        assertThat(((ObjectProperty) bodyProperty).getProperties())
            .extracting(Property::getName)
            .containsExactlyInAnyOrder("invoiceId", "name");
    }

    private static Property getProperty(ObjectProperty objectProperty, String name) {
        return objectProperty.getProperties()
            .stream()
            .filter(property -> name.equals(property.getName()))
            .findFirst()
            .orElseThrow();
    }
}
