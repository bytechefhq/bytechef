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

package com.bytechef.platform.component.registry.definition;

import com.bytechef.atlas.coordinator.event.TaskProgressedApplicationEvent;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.data.storage.service.DataStorageService;
import com.bytechef.platform.workflow.execution.constants.FileEntryConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class ActionContextImpl extends ContextImpl implements ActionContext {

    private final Data data;
    private final Event event;
    private final File file;

    @SuppressFBWarnings("EI")
    public ActionContextImpl(
        String componentName, int componentVersion, String actionName, AppType type,
        Long instanceWorkflowId, Long jobId, ComponentConnection connection, DataStorageService dataStorageService,
        ApplicationEventPublisher eventPublisher, FileStorageService fileStorageService,
        HttpClientExecutor httpClientExecutor) {

        super(componentName, actionName, connection, httpClientExecutor);

        if (type == null || instanceWorkflowId == null || jobId == null) {
            this.data = new NoOpDataImpl();
        } else {
            this.data = new DataImpl(
                componentName, componentVersion, actionName, type, instanceWorkflowId, jobId, dataStorageService);
        }

        this.event = jobId == null ? progress -> {} : new EventImpl(eventPublisher, jobId);
        this.file = new FileImpl(fileStorageService);
    }

    @Override
    public <R> R data(ContextFunction<Data, R> dataFunction) {
        try {
            return dataFunction.apply(data);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void event(Consumer<Event> eventConsumer) {
        eventConsumer.accept(event);
    }

    @Override
    public <R> R file(ContextFunction<File, R> fileFunction) {
        try {
            return fileFunction.apply(file);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private record DataImpl(
        String componentName, Integer componentVersion, String actionName, AppType type, long instanceWorkflowId,
        long jobId, DataStorageService dataStorageService) implements Data {

        @Override
        public <T> Optional<T> fetchValue(Scope scope, String key) {
            return dataStorageService.fetch(componentName, scope, getScopeId(scope), key, type);
        }

        @Override
        public <T> T getValue(Scope scope, String key) {
            return dataStorageService.get(componentName, scope, getScopeId(scope), key, type);
        }

        @Override
        public <T> Map<String, T> getAll(Scope scope) {
            return dataStorageService.getAll(componentName, scope, getScopeId(scope), type);
        }

        @Override
        public Void setValue(Scope scope, String key, Object value) {
            dataStorageService.put(componentName, scope, getScopeId(scope), key, type, value);

            return null;
        }

        @Override
        public Void deleteValue(Scope scope, String key) {
            dataStorageService.delete(componentName, scope, getScopeId(scope), key, type);

            return null;
        }

        private String getScopeId(Scope scope) {
            return Validate.notNull(
                switch (scope) {
                    case CURRENT_EXECUTION -> String.valueOf(jobId);
                    case WORKFLOW -> String.valueOf(instanceWorkflowId);
                    case ACCOUNT -> "";
                }, "scope");
        }
    }

    private record EventImpl(ApplicationEventPublisher eventPublisher, long taskExecutionId) implements Event {

        @Override
        public void publishActionProgressEvent(int progress) {
            eventPublisher.publishEvent(new TaskProgressedApplicationEvent(taskExecutionId, progress));
        }
    }

    private record FileImpl(FileStorageService fileStorageService) implements File {

        @Override
        public InputStream getStream(FileEntry fileEntry) {
            return fileStorageService.getFileStream(
                FileEntryConstants.FILES_DIR, ((FileEntryImpl) fileEntry).getFileEntry());
        }

        @Override
        public String readToString(FileEntry fileEntry) {
            return fileStorageService.readFileToString(
                FileEntryConstants.FILES_DIR, ((FileEntryImpl) fileEntry).getFileEntry());
        }

        @Override
        public FileEntry storeContent(String fileName, String data) {
            return new FileEntryImpl(
                fileStorageService.storeFileContent(FileEntryConstants.FILES_DIR, fileName, data));
        }

        @Override
        public java.io.File toTempFile(FileEntry fileEntry) {
            Path path = toTempFilePath(fileEntry);

            return path.toFile();
        }

        @Override
        public Path toTempFilePath(FileEntry fileEntry) {
            Path tempFilePath;

            try {
                tempFilePath = Files.createTempFile("action_context_", fileEntry.getName());

                Files.copy(
                    fileStorageService.getFileStream(FileEntryConstants.FILES_DIR, toFileEntry(fileEntry)),
                    tempFilePath,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return tempFilePath;
        }

        @Override
        public byte[] readAllBytes(FileEntry fileEntry) throws IOException {
            InputStream inputStream = getStream(fileEntry);

            return inputStream.readAllBytes();
        }

        @Override
        public FileEntry storeContent(String fileName, InputStream inputStream) {
            try {
                return new FileEntryImpl(
                    fileStorageService.storeFileContent(FileEntryConstants.FILES_DIR, fileName, inputStream));
            } catch (Exception exception) {
                throw new RuntimeException("Unable to store file " + fileName);
            }
        }

        private static com.bytechef.file.storage.domain.FileEntry toFileEntry(FileEntry fileEntry) {
            return new com.bytechef.file.storage.domain.FileEntry(
                fileEntry.getName(), fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getUrl());
        }
    }

    private record NoOpDataImpl() implements Data {

        @Override
        public <T> Optional<T> fetchValue(Scope scope, String key) {
            return Optional.empty();
        }

        @Override
        public <T> T getValue(Scope scope, String key) {
            return null;
        }

        @Override
        public <T> Map<String, T> getAll(Scope scope) {
            return null;
        }

        @Override
        public Void setValue(Scope scope, String key, Object data) {
            return null;
        }

        @Override
        public Void deleteValue(Scope scope, String key) {
            return null;
        }
    }
}
