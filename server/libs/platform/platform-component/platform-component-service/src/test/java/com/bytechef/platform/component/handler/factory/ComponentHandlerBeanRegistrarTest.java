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

package com.bytechef.platform.component.handler.factory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.component.task.handler.ComponentTaskHandlerProvider;
import com.bytechef.platform.component.trigger.handler.ComponentTriggerHandlerProvider;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ComponentHandlerBeanRegistrarTest {

    @Mock
    private BeanRegistry registry;

    @Mock
    private Environment environment;

    private final ComponentHandlerBeanRegistrar registrar = new ComponentHandlerBeanRegistrar();

    @Test
    void testRegister() {
        registrar.register(registry, environment);

        verify(registry).registerBean(
            eq("componentDefinitionRegistry"), eq(ComponentDefinitionRegistry.class), notNull());
        verify(registry).registerBean(
            eq("componentTaskHandlerProvider"), eq(ComponentTaskHandlerProvider.class), notNull());
        verify(registry).registerBean(
            eq("componentTriggerHandlerProvider"), eq(ComponentTriggerHandlerProvider.class), notNull());

        verifyNoMoreInteractions(registry);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRegisterComponentDefinitionRegistrySpec() {
        ArgumentCaptor<Consumer<BeanRegistry.Spec<ComponentDefinitionRegistry>>> captor =
            ArgumentCaptor.forClass(Consumer.class);

        registrar.register(registry, environment);

        verify(registry).registerBean(
            eq("componentDefinitionRegistry"), eq(ComponentDefinitionRegistry.class), captor.capture());

        BeanRegistry.Spec<ComponentDefinitionRegistry> spec = mock();

        when(spec.lazyInit()).thenReturn(spec);
        when(spec.supplier(any())).thenReturn(spec);

        captor.getValue()
            .accept(spec);

        verify(spec).lazyInit();
        verify(spec).supplier(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRegisterComponentTaskHandlerProviderSpec() {
        ArgumentCaptor<Consumer<BeanRegistry.Spec<ComponentTaskHandlerProvider>>> captor =
            ArgumentCaptor.forClass(Consumer.class);

        registrar.register(registry, environment);

        verify(registry).registerBean(
            eq("componentTaskHandlerProvider"), eq(ComponentTaskHandlerProvider.class), captor.capture());

        BeanRegistry.Spec<ComponentTaskHandlerProvider> spec = mock();

        when(spec.lazyInit()).thenReturn(spec);
        when(spec.supplier(any())).thenReturn(spec);

        captor.getValue()
            .accept(spec);

        verify(spec).lazyInit();
        verify(spec).supplier(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRegisterComponentTriggerHandlerProviderSpec() {
        ArgumentCaptor<Consumer<BeanRegistry.Spec<ComponentTriggerHandlerProvider>>> captor =
            ArgumentCaptor.forClass(Consumer.class);

        registrar.register(registry, environment);

        verify(registry).registerBean(
            eq("componentTriggerHandlerProvider"), eq(ComponentTriggerHandlerProvider.class), captor.capture());

        BeanRegistry.Spec<ComponentTriggerHandlerProvider> spec = mock();

        when(spec.lazyInit()).thenReturn(spec);
        when(spec.supplier(any())).thenReturn(spec);

        captor.getValue()
            .accept(spec);

        verify(spec).lazyInit();
        verify(spec).supplier(any());
    }

    @Test
    void testComponentHandlerEntriesSupplierMemoization() {
        List<ComponentHandlerEntry> firstResult =
            ComponentHandlerBeanRegistrar.COMPONENT_HANDLER_ENTRIES_SUPPLIER.get();
        List<ComponentHandlerEntry> secondResult =
            ComponentHandlerBeanRegistrar.COMPONENT_HANDLER_ENTRIES_SUPPLIER.get();

        assertNotNull(firstResult);
        assertSame(firstResult, secondResult);
    }
}
