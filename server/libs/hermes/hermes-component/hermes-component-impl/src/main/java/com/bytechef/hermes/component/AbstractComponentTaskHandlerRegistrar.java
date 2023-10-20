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

package com.bytechef.hermes.component;

import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.atlas.worker.task.handler.TaskHandlerRegistrar;
import com.bytechef.hermes.component.definition.ComponentAction;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractComponentTaskHandlerRegistrar implements TaskHandlerRegistrar {

    private final ConnectionService connectionService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    protected AbstractComponentTaskHandlerRegistrar(
            ConnectionService connectionService, EventPublisher eventPublisher, FileStorageService fileStorageService) {
        this.connectionService = connectionService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    protected void registerComponentActionTaskHandlerAdapter(
            ComponentHandler componentHandler, ConfigurableListableBeanFactory beanFactory) {
        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        if (componentDefinition == null) {
            return;
        }

        for (ComponentAction action : componentDefinition.getActions()) {
            if (componentHandler instanceof GenericComponentHandler genericComponentHandler) {
                beanFactory.registerSingleton(
                        getBeanName(componentDefinition.getName(), componentDefinition.getVersion(), action.getName()),
                        new GenericComponentHandlerAdapterTaskHandler(
                                action,
                                connectionService,
                                genericComponentHandler,
                                eventPublisher,
                                fileStorageService));
            } else {
                beanFactory.registerSingleton(
                        getBeanName(componentDefinition.getName(), componentDefinition.getVersion(), action.getName()),
                        new ComponentActionAdapterTaskHandler(
                                action.getPerformFunction(), connectionService, eventPublisher, fileStorageService));
            }
        }

        beanFactory.registerSingleton(
                getBeanName(
                        componentDefinition.getName(),
                        componentDefinition.getVersion(),
                        ComponentDefinitionFactory.class.getSimpleName()),
                componentHandler);
    }

    private String getBeanName(String componentName, int version, String typeName) {
        return componentName + "/v" + version + "/" + typeName;
    }
}
