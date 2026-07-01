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

package com.bytechef.platform.component.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.context.ContextFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ComponentDefinitionServiceExecuteWorkflowInputOptionsTest {

    @Mock
    private ComponentDefinitionRegistry componentDefinitionRegistry;

    @Mock
    private ContextFactory contextFactory;

    @Mock
    private ActionContext actionContext;

    private ComponentDefinitionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ComponentDefinitionServiceImpl(List.of(), componentDefinitionRegistry, contextFactory);
    }

    @Test
    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    void testExecuteWorkflowInputOptions() throws Exception {
        // Mock as Property (the return type of getComponentInputProperty) so Mockito accepts it,
        // then cast to DynamicOptionsProperty since the impl casts the returned property.
        Property propertyMock = Mockito.mock(Property.class, Mockito.withSettings()
            .extraInterfaces(DynamicOptionsProperty.class));
        DynamicOptionsProperty<?> property = (DynamicOptionsProperty<?>) propertyMock;
        OptionsDataSource<OptionsFunction> optionsDataSource = (OptionsDataSource<OptionsFunction>) Mockito
            .mock(OptionsDataSource.class);
        OptionsFunction optionsFunction =
            (in, conn, deps, search, ctx) -> List.of(ComponentDsl.option("General", "C1"));

        when(contextFactory.createActionContext(
            anyString(), anyInt(), anyString(), any(), any(), any(), any(), any(), any(), any(), any(),
            eq(true)))
                .thenReturn(actionContext);

        doReturn(propertyMock).when(componentDefinitionRegistry)
            .getComponentInputProperty(
                eq("slack"), eq(1), eq("channel"), eq("channel"), any(), any(), any(), any());
        when(property.getOptionsDataSource()).thenReturn((Optional) Optional.of(optionsDataSource));
        when(optionsDataSource.getOptions()).thenReturn(optionsFunction);

        List<com.bytechef.platform.component.domain.Option> options = service.executeWorkflowInputOptions(
            "slack", 1, "channel", "channel", Map.of(), List.of(), null, null);

        assertThat(options)
            .extracting(com.bytechef.platform.component.domain.Option::getValue)
            .containsExactly("C1");
    }
}
