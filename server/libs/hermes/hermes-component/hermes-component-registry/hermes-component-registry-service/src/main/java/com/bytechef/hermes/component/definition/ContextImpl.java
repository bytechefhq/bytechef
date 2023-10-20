
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

package com.bytechef.hermes.component.definition;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.data.storage.service.DataStorageService;
import com.bytechef.event.EventPublisher;
import com.bytechef.atlas.execution.event.TaskProgressedEvent;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.hermes.component.definition.ActionDefinition.ActionContext;
import com.bytechef.hermes.component.definition.TriggerDefinition.TriggerContext;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.registry.dto.ComponentConnection;
import com.bytechef.hermes.execution.constants.FileEntryConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements ActionContext, TriggerContext {

    private final Data data;
    private final Event event;
    private final File file;
    private final Http http;
    private final Json json;
    private final Xml xml;

    @SuppressFBWarnings("EI")
    public ContextImpl(
        String componentName, ComponentConnection connection, DataStorageService dataStorageService,
        EventPublisher eventPublisher, ObjectMapper objectMapper, FileStorageService fileStorageService,
        HttpClientExecutor httpClientExecutor, Long taskExecutionId, XmlMapper xmlMapper) {

        this.data = new DataImpl(dataStorageService);
        this.event = taskExecutionId == null ? null : new EventImpl(eventPublisher, taskExecutionId);
        this.file = new FileImpl(fileStorageService);
        this.http = new HttpImpl(componentName, connection, this, httpClientExecutor);
        this.json = new JsonImpl(objectMapper);
        this.xml = new XmlImpl(xmlMapper);
    }

    @Override
    public <R> R data(ContextFunction<Data, R> dataFunction) {
        try {
            return dataFunction.apply(data);
        } catch (Exception e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R file(ContextFunction<File, R> fileFunction) {
        try {
            return fileFunction.apply(file);
        } catch (Exception e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R http(ContextFunction<Http, R> httpFunction) {
        try {
            return httpFunction.apply(http);
        } catch (Exception e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R json(ContextFunction<Json, R> jsonFunction) {
        try {
            return jsonFunction.apply(json);
        } catch (Exception e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
        try {
            return xmlFunction.apply(xml);
        } catch (Exception e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public void event(Consumer<Event> eventConsumer) {
        eventConsumer.accept(event);
    }

    private static class DataImpl implements Data {

        private final DataStorageService dataStorageService;

        private DataImpl(DataStorageService dataStorageService) {
            this.dataStorageService = dataStorageService;
        }

        @Override
        public <T> Optional<T> fetchValue(String context, int scope, long scopeId, String key) {
            return dataStorageService.fetch(context, scope, scopeId, key);
        }

        @Override
        public <T> T getValue(String context, int scope, long scopeId, String key) {
            return dataStorageService.get(context, scope, scopeId, key);
        }

        @Override
        public void setValue(String context, int scope, long scopeId, String key, Object value) {
            dataStorageService.put(context, scope, scopeId, key, value);
        }
    }

    private static class EventImpl implements Event {

        private final EventPublisher eventPublisher;
        private final long taskExecutionId;

        public EventImpl(EventPublisher eventPublisher, long taskExecutionId) {
            this.eventPublisher = eventPublisher;
            this.taskExecutionId = taskExecutionId;
        }

        @Override
        public void publishActionProgressEvent(int progress) {
            eventPublisher.publishEvent(new TaskProgressedEvent(taskExecutionId, progress));
        }
    }

    private static class FileImpl implements File {

        private final FileStorageService fileStorageService;

        public FileImpl(FileStorageService fileStorageService) {
            this.fileStorageService = fileStorageService;
        }

        @Override
        public InputStream getStream(Context.FileEntry fileEntry) {
            return fileStorageService.getFileStream(
                FileEntryConstants.DOCUMENTS_DIR, ((ContextFileEntryImpl) fileEntry).getFileEntry());
        }

        @Override
        public String readToString(Context.FileEntry fileEntry) {
            return fileStorageService.readFileToString(
                FileEntryConstants.DOCUMENTS_DIR, ((ContextFileEntryImpl) fileEntry).getFileEntry());
        }

        @Override
        public FileEntry storeContent(String fileName, String data) {
            return new ContextFileEntryImpl(
                fileStorageService.storeFileContent(FileEntryConstants.DOCUMENTS_DIR, fileName, data));
        }

        @Override
        public FileEntry storeContent(String fileName, InputStream inputStream) {
            try {
                return new ContextFileEntryImpl(
                    fileStorageService.storeFileContent(FileEntryConstants.DOCUMENTS_DIR, fileName, inputStream));
            } catch (Exception exception) {
                throw new ComponentExecutionException("Unable to store file " + fileName, exception);
            }
        }
    }

    private static class HttpImpl implements Http {

        private final String componentName;
        private final ComponentConnection connection;
        private final Context context;
        private final HttpClientExecutor httpClientExecutor;

        private HttpImpl(
            String componentName, ComponentConnection connection, Context context,
            HttpClientExecutor httpClientExecutor) {

            this.componentName = componentName;
            this.connection = connection;
            this.context = context;
            this.httpClientExecutor = httpClientExecutor;
        }

        @Override
        public Executor delete(String url) {
            return new ExecutorImpl(url, RequestMethod.DELETE, componentName, connection, context, httpClientExecutor);
        }

        @Override
        public Executor exchange(String url, RequestMethod requestMethod) {
            return new ExecutorImpl(url, requestMethod, componentName, connection, context, httpClientExecutor);
        }

        @Override
        public Executor head(String url) {
            return new ExecutorImpl(url, RequestMethod.HEAD, componentName, connection, context, httpClientExecutor);
        }

        @Override
        public Executor get(String url) {
            return new ExecutorImpl(url, RequestMethod.GET, componentName, connection, context, httpClientExecutor);
        }

        @Override
        public Executor patch(String url) {
            return new ExecutorImpl(url, RequestMethod.PATCH, componentName, connection, context, httpClientExecutor);
        }

        @Override
        public Executor post(String url) {
            return new ExecutorImpl(url, RequestMethod.POST, componentName, connection, context, httpClientExecutor);
        }

        @Override
        public Executor put(String url) {
            return new ExecutorImpl(url, RequestMethod.PUT, componentName, connection, context, httpClientExecutor);
        }

        private static class ExecutorImpl implements Executor {

            private Http.Body body;
            private final String componentName;
            private Configuration configuration = new Configuration();
            private final ComponentConnection connection;
            private final Context context;
            private final HttpClientExecutor httpClientExecutor;
            private Map<String, List<String>> headers = new HashMap<>();
            private Map<String, List<String>> queryParameters = new HashMap<>();
            private final RequestMethod requestMethod;
            private final String url;

            private ExecutorImpl(
                String url, RequestMethod requestMethod, String componentName, ComponentConnection connection,
                Context context, HttpClientExecutor httpClientExecutor) {

                this.componentName = componentName;
                this.connection = connection;
                this.context = context;
                this.httpClientExecutor = httpClientExecutor;
                this.url = url;
                this.requestMethod = requestMethod;
            }

            @Override
            public Executor configuration(Configuration.ConfigurationBuilder configurationBuilder) {
                this.configuration = Objects.requireNonNull(configurationBuilder)
                    .build();

                return this;
            }

            @Override
            public Executor header(String name, String value) {
                headers.put(Objects.requireNonNull(name), List.of(Objects.requireNonNull(value)));

                return this;
            }

            @Override
            public Executor headers(Map<String, List<String>> headers) {
                this.headers = new HashMap<>(Objects.requireNonNull(headers));

                return this;
            }

            @Override
            public Executor queryParameter(String name, String value) {
                queryParameters.put(Objects.requireNonNull(name), List.of(Objects.requireNonNull(value)));

                return this;
            }

            @Override
            public Executor queryParameters(Map<String, List<String>> queryParameters) {
                this.queryParameters = new HashMap<>(Objects.requireNonNull(queryParameters));

                return this;
            }

            @Override
            public Executor body(Http.Body body) {
                this.body = body;

                return this;
            }

            @Override
            public Http.Response execute() throws ComponentExecutionException {
                try {
                    return httpClientExecutor.execute(
                        url, headers, queryParameters, body, configuration, requestMethod, componentName, connection,
                        context);
                } catch (Exception e) {
                    throw new ComponentExecutionException("Unable to execute HTTP request", e);
                }
            }
        }
    }

    private record JsonImpl(ObjectMapper objectMapper) implements Json {

        @Override
        public Object read(InputStream inputStream) {
            return JsonUtils.read(inputStream, objectMapper);
        }

        @Override
        public <T> T read(InputStream inputStream, Class<T> valueType) {
            return JsonUtils.read(inputStream, valueType, objectMapper);
        }

        @Override
        public <T> T read(InputStream inputStream, TypeReference<T> typeReference) {
            return JsonUtils.read(inputStream, typeReference.getType(), objectMapper);
        }

        @Override
        public Object read(InputStream inputStream, String path) {
            return JsonUtils.read(inputStream, path, objectMapper);
        }

        @Override
        public <T> T read(InputStream inputStream, String path, Class<T> valueType) {
            return JsonUtils.read(inputStream, path, valueType, objectMapper);
        }

        @Override
        public <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference) {
            return JsonUtils.read(inputStream, path, typeReference.getType(), objectMapper);
        }

        @Override
        public Object read(String json) {
            return JsonUtils.read(json, objectMapper);
        }

        @Override
        public <T> T read(String json, Class<T> valueType) {
            return JsonUtils.read(json, valueType, objectMapper);
        }

        @Override
        public <T> T read(String json, TypeReference<T> typeReference) {
            return JsonUtils.read(json, typeReference.getType(), objectMapper);
        }

        @Override
        public Object read(String json, String path) {
            return JsonUtils.read(json, path, objectMapper);
        }

        @Override
        public <T> T read(String json, String path, Class<T> valueType) {
            return JsonUtils.read(json, path, valueType, objectMapper);
        }

        @Override
        public <T> T read(String json, String path, TypeReference<T> typeReference) {
            return JsonUtils.read(json, path, typeReference.getType(), objectMapper);
        }

        @Override
        public List<?> readList(InputStream inputStream) {
            return JsonUtils.readList(inputStream, objectMapper);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, Class<T> elementType) {
            return JsonUtils.readList(inputStream, elementType, objectMapper);
        }

        @Override
        public List<?> readList(InputStream inputStream, String path) {
            return JsonUtils.readList(inputStream, path, objectMapper);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
            return JsonUtils.readList(inputStream, path, elementType, objectMapper);
        }

        @Override
        public List<?> readList(String json) {
            return JsonUtils.readList(json, objectMapper);
        }

        @Override
        public <T> List<T> readList(String json, Class<T> elementType) {
            return JsonUtils.readList(json, elementType, objectMapper);
        }

        @Override
        public List<?> readList(String json, String path) {
            return JsonUtils.readList(json, path, objectMapper);
        }

        @Override
        public <T> List<T> readList(String json, String path, Class<T> elementType) {
            return JsonUtils.readList(json, path, elementType, objectMapper);
        }

        @Override
        public <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType) {
            return JsonUtils.readMap(inputStream, valueType, objectMapper);
        }

        @Override
        public Map<String, ?> readMap(InputStream inputStream, String path) {
            return JsonUtils.readMap(inputStream, path, objectMapper);
        }

        @Override
        public <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType) {
            return JsonUtils.readMap(inputStream, path, valueType, objectMapper);
        }

        @Override
        public Map<String, ?> readMap(String json) {
            return JsonUtils.readMap(json, objectMapper);
        }

        @Override
        public <V> Map<String, V> readMap(String json, Class<V> valueType) {
            return JsonUtils.readMap(json, valueType, objectMapper);
        }

        @Override
        public Map<String, ?> readMap(String json, String path) {
            return JsonUtils.readMap(json, path, objectMapper);
        }

        @Override
        public <V> Map<String, V> readMap(String json, String path, Class<V> valueType) {
            return JsonUtils.readMap(json, path, valueType, objectMapper);
        }

        @Override
        public Stream<Map<String, ?>> stream(InputStream inputStream) {
            return JsonUtils.stream(inputStream, objectMapper);
        }

        @Override
        public String write(Object object) {
            return JsonUtils.write(object, objectMapper);
        }
    }

    private record XmlImpl(XmlMapper xmlMapper) implements Xml {

        @Override
        public Map<String, ?> read(InputStream inputStream) {
            return XmlUtils.read(inputStream, xmlMapper);
        }

        @Override
        public <T> Map<String, T> read(InputStream inputStream, Class<T> valueType) {
            return XmlUtils.read(inputStream, valueType, xmlMapper);
        }

        @Override
        public <T> Map<String, T> read(InputStream inputStream, TypeReference<T> valueTypeReference) {
            return XmlUtils.read(inputStream, valueTypeReference.getType(), xmlMapper);
        }

        @Override
        public Map<String, ?> read(String xml) {
            return XmlUtils.read(xml, xmlMapper);
        }

        @Override
        public <T> Map<String, T> read(String xml, Class<T> valueType) {
            return XmlUtils.read(xml, valueType, xmlMapper);
        }

        @Override
        public <T> Map<String, T> read(String xml, TypeReference<T> valueTypeReference) {
            return XmlUtils.read(xml, valueTypeReference.getType(), xmlMapper);
        }

        @Override
        public List<?> readList(InputStream inputStream, String path) {
            return XmlUtils.readList(inputStream, path, xmlMapper);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
            return XmlUtils.readList(inputStream, path, elementType, xmlMapper);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, String path, TypeReference<T> elementTypeReference) {
            return XmlUtils.readList(inputStream, path, elementTypeReference.getType(), xmlMapper);
        }

        @Override
        public Stream<Map<String, ?>> stream(InputStream inputStream) {
            return XmlUtils.stream(inputStream, xmlMapper);
        }

        @Override
        public String write(Object object) {
            return XmlUtils.write(object, xmlMapper);
        }

        @Override
        public String write(Object object, String rootName) {
            return XmlUtils.write(object, rootName, xmlMapper);
        }
    }
}
