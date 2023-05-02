
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

package com.bytechef.hermes.component.registrar.jdbc.handler;

import com.bytechef.event.EventPublisher;
import com.bytechef.hermes.component.registrar.jdbc.sql.DataSourceFactory;
import com.bytechef.hermes.component.registrar.handler.DefaultComponentActionTaskHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.connection.InstanceConnectionFetcherAccessor;
import com.bytechef.hermes.connection.service.ConnectionService;
import com.bytechef.hermes.definition.registry.service.ConnectionDefinitionService;
import com.bytechef.hermes.file.storage.service.FileStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Ivica Cardic
 */
public class JdbcComponentActionTaskHandler extends DefaultComponentActionTaskHandler implements InitializingBean {

    private final DataSourceFactory dataSourceFactory;

    @SuppressFBWarnings("EI2")
    public JdbcComponentActionTaskHandler(
        ActionDefinition actionDefinition, ConnectionDefinitionService connectionDefinitionService,
        ConnectionService connectionService, DataSourceFactory dataSourceFactory, EventPublisher eventPublisher,
        FileStorageService fileStorageService, InstanceConnectionFetcherAccessor instanceConnectionFetcherAccessor,
        JdbcComponentHandler jdbcComponentHandler) {

        super(
            actionDefinition, jdbcComponentHandler, connectionDefinitionService, connectionService, eventPublisher,
            fileStorageService, instanceConnectionFetcherAccessor);

        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public void afterPropertiesSet() {
        ((JdbcComponentHandler) componentHandler).setDataSourceFactory(dataSourceFactory);
    }
}
