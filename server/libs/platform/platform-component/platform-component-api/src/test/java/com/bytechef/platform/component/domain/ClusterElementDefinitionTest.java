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

import static com.bytechef.component.definition.ComponentDsl.clusterElement;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementDefinitionTest {

    @Test
    void testWithPrependedProperties() {
        com.bytechef.component.definition.ClusterElementDefinition<?> sdkDefinition = clusterElement("test")
            .title("Test")
            .description("A test element")
            .type(TOOLS)
            .properties(string("foo"));

        ClusterElementDefinition definition = new ClusterElementDefinition(sdkDefinition, "comp", 1, "icon");

        Property barProperty = Property.toProperty(string("bar"));
        Property bazProperty = Property.toProperty(string("baz"));

        ClusterElementDefinition result = definition.withPrependedProperties(List.of(barProperty, bazProperty));

        assertEquals(
            List.of("bar", "baz", "foo"),
            result.getProperties()
                .stream()
                .map(Property::getName)
                .toList());

        // original is unmodified
        assertEquals(
            List.of("foo"),
            definition.getProperties()
                .stream()
                .map(Property::getName)
                .toList());

        // other fields copied
        assertEquals("comp", result.getComponentName());
        assertEquals(1, result.getComponentVersion());
        assertEquals("test", result.getName());
        assertEquals(TOOLS, result.getType());
    }
}
