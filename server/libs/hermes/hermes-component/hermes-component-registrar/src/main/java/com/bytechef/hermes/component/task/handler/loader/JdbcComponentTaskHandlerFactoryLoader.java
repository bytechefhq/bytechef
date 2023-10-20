
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

package com.bytechef.hermes.component.task.handler.loader;

import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.JdbcComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.definition.JdbcComponentDefinition;
import com.bytechef.hermes.component.jdbc.DataSourceFactory;
import com.bytechef.hermes.component.jdbc.JdbcExecutor;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class JdbcComponentTaskHandlerFactoryLoader extends DefaultComponentTaskHandlerFactoryLoader {

    private final DataSourceFactory dataSourceFactory;

    @SuppressFBWarnings("EI2")
    JdbcComponentTaskHandlerFactoryLoader(
        ConnectionService connectionService, DataSourceFactory dataSourceFactory, EventPublisher eventPublisher,
        FileStorageService fileStorageService) {

        super(connectionService, eventPublisher, fileStorageService);

        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public List<ComponentTaskHandlerFactory> loadComponentTaskHandlerFactories() {
        List<ComponentTaskHandlerFactory> componentTaskHandlerFactories = new ArrayList<>();

        for (JdbcComponentDefinitionFactory jdbcComponentDefinitionFactory : ServiceLoader.load(
            JdbcComponentDefinitionFactory.class)) {

            JdbcComponentDefinition jdbcComponentDefinition = jdbcComponentDefinitionFactory
                .getJdbcComponentDefinition();

            JdbcExecutor jdbcExecutor = new JdbcExecutor(
                jdbcComponentDefinition.getDatabaseJdbcName(), dataSourceFactory,
                jdbcComponentDefinition.getJdbcDriverClassName());

            ComponentHandler componentHandler = new JdbcComponentHandler(jdbcExecutor, jdbcComponentDefinition);

            ComponentDefinition componentDefinition = componentHandler.getDefinition();

            componentTaskHandlerFactories.add(
                new ComponentTaskHandlerFactory(
                    componentDefinition,
                    componentDefinition.getActions()
                        .stream()
                        .map(actionDefinition -> new TaskHandlerFactoryItem(
                            actionDefinition.getName(),
                            createTaskHandlerFactory(
                                actionDefinition, componentDefinition.getConnection(), componentHandler)))
                        .toList()));
        }

        return componentTaskHandlerFactories;
    }
}
