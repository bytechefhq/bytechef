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
import com.bytechef.file.storage.FileStorageServiceRegistry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.datastream.ClusterElementResolverFunction;
import com.bytechef.platform.component.log.LogFileStorage;
import com.bytechef.platform.component.log.LogFileStorageWriter;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.storage.DataStorage;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
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
    private final DataStorage dataStorage;
    private final EditorLogFileStorageWriter editorLogFileStorageWriter;
    private final EditorTempFileStorage editorTempFileStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final LogFileStorage logFileStorage;
    private final TempFileStorage tempFileStorage;
    private final String publicUrl;

    @SuppressFBWarnings("EI")
    public ContextFactoryImpl(
        ApplicationContext applicationContext, ApplicationProperties applicationProperties, CacheManager cacheManager,
        DataStorage dataStorage, ApplicationEventPublisher eventPublisher,
        FileStorageServiceRegistry fileStorageServiceRegistry, LogFileStorage logFileStorage,
        TempFileStorage tempFileStorage) {

        this.applicationContext = applicationContext;
        this.cacheManager = cacheManager;
        this.dataStorage = dataStorage;

        FileStorageService fileStorageService = fileStorageServiceRegistry.getFileStorageService(
            applicationProperties.getFileStorage()
                .getProvider()
                .name());

        this.editorLogFileStorageWriter = new EditorLogFileStorageWriter(fileStorageService);
        this.editorTempFileStorage = new EditorTempFileStorage(fileStorageService);
        this.eventPublisher = eventPublisher;
        this.logFileStorage = logFileStorage;
        this.tempFileStorage = tempFileStorage;
        this.publicUrl = applicationProperties.getPublicUrl();
    }

    @Override
    public ActionContext createActionContext(
        String componentName, int componentVersion, String actionName, @Nullable Long jobPrincipalId,
        @Nullable Long jobPrincipalWorkflowId, @Nullable Long jobId, @Nullable Long taskExecutionId,
        @Nullable String workflowId, @Nullable ComponentConnection componentConnection,
        @Nullable Long environmentId, @Nullable PlatformType type, boolean editorEnvironment) {

        return ActionContextImpl
            .builder(
                componentName, componentVersion, actionName, editorEnvironment, cacheManager, dataStorage,
                eventPublisher, getHttpClientExecutor(), getTempFileStorage(editorEnvironment))
            .componentConnection(componentConnection)
            .environmentId(environmentId)
            .jobId(jobId)
            .jobPrincipalId(jobPrincipalId)
            .jobPrincipalWorkflowId(jobPrincipalWorkflowId)
            .logFileStorageWriter(getLogFileStorageWriter(editorEnvironment))
            .publicUrl(publicUrl)
            .taskExecutionId(taskExecutionId != null ? taskExecutionId : 0)
            .type(type)
            .workflowId(workflowId)
            .build();
    }

    @Override
    public Context createContext(String componentName, @Nullable ComponentConnection componentConnection) {
        return new ContextImpl(
            componentName, -1, null, componentConnection, false, getHttpClientExecutor(), tempFileStorage);
    }

    @Override
    public ClusterElementContext createClusterElementContext(
        String componentName, int componentVersion, String clusterElementName,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment) {

        return createClusterElementContext(
            componentName, componentVersion, clusterElementName, componentConnection, editorEnvironment, null);
    }

    @Override
    public ClusterElementContext createClusterElementContext(
        String componentName, int componentVersion, String clusterElementName,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment,
        @Nullable ClusterElementResolverFunction clusterElementResolver) {

        return ClusterElementContextImpl
            .builder(
                componentName, componentVersion, clusterElementName, editorEnvironment, cacheManager, dataStorage,
                eventPublisher, getHttpClientExecutor(), getTempFileStorage(editorEnvironment))
            .clusterElementResolver(clusterElementResolver)
            .componentConnection(componentConnection)
            .logFileStorageWriter(getLogFileStorageWriter(editorEnvironment))
            .publicUrl(publicUrl)
            .build();
    }

    @Override
    public TriggerContext createTriggerContext(
        String componentName, int componentVersion, String triggerName, @Nullable Long jobPrincipalId,
        @Nullable String workflowUuid, @Nullable ComponentConnection componentConnection, @Nullable Long environmentId,
        @Nullable PlatformType type, boolean editorEnvironment) {

        return TriggerContextImpl
            .builder(
                componentName, componentVersion, triggerName, editorEnvironment, cacheManager, dataStorage,
                getHttpClientExecutor(), getTempFileStorage(editorEnvironment))
            .componentConnection(componentConnection)
            .environmentId(environmentId)
            .jobPrincipalId(jobPrincipalId)
            .logFileStorageWriter(getLogFileStorageWriter(editorEnvironment))
            .type(type)
            .workflowUuid(workflowUuid)
            .build();
    }

    private TempFileStorage getTempFileStorage(boolean editorEnvironment) {
        if (editorEnvironment) {
            return editorTempFileStorage;
        }

        return tempFileStorage;
    }

    private HttpClientExecutor getHttpClientExecutor() {
        return new HttpClientExecutor(applicationContext, tempFileStorage);
    }

    private LogFileStorageWriter getLogFileStorageWriter(boolean editorEnvironment) {
        if (editorEnvironment) {
            return editorLogFileStorageWriter;
        }

        return logFileStorage;
    }
}
