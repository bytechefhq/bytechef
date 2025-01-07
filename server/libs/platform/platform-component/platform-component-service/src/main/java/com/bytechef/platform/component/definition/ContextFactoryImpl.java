/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.platform.component.domain.ComponentConnection;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.FilesFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class ContextFactoryImpl implements ContextFactory {

    private final ApplicationContext applicationContext;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final DataStorage dataStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final FilesFileStorage filesFileStorage;

    @SuppressFBWarnings("EI")
    public ContextFactoryImpl(
        ApplicationContext applicationContext, ConnectionDefinitionService connectionDefinitionService,
        DataStorage dataStorage, ApplicationEventPublisher eventPublisher, FilesFileStorage filesFileStorage) {

        this.applicationContext = applicationContext;
        this.connectionDefinitionService = connectionDefinitionService;
        this.dataStorage = dataStorage;
        this.eventPublisher = eventPublisher;
        this.filesFileStorage = filesFileStorage;
    }

    @Override
    public ActionContext createActionContext(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, ModeType type,
        Long instanceId, Long instanceWorkflowId, String workflowId, Long jobId, ComponentConnection connection,
        boolean devEnvironment) {

        return new ActionContextImpl(
            componentName, componentVersion, actionName, type, instanceId, instanceWorkflowId, workflowId, jobId,
            connection, devEnvironment, getDataStorage(workflowId, devEnvironment), eventPublisher,
            getFilesFileStorage(devEnvironment), getHttpClientExecutor(devEnvironment));
    }

    @Override
    public Context createContext(@NonNull String componentName, ComponentConnection connection) {
        return new ContextImpl(
            componentName, -1, null, filesFileStorage, connection, getHttpClientExecutor(false));
    }

    @Override
    public TriggerContext createTriggerContext(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, ModeType type,
        Long instanceId, String workflowReferenceCode, ComponentConnection connection, boolean devEnvironment) {

        return new TriggerContextImpl(
            componentName, componentVersion, triggerName, type, instanceId, workflowReferenceCode, connection,
            devEnvironment, getDataStorage(workflowReferenceCode, devEnvironment), getFilesFileStorage(devEnvironment),
            getHttpClientExecutor(devEnvironment));
    }

    private DataStorage getDataStorage(String workflowReference, boolean devEnvironment) {
        if (devEnvironment) {
            return new InMemoryDataStorage(workflowReference);
        }

        return dataStorage;
    }

    private FilesFileStorage getFilesFileStorage(boolean devEnvironment) {
        if (devEnvironment) {
            return new TempFilesFileStorage();
        }

        return filesFileStorage;
    }

    private HttpClientExecutor getHttpClientExecutor(boolean devEnvironment) {
        return new HttpClientExecutor(
            applicationContext, connectionDefinitionService, getFilesFileStorage(devEnvironment));
    }
}
