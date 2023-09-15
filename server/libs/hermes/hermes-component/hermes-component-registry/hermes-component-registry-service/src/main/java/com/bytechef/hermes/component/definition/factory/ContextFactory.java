
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

package com.bytechef.hermes.component.definition.factory;

import com.bytechef.data.storage.service.DataStorageService;
import com.bytechef.event.EventPublisher;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.ContextImpl;
import com.bytechef.hermes.component.definition.HttpClientExecutor;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.connection.domain.Connection;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextFactory {

    private final DataStorageService dataStorageService;
    private final EventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final HttpClientExecutor httpClientExecutor;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    @SuppressFBWarnings("EI")
    public ContextFactory(
        DataStorageService dataStorageService, EventPublisher eventPublisher, FileStorageService fileStorageService,
        HttpClientExecutor httpClientExecutor, ObjectMapper objectMapper, XmlMapper xmlMapper) {

        this.dataStorageService = dataStorageService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.httpClientExecutor = httpClientExecutor;
        this.objectMapper = objectMapper;
        this.xmlMapper = xmlMapper;
    }

    public ActionContext createActionContext(Connection connection, Long taskExecutionId) {
        return createContextImpl(connection, taskExecutionId);
    }

    public ActionContext createActionContext(Connection connection) {
        return createContextImpl(connection, null);
    }

    public Context createContext(Connection connection) {
        return createContextImpl(connection, null);
    }

    public TriggerContext createTriggerContext(Connection connection) {
        return createContextImpl(connection, null);
    }

    private ContextImpl createContextImpl(Connection connection, Long taskExecutionId) {
        return new ContextImpl(
            connection, dataStorageService, eventPublisher, objectMapper, fileStorageService,
            httpClientExecutor, taskExecutionId, xmlMapper);
    }
}
