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
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import java.util.ServiceLoader;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ComponentTaskHandlerRegistrar extends AbstractComponentTaskHandlerRegistrar {

    public ComponentTaskHandlerRegistrar(
            ConnectionService connectionService, EventPublisher eventPublisher, FileStorageService fileStorageService) {
        super(connectionService, eventPublisher, fileStorageService);
    }

    @Override
    public void registerTaskHandlers(ConfigurableListableBeanFactory beanFactory) {
        for (ComponentHandler componentHandler : ServiceLoader.load(ComponentHandler.class)) {
            registerComponentActionTaskHandlerAdapter(componentHandler, beanFactory);
        }
    }
}
