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

package com.bytechef.platform.component.registry.definition.factory;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.registry.definition.ActionContextImpl;
import com.bytechef.platform.component.registry.definition.ContextImpl;
import com.bytechef.platform.component.registry.definition.HttpClientExecutor;
import com.bytechef.platform.component.registry.definition.TriggerContextImpl;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.data.storage.service.DataStorageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class ContextFactory {

    private final DataStorageService dataStorageService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;
    private final HttpClientExecutor httpClientExecutor;

    @SuppressFBWarnings("EI")
    public ContextFactory(
        DataStorageService dataStorageService, ApplicationEventPublisher eventPublisher,
        FileStorageService fileStorageService, HttpClientExecutor httpClientExecutor) {

        this.dataStorageService = dataStorageService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
        this.httpClientExecutor = httpClientExecutor;
    }

    public ActionContext createActionContext(
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @Nullable Type type,
        @Nullable String projectWorkflowId, @Nullable Long jobId, @Nullable ComponentConnection connection) {

        return new ActionContextImpl(
            componentName, componentVersion, actionName, type, projectWorkflowId, jobId,
            connection, dataStorageService, eventPublisher, fileStorageService, httpClientExecutor);
    }

    public Context createContext(@NonNull String componentName, @Nullable ComponentConnection connection) {
        return new ContextImpl(componentName, null, connection, httpClientExecutor);
    }

    public TriggerContext createTriggerContext(
        @NonNull String componentName, int componentVersion, @NonNull String triggerName, @Nullable Type type,
        @Nullable String workflowId, @Nullable Long jobId, @Nullable ComponentConnection connection) {

        return new TriggerContextImpl(
            componentName, componentVersion, triggerName, type, workflowId, jobId,
            connection, dataStorageService, eventPublisher, fileStorageService, httpClientExecutor);
    }
}
