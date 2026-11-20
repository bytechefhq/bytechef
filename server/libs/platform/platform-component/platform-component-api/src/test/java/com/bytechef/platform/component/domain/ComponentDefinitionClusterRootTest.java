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

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ComponentDefinitionClusterRootTest {

    private static final ClusterElementType READER = new ClusterElementType("READER", "reader", "Reader");

    @Test
    void testAnActionWithClusterElementTypesIsAClusterRoot() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            new TestClusterRootComponentDefinition(Map.of("load", List.of(READER.name()), "search", List.of())));

        assertThat(componentDefinition.isClusterRoot("load")).isTrue();
    }

    @Test
    void testAnActionWithoutClusterElementTypesIsNotAClusterRoot() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            new TestClusterRootComponentDefinition(Map.of("load", List.of(READER.name()), "search", List.of())));

        assertThat(componentDefinition.isClusterRoot()).isTrue();
        assertThat(componentDefinition.isClusterRoot("search")).isFalse();
    }

    @Test
    void testAnActionMissingFromTheMapFallsBackToTheComponent() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            new TestClusterRootComponentDefinition(Map.of()));

        assertThat(componentDefinition.isClusterRoot("load")).isTrue();
        assertThat(componentDefinition.isClusterRoot(null)).isTrue();
    }

    @Test
    void testAComponentThatIsNotAClusterRootIsNeverOne() {
        ComponentDefinition componentDefinition = new ComponentDefinition(
            component("plain").actions(action("search")));

        assertThat(componentDefinition.isClusterRoot("search")).isFalse();
    }

    private static class TestClusterRootComponentDefinition extends AbstractComponentDefinitionWrapper
        implements ClusterRootComponentDefinition {

        private final Map<String, List<String>> actionClusterElementTypes;

        TestClusterRootComponentDefinition(Map<String, List<String>> actionClusterElementTypes) {
            super(component("root").actions(action("load"), action("search")));

            this.actionClusterElementTypes = Map.copyOf(actionClusterElementTypes);
        }

        @Override
        public List<ClusterElementType> getClusterElementTypes() {
            return List.of(READER);
        }

        @Override
        public Map<String, List<String>> getActionClusterElementTypes() {
            return Map.copyOf(actionClusterElementTypes);
        }
    }
}
