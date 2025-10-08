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

package com.bytechef.platform.component.context;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.Nullable;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextFactoryImpl implements ContextFactory {

    private final ApplicationContext applicationContext;
    private final CacheManager cacheManager;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final DataStorage dataStorage;
    private final EditorTempFileStorage editorTempFileStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final TempFileStorage tempFileStorage;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    public ContextFactoryImpl(
        ApplicationContext applicationContext, ApplicationProperties applicationProperties, CacheManager cacheManager,
        ConnectionDefinitionService connectionDefinitionService, DataStorage dataStorage,
        ApplicationEventPublisher eventPublisher, TempFileStorage tempFileStorage) {

        this.applicationContext = applicationContext;
        this.cacheManager = cacheManager;
        this.connectionDefinitionService = connectionDefinitionService;
        this.dataStorage = dataStorage;
        this.editorTempFileStorage = new EditorTempFileStorage();
        this.eventPublisher = eventPublisher;
        this.tempFileStorage = tempFileStorage;
        this.publicUrl = applicationProperties.getPublicUrl();
    }

    @Override
    public ActionContext createActionContext(
        String componentName, int componentVersion, String actionName, @Nullable ModeType type,
        @Nullable Long jobPrincipalId, @Nullable Long jobPrincipalWorkflowId, @Nullable Long jobId,
        @Nullable String workflowId, @Nullable ComponentConnection connection, boolean editorEnvironment) {

        return new ActionContextImpl(
            actionName, componentName, componentVersion, connection, this,
            getDataStorage(workflowId, editorEnvironment), editorEnvironment, eventPublisher,
            getHttpClientExecutor(editorEnvironment), jobId, jobPrincipalId, jobPrincipalWorkflowId, type, publicUrl,
            getTempFileStorage(editorEnvironment),
            workflowId);
    }

    @Override
    public Context createContext(String componentName, @Nullable ComponentConnection componentConnection) {
        return new ContextImpl(
            componentName, -1, null, componentConnection, false, getHttpClientExecutor(false),
            getTempFileStorage(false));
    }

    @Override
    public ClusterElementContext createClusterElementContext(
        String componentName, int componentVersion, String clusterElementName,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        return new ClusterElementContextImpl(
            componentName, componentVersion, clusterElementName, componentConnection, editorEnvironment,
            getHttpClientExecutor(editorEnvironment), getTempFileStorage(editorEnvironment));
    }

    @Override
    public TriggerContext createTriggerContext(
        String componentName, int componentVersion, String triggerName, @Nullable ModeType type,
        @Nullable Long jobPrincipalId, @Nullable String workflowUuid, @Nullable ComponentConnection connection,
        boolean editorEnvironment) {

        return new TriggerContextImpl(
            componentName, componentVersion, connection, getDataStorage(workflowUuid, editorEnvironment),
            editorEnvironment, getTempFileStorage(editorEnvironment), getHttpClientExecutor(editorEnvironment),
            jobPrincipalId, triggerName, type, workflowUuid);
    }

    private DataStorage getDataStorage(String workflowReference, boolean editorEnvironment) {
        if (editorEnvironment) {
            return new InMemoryDataStorage(workflowReference, cacheManager);
        }

        return dataStorage;
    }

    private TempFileStorage getTempFileStorage(boolean editorEnvironment) {
        if (editorEnvironment) {
            return editorTempFileStorage;
        }

        return tempFileStorage;
    }

    private HttpClientExecutor getHttpClientExecutor(boolean editorEnvironment) {
        return new HttpClientExecutor(
            applicationContext, connectionDefinitionService, getTempFileStorage(editorEnvironment));
    }
}
