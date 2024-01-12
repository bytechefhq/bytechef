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

import static com.bytechef.hermes.component.definition.ComponentDSL.object;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Property;
import com.bytechef.platform.component.registry.domain.ComponentConnection;
import com.bytechef.platform.component.util.OutputSchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ContextImpl implements Context {

    private final Http http;
    private final Json json;
    private final Logger logger;
    private final OutputSchema outputSchema;
    private final Xml xml;

    @SuppressFBWarnings("EI")
    public ContextImpl(
        String componentName, String operationName, ComponentConnection connection,
        HttpClientExecutor httpClientExecutor) {

        this.http = new HttpImpl(componentName, connection, this, httpClientExecutor);
        this.json = new JsonImpl();
        this.logger = new LoggerImpl(componentName, operationName);
        this.outputSchema = new OutputSchemaImpl();
        this.xml = new XmlImpl();
    }

    @Override
    public <R> R http(ContextFunction<Http, R> httpFunction) {
        try {
            return httpFunction.apply(http);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R json(ContextFunction<Json, R> jsonFunction) {
        try {
            return jsonFunction.apply(json);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public void logger(ContextConsumer<Logger> loggerConsumer) {
        try {
            loggerConsumer.accept(logger);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Property.OutputProperty<?>
        outputSchema(ContextFunction<OutputSchema, Property.OutputProperty<?>> outputSchemaFunction) {
        try {
            return outputSchemaFunction.apply(outputSchema);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
        try {
            return xmlFunction.apply(xml);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private record HttpImpl(
        String componentName, ComponentConnection connection, Context context, HttpClientExecutor httpClientExecutor)
        implements Http {

        @Override
        public Executor delete(String url) {
            return new ExecutorImpl(url, RequestMethod.DELETE, componentName, connection, httpClientExecutor, context);
        }

        @Override
        public Executor exchange(String url, RequestMethod requestMethod) {
            return new ExecutorImpl(url, requestMethod, componentName, connection, httpClientExecutor, context);
        }

        @Override
        public Executor head(String url) {
            return new ExecutorImpl(
                url, RequestMethod.HEAD, componentName, connection, httpClientExecutor, context);
        }

        @Override
        public Executor get(String url) {
            return new ExecutorImpl(url, RequestMethod.GET, componentName, connection, httpClientExecutor, context);
        }

        @Override
        public Executor patch(String url) {
            return new ExecutorImpl(
                url, RequestMethod.PATCH, componentName, connection, httpClientExecutor, context);
        }

        @Override
        public Executor post(String url) {
            return new ExecutorImpl(
                url, RequestMethod.POST, componentName, connection, httpClientExecutor, context);
        }

        @Override
        public Executor put(String url) {
            return new ExecutorImpl(url, RequestMethod.PUT, componentName, connection, httpClientExecutor, context);
        }

        private static class ExecutorImpl implements Executor {

            private Body body;
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
                HttpClientExecutor httpClientExecutor, Context context) {

                this.componentName = componentName;
                this.connection = connection;
                this.context = context;
                this.httpClientExecutor = httpClientExecutor;
                this.url = url;
                this.requestMethod = requestMethod;
            }

            @Override
            public Executor configuration(Configuration.ConfigurationBuilder configurationBuilder) {
                this.configuration = configurationBuilder.build();

                return this;
            }

            @Override
            public Executor header(String name, String value) {
                headers.put(Validate.notNull(name, "name"), List.of(Validate.notNull(value, "value")));

                return this;
            }

            @Override
            public Executor headers(Map<String, List<String>> headers) {
                this.headers = new HashMap<>(Validate.notNull(headers, "headers"));

                return this;
            }

            @Override
            public Executor queryParameter(String name, String value) {
                queryParameters.put(Validate.notNull(name, "name"), List.of(Validate.notNull(value, "value")));

                return this;
            }

            @Override
            public Executor queryParameters(Map<String, List<String>> queryParameters) {
                this.queryParameters = new HashMap<>(Validate.notNull(queryParameters, "queryParameters"));

                return this;
            }

            @Override
            public Executor body(Body body) {
                this.body = body;

                return this;
            }

            @Override
            public Response execute() {
                try {
                    return httpClientExecutor.execute(
                        url, headers, queryParameters, body, configuration, requestMethod, componentName, connection,
                        context);
                } catch (Exception e) {
                    throw new RuntimeException("Unable to execute HTTP request", e);
                }
            }
        }
    }

    private record JsonImpl() implements Json {

        @Override
        public Object read(InputStream inputStream) {
            return JsonUtils.read(inputStream);
        }

        @Override
        public <T> T read(InputStream inputStream, Class<T> valueType) {
            return JsonUtils.read(inputStream, valueType);
        }

        @Override
        public <T> T read(InputStream inputStream, TypeReference<T> typeReference) {
            return JsonUtils.read(inputStream, typeReference.getType());
        }

        @Override
        public Object read(InputStream inputStream, String path) {
            return JsonUtils.read(inputStream, path);
        }

        @Override
        public <T> T read(InputStream inputStream, String path, Class<T> valueType) {
            return JsonUtils.read(inputStream, path, valueType);
        }

        @Override
        public <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference) {
            return JsonUtils.read(inputStream, path, typeReference.getType());
        }

        @Override
        public Object read(String json) {
            return JsonUtils.read(json);
        }

        @Override
        public <T> T read(String json, Class<T> valueType) {
            return JsonUtils.read(json, valueType);
        }

        @Override
        public <T> T read(String json, TypeReference<T> typeReference) {
            return JsonUtils.read(json, typeReference.getType());
        }

        @Override
        public Object read(String json, String path) {
            return JsonUtils.read(json, path);
        }

        @Override
        public <T> T read(String json, String path, Class<T> valueType) {
            return JsonUtils.read(json, path, valueType);
        }

        @Override
        public <T> T read(String json, String path, TypeReference<T> typeReference) {
            return JsonUtils.read(json, path, typeReference.getType());
        }

        @Override
        public List<?> readList(InputStream inputStream) {
            return JsonUtils.readList(inputStream);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, Class<T> elementType) {
            return JsonUtils.readList(inputStream, elementType);
        }

        @Override
        public List<?> readList(InputStream inputStream, String path) {
            return JsonUtils.readList(inputStream, path);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
            return JsonUtils.readList(inputStream, path, elementType);
        }

        @Override
        public List<?> readList(String json) {
            return JsonUtils.readList(json);
        }

        @Override
        public <T> List<T> readList(String json, Class<T> elementType) {
            return JsonUtils.readList(json, elementType);
        }

        @Override
        public List<?> readList(String json, String path) {
            return JsonUtils.readList(json, path);
        }

        @Override
        public <T> List<T> readList(String json, String path, Class<T> elementType) {
            return JsonUtils.readList(json, path, elementType);
        }

        @Override
        public <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType) {
            return JsonUtils.readMap(inputStream, valueType);
        }

        @Override
        public Map<String, ?> readMap(InputStream inputStream, String path) {
            return JsonUtils.readMap(inputStream, path);
        }

        @Override
        public <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType) {
            return JsonUtils.readMap(inputStream, path, valueType);
        }

        @Override
        public Map<String, ?> readMap(String json) {
            return JsonUtils.readMap(json);
        }

        @Override
        public <V> Map<String, V> readMap(String json, Class<V> valueType) {
            return JsonUtils.readMap(json, valueType);
        }

        @Override
        public Map<String, ?> readMap(String json, String path) {
            return JsonUtils.readMap(json, path);
        }

        @Override
        public <V> Map<String, V> readMap(String json, String path, Class<V> valueType) {
            return JsonUtils.readMap(json, path, valueType);
        }

        @Override
        public Stream<Map<String, ?>> stream(InputStream inputStream) {
            return JsonUtils.stream(inputStream);
        }

        @Override
        public String write(Object object) {
            return JsonUtils.write(object);
        }
    }

    private static class LoggerImpl implements Logger {

        private final org.slf4j.Logger logger;

        public LoggerImpl(String componentName, String actionName) {
            logger = LoggerFactory.getLogger(componentName + (actionName == null ? "" : "." + actionName));
        }

        @Override
        public void debug(String message) {
            if (logger.isDebugEnabled()) {
                logger.debug(message);
            }
        }

        @Override
        public void debug(String format, Object... args) {
            if (logger.isDebugEnabled()) {
                logger.debug(format, args);
            }
        }

        @Override
        public void debug(String message, Exception exception) {
            if (logger.isDebugEnabled()) {
                logger.debug(message, exception);
            }
        }

        @Override
        public void error(String message) {
            if (logger.isErrorEnabled()) {
                logger.error(message);
            }
        }

        @Override
        public void error(String format, Object... args) {
            if (logger.isErrorEnabled()) {
                logger.error(format, args);
            }
        }

        @Override
        public void error(String message, Exception exception) {
            if (logger.isErrorEnabled()) {
                logger.error(message, exception);
            }
        }

        @Override
        public void info(String message) {
            if (logger.isInfoEnabled()) {
                logger.info(message);
            }
        }

        @Override
        public void info(String format, Object... args) {
            if (logger.isInfoEnabled()) {
                logger.info(format, args);
            }
        }

        @Override
        public void info(String message, Exception exception) {
            if (logger.isInfoEnabled()) {
                logger.info(message, exception);
            }
        }

        @Override
        public void warn(String message) {
            if (logger.isWarnEnabled()) {
                logger.warn(message);
            }
        }

        @Override
        public void warn(String format, Object... args) {
            if (logger.isWarnEnabled()) {
                logger.warn(format, args);
            }
        }

        @Override
        public void warn(String message, Exception exception) {
            if (logger.isWarnEnabled()) {
                logger.warn(message, exception);
            }
        }

        @Override
        public void trace(String message) {
            if (logger.isTraceEnabled()) {
                logger.trace(message);
            }
        }

        @Override
        public void trace(String format, Object... args) {
            if (logger.isTraceEnabled()) {
                logger.trace(format, args);
            }
        }

        @Override
        public void trace(String message, Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(message, exception);
            }
        }
    }

    private static class OutputSchemaImpl implements OutputSchema {

        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OutputSchemaImpl.class);

        private OutputSchemaImpl() {
        }

        @Override
        public Property.OutputProperty<?> get(Object value) {
            return OutputSchemaUtils.getOutputSchema(value);
        }
    }

    private record XmlImpl() implements Xml {

        @Override
        public Map<String, ?> read(InputStream inputStream) {
            return XmlUtils.read(inputStream);
        }

        @Override
        public <T> Map<String, T> read(InputStream inputStream, Class<T> valueType) {
            return XmlUtils.read(inputStream, valueType);
        }

        @Override
        public <T> Map<String, T> read(InputStream inputStream, TypeReference<T> valueTypeReference) {
            return XmlUtils.read(inputStream, valueTypeReference.getType());
        }

        @Override
        public Map<String, ?> read(String xml) {
            return XmlUtils.read(xml);
        }

        @Override
        public <T> Map<String, T> read(String xml, Class<T> valueType) {
            return XmlUtils.read(xml, valueType);
        }

        @Override
        public <T> Map<String, T> read(String xml, TypeReference<T> valueTypeReference) {
            return XmlUtils.read(xml, valueTypeReference.getType());
        }

        @Override
        public List<?> readList(InputStream inputStream, String path) {
            return XmlUtils.readList(inputStream, path);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
            return XmlUtils.readList(inputStream, path, elementType);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, String path, TypeReference<T> elementTypeReference) {
            return XmlUtils.readList(inputStream, path, elementTypeReference.getType());
        }

        @Override
        public Stream<Map<String, ?>> stream(InputStream inputStream) {
            return XmlUtils.stream(inputStream);
        }

        @Override
        public String write(Object object) {
            return XmlUtils.write(object);
        }

        @Override
        public String write(Object object, String rootName) {
            return XmlUtils.write(object, rootName);
        }
    }
}
