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

package com.bytechef.platform.workflow.task.dispatcher.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property.Type;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class PropertyTest {

    @Test
    void testReadFileEntryAndTaskProperties() {
        List<Property> properties = JsonUtils.read(
            "[{\"name\":\"file\",\"type\":\"FILE_ENTRY\"},{\"name\":\"task\",\"type\":\"TASK\"}]",
            new TypeReference<>() {});

        assertThat(properties).hasExactlyElementsOfTypes(FileEntryProperty.class, TaskProperty.class);
    }

    @Test
    void testEveryPropertyTypeHasAJsonSubtype() {
        JsonSubTypes jsonSubTypes = Property.class.getAnnotation(JsonSubTypes.class);

        List<String> subtypeNames = Arrays.stream(jsonSubTypes.value())
            .map(JsonSubTypes.Type::name)
            .toList();

        List<String> typeNames = Arrays.stream(Type.values())
            .map(Type::name)
            .toList();

        assertThat(subtypeNames).containsExactlyInAnyOrderElementsOf(typeNames);
    }
}
