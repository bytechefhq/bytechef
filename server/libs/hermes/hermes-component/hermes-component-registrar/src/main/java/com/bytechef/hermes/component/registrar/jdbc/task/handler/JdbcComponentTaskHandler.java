
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

package com.bytechef.hermes.component.registrar.jdbc.task.handler;

import com.bytechef.atlas.event.EventPublisher;
import com.bytechef.hermes.component.registrar.jdbc.DataSourceFactory;
import com.bytechef.hermes.component.registrar.task.handler.DefaultComponentTaskHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.facade.ConnectionDefinitionFacade;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentTaskHandler extends DefaultComponentTaskHandler implements InitializingBean {

    private final DataSourceFactory dataSourceFactory;

    @SuppressFBWarnings("EI2")
    public JdbcComponentTaskHandler(
        ActionDefinition actionDefinition, ConnectionDefinitionFacade connectionDefinitionFacade,
        ConnectionService connectionService, DataSourceFactory dataSourceFactory, EventPublisher eventPublisher,
        FileStorageService fileStorageService, JdbcComponentHandler jdbcComponentHandler) {

        super(
            actionDefinition, connectionDefinitionFacade, jdbcComponentHandler, connectionService, eventPublisher,
            fileStorageService);

        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public void afterPropertiesSet() {
        ((JdbcComponentHandler) componentHandler).setDataSourceFactory(dataSourceFactory);
    }
}
