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

package com.bytechef.hermes.component.definition.factory;

import com.bytechef.data.storage.service.DataStorageService;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ActionContextImpl;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.ContextImpl;
import com.bytechef.hermes.component.definition.HttpClientExecutor;
import com.bytechef.hermes.component.definition.TriggerContext;
import com.bytechef.hermes.component.definition.TriggerContextImpl;
import com.bytechef.hermes.component.registry.domain.ComponentConnection;
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
        @NonNull String componentName, int componentVersion, @NonNull String actionName, @Nullable Integer type,
        @Nullable Long instanceId, @Nullable String workflowId, @Nullable Long jobId,
        @Nullable ComponentConnection connection) {

        return new ActionContextImpl(
            componentName, componentVersion, actionName, instanceId, type, workflowId, jobId,
            connection, dataStorageService, eventPublisher, fileStorageService, httpClientExecutor);
    }

    public Context createContext(@NonNull String componentName, @Nullable ComponentConnection connection) {
        return new ContextImpl(componentName, null, connection, httpClientExecutor);
    }

    public TriggerContext createTriggerContext(
        @NonNull String componentName, @NonNull String triggerName, @Nullable ComponentConnection connection) {

        return new TriggerContextImpl(
            componentName, triggerName, connection, httpClientExecutor);
    }
}
