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

import static com.bytechef.commons.util.MemoizationUtils.memoize;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.ComponentHandler;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentDefinitionRegistry;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.handler.DynamicComponentHandlerRegistry;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader;
import com.bytechef.platform.component.handler.loader.ComponentHandlerLoader.ComponentHandlerEntry;
import com.bytechef.platform.component.handler.loader.DefaultComponentHandlerLoader;
import com.bytechef.platform.component.jdbc.handler.loader.JdbcComponentHandlerLoader;
import com.bytechef.platform.component.oas.handler.loader.OpenApiComponentHandlerLoader;
import com.bytechef.platform.component.task.handler.ComponentTaskHandlerProvider;
import com.bytechef.platform.component.trigger.handler.ComponentTriggerHandlerProvider;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.core.env.Environment;

/**
 * @author Ivica Cardic
 */
class ComponentHandlerBeanRegistrar implements BeanRegistrar {

    private static final List<ComponentHandlerLoader> COMPONENT_HANDLER_LOADERS =
        List.of(
            new DefaultComponentHandlerLoader(), new JdbcComponentHandlerLoader(), new OpenApiComponentHandlerLoader());

    static final Supplier<List<ComponentHandlerEntry>> COMPONENT_HANDLER_ENTRIES_SUPPLIER = memoize(
        () -> CollectionUtils.flatMap(COMPONENT_HANDLER_LOADERS, ComponentHandlerLoader::loadComponentHandlers));

    @Override
    public void register(BeanRegistry registry, Environment env) {
        registry.registerBean("componentDefinitionRegistry", ComponentDefinitionRegistry.class, spec -> spec
            .lazyInit()
            .supplier(context -> new ComponentDefinitionRegistry(
                context.bean(ApplicationProperties.class),
                context.beanProvider(ComponentHandler.class)
                    .orderedStream()
                    .toList(),
                COMPONENT_HANDLER_ENTRIES_SUPPLIER,
                context.beanProvider(DynamicComponentHandlerRegistry.class)
                    .orderedStream()
                    .toList())));

        registry.registerBean("componentTaskHandlerProvider", ComponentTaskHandlerProvider.class, spec -> spec
            .lazyInit()
            .supplier(context -> new ComponentTaskHandlerProvider(
                COMPONENT_HANDLER_ENTRIES_SUPPLIER,
                context.bean("actionDefinitionFacade", ActionDefinitionFacade.class))));

        registry.registerBean("componentTriggerHandlerProvider", ComponentTriggerHandlerProvider.class, spec -> spec
            .lazyInit()
            .supplier(context -> new ComponentTriggerHandlerProvider(
                context.beanProvider(ComponentHandler.class)
                    .orderedStream()
                    .toList(),
                COMPONENT_HANDLER_ENTRIES_SUPPLIER,
                context.bean("triggerDefinitionFacade", TriggerDefinitionFacade.class))));
    }
}
