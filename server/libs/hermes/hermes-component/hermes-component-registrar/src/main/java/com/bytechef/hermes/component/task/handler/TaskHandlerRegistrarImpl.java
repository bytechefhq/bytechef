
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

package com.bytechef.hermes.component.task.handler;

import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistrar;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.task.handler.loader.ComponentTaskHandlerFactoryLoader;
import com.bytechef.hermes.component.task.handler.loader.ComponentTaskHandlerFactoryLoader.ComponentTaskHandlerFactory;
import com.bytechef.hermes.component.task.handler.loader.ComponentTaskHandlerFactoryLoader.TaskHandlerFactory;
import com.bytechef.hermes.component.task.handler.loader.ComponentTaskHandlerFactoryLoader.TaskHandlerFactoryItem;
import com.bytechef.hermes.definition.registry.facade.ConnectionDefinitionFacade;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Ivica Cardic
 */
@Component
class TaskHandlerRegistrarImpl implements TaskHandlerRegistrar {

    private final ApplicationContext applicationContext;
    private final List<ComponentTaskHandlerFactoryLoader> componentTaskHandlerFactoryLoaders;

    public TaskHandlerRegistrarImpl(ApplicationContext applicationContext,
        List<ComponentTaskHandlerFactoryLoader> componentTaskHandlerFactoryLoaders) {
        this.applicationContext = applicationContext;
        this.componentTaskHandlerFactoryLoaders = componentTaskHandlerFactoryLoaders;
    }

    @Override
    public void registerTaskHandlers(ConfigurableListableBeanFactory beanFactory) {
        List<ComponentTaskHandlerFactory> componentTaskHandlerFactories = componentTaskHandlerFactoryLoaders
            .stream()
            .flatMap(componentTaskHandlerFactoryLoader -> componentTaskHandlerFactoryLoader
                .loadComponentTaskHandlerFactories()
                .stream())
            .toList();

        for (ComponentTaskHandlerFactory componentTaskHandlerFactory : componentTaskHandlerFactories) {
            ComponentDefinition componentDefinition = componentTaskHandlerFactory.componentDefinition();

            beanFactory.registerSingleton(
                getBeanName(
                    componentDefinition.getName(), componentDefinition.getVersion(),
                    ComponentDefinition.class.getSimpleName()),
                componentDefinition);
        }

        // If ConnectionDefinitionService is not fetched via applicationContext but directly injected via constructor,
        // the list of componentDefinitions in the ConnectionDefinitionServiceImpl is equal 0

        ConnectionDefinitionFacade connectionDefinitionFacade = applicationContext.getBean(
            ConnectionDefinitionFacade.class);

        for (ComponentTaskHandlerFactory componentTaskHandlerFactory : componentTaskHandlerFactories) {
            ComponentDefinition componentDefinition = componentTaskHandlerFactory.componentDefinition();

            for (TaskHandlerFactoryItem taskHandlerFactoryItem : componentTaskHandlerFactory
                .taskHandlerFactoryItems()) {

                TaskHandlerFactory taskHandlerFactory = taskHandlerFactoryItem.taskHandlerFactory();

                beanFactory.registerSingleton(
                    getBeanName(
                        componentDefinition.getName(), componentDefinition.getVersion(),
                        taskHandlerFactoryItem.actionDefinitionName()),
                    taskHandlerFactory.createTaskHandler(connectionDefinitionFacade));
            }
        }
    }

    private String getBeanName(String componentName, int version, String typeName) {
        return componentName + "/v" + version + "/" + typeName;
    }
}
