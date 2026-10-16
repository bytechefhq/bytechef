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

package com.bytechef.platform.component.runner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.domain.ComponentDefinition;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * @author Ivica Cardic
 */
class PolyglotComponentActionInvokerTest {

    private final PolyglotComponentCatalog componentCatalog = mock(PolyglotComponentCatalog.class);

    @Test
    void testInvokeNamedConnectionWithoutComponentConnections() {
        PolyglotComponentActionInvoker componentActionInvoker = new PolyglotComponentActionInvoker(
            mock(ApplicationContext.class), Map.of(), mock(JobContextAware.class), componentCatalog);

        when(componentCatalog.getComponentDefinition("httpClient")).thenReturn(mock(ComponentDefinition.class));

        assertThatThrownBy(() -> componentActionInvoker.invoke("httpClient", "get", Map.of(), "missing", Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Connection with name missing does not exist");
    }
}
