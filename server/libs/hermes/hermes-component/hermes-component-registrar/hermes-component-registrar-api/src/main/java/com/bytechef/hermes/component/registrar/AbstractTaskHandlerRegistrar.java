/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.registrar;

import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistrar;
import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import java.util.ServiceLoader;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractTaskHandlerRegistrar<T extends ComponentDefinitionFactory>
        implements TaskHandlerRegistrar {

    private final Class<T> componentFactoryClass;

    protected AbstractTaskHandlerRegistrar(Class<T> componentFactoryClass) {
        this.componentFactoryClass = componentFactoryClass;
    }

    @Override
    public void registerTaskHandlers(ConfigurableListableBeanFactory beanFactory) {
        for (T componentFactory : ServiceLoader.load(componentFactoryClass)) {
            registerComponentActionTaskHandlerAdapter(componentFactory, beanFactory);
        }
    }

    protected void registerComponentActionTaskHandlerAdapter(
            T componentFactory, ConfigurableListableBeanFactory beanFactory) {
        ComponentDefinition componentDefinition = componentFactory.getDefinition();

        if (componentDefinition == null) {
            return;
        }

        for (ActionDefinition actionDefinition : componentDefinition.getActionDefinitions()) {
            beanFactory.registerSingleton(
                    getBeanName(
                            componentDefinition.getName(),
                            componentDefinition.getVersion(),
                            actionDefinition.getName()),
                    createTaskHandler(
                            actionDefinition, componentDefinition.getConnectionDefinition(), componentFactory));
        }

        beanFactory.registerSingleton(
                getBeanName(
                        componentDefinition.getName(),
                        componentDefinition.getVersion(),
                        ComponentDefinitionFactory.class.getSimpleName()),
                componentFactory);
    }

    protected abstract TaskHandler<?> createTaskHandler(
            ActionDefinition actionDefinition, ConnectionDefinition connectionDefinition, T componentFactory);

    private String getBeanName(String componentName, int version, String typeName) {
        return componentName + "/v" + version + "/" + typeName;
    }
}
