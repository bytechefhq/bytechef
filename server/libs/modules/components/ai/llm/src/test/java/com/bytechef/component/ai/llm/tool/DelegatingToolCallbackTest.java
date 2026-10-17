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

package com.bytechef.component.ai.llm.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class DelegatingToolCallbackTest {

    @Test
    void testUnwrapWalksNestedDelegates() {
        ToolCallback inner = mock(ToolCallback.class);
        DelegatingToolCallback middle = delegating(inner);
        DelegatingToolCallback outer = delegating(middle);

        assertThat(DelegatingToolCallback.unwrap(outer)).isSameAs(inner);
    }

    @Test
    void testUnwrapReturnsPlainCallbackUnchanged() {
        ToolCallback plain = mock(ToolCallback.class);

        assertThat(DelegatingToolCallback.unwrap(plain)).isSameAs(plain);
    }

    private DelegatingToolCallback delegating(ToolCallback delegate) {
        return new DelegatingToolCallback() {

            @Override
            public ToolCallback getDelegate() {
                return delegate;
            }

            @Override
            public ToolDefinition getToolDefinition() {
                return delegate.getToolDefinition();
            }

            @Override
            public String call(String toolInput) {
                return delegate.call(toolInput);
            }
        };
    }
}
