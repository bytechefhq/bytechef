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

import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MimeTypeUtils;
import com.bytechef.commons.util.XmlUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.util.JsonSchemaUtils;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
class ContextImpl implements Context {

    private final Convert convert;
    private final Encoder encoder;
    private final File file;
    private final Http http;
    private final Json json;
    private final Log log;
    private final MimeType mimeType;
    private final OutputSchema outputSchema;
    private final boolean editorEnvironment;
    private final Xml xml;

    @SuppressFBWarnings("EI")
    public ContextImpl(
        String componentName, int componentVersion, String componentOperationName,
        @Nullable ComponentConnection componentConnection, boolean editorEnvironment,
        HttpClientExecutor httpClientExecutor, TempFileStorage tempFileStorage) {

        this.convert = new ConvertImpl();
        this.editorEnvironment = editorEnvironment;
        this.encoder = new EncoderImpl();
        this.file = new FileImpl(tempFileStorage);
        this.http = new HttpImpl(
            componentName, componentVersion, componentOperationName, componentConnection, this, httpClientExecutor);
        this.json = new JsonImpl();
        this.log = new LogImpl(componentName, componentOperationName);
        this.mimeType = new MimeTypeImpl();
        this.outputSchema = new OutputSchemaImpl();
        this.xml = new XmlImpl();
    }

    @Override
    public <R> R convert(ContextFunction<Convert, R> convertFunction) {
        try {
            return convertFunction.apply(convert);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R encoder(ContextFunction<Encoder, R> encoderFunction) {
        try {
            return encoderFunction.apply(encoder);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R file(ContextFunction<File, R> fileFunction) {
        try {
            return fileFunction.apply(file);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
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
    public boolean isEditorEnvironment() {
        return editorEnvironment;
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
    public void log(ContextConsumer<Log> logConsumer) {
        try {
            logConsumer.accept(log);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R mimeType(ContextFunction<MimeType, R> mimeTypeContextFunction) {
        try {
            return mimeTypeContextFunction.apply(mimeType);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction) {
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
        String componentName, int componentVersion, String componentOperationName,
        ComponentConnection componentConnection, Context context, HttpClientExecutor httpClientExecutor)

        implements Http {

        @Override
        public Executor delete(String url) {
            return new ExecutorImpl(
                url, RequestMethod.DELETE, componentName, componentVersion, componentOperationName, componentConnection,
                httpClientExecutor, context);
        }

        @Override
        public Executor exchange(String url, RequestMethod requestMethod) {
            return new ExecutorImpl(url, requestMethod, componentName, componentVersion, componentOperationName,
                componentConnection, httpClientExecutor, context);
        }

        @Override
        public Executor head(String url) {
            return new ExecutorImpl(
                url, RequestMethod.HEAD, componentName, componentVersion, componentOperationName, componentConnection,
                httpClientExecutor, context);
        }

        @Override
        public Executor get(String url) {
            return new ExecutorImpl(url, RequestMethod.GET, componentName, componentVersion, componentOperationName,
                componentConnection, httpClientExecutor, context);
        }

        @Override
        public Executor patch(String url) {
            return new ExecutorImpl(
                url, RequestMethod.PATCH, componentName, componentVersion, componentOperationName, componentConnection,
                httpClientExecutor, context);
        }

        @Override
        public Executor post(String url) {
            return new ExecutorImpl(
                url, RequestMethod.POST, componentName, componentVersion, componentOperationName, componentConnection,
                httpClientExecutor, context);
        }

        @Override
        public Executor put(String url) {
            return new ExecutorImpl(
                url, RequestMethod.PUT, componentName, componentVersion, componentOperationName, componentConnection,
                httpClientExecutor, context);
        }

        private static class ExecutorImpl implements Executor {

            private Body body;
            private final String componentName;
            private final int componentVersion;
            private final String componentOperationName;
            private Configuration configuration = new Configuration();
            private final ComponentConnection componentConnection;
            private final Context context;
            private final HttpClientExecutor httpClientExecutor;
            private Map<String, List<String>> headers = new HashMap<>();
            private Map<String, List<String>> queryParameters = new HashMap<>();
            private final RequestMethod requestMethod;
            private final String url;

            private ExecutorImpl(
                String url, RequestMethod requestMethod, String componentName, int componentVersion,
                String componentOperationName, ComponentConnection componentConnection,
                HttpClientExecutor httpClientExecutor, Context context) {

                this.componentName = componentName;
                this.componentVersion = componentVersion;
                this.componentOperationName = componentOperationName;
                this.componentConnection = componentConnection;
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
            public Executor queryParameters(Object... keyValueArray) {
                Objects.requireNonNull(keyValueArray);

                if (keyValueArray.length % 2 != 0) {
                    throw new IllegalArgumentException();
                }

                this.queryParameters = IntStream
                    .range(0, keyValueArray.length / 2)
                    .filter(i -> keyValueArray[i * 2] != null && keyValueArray[i * 2 + 1] != null)
                    .collect(
                        HashMap::new,
                        (map, i) -> map.put(String.valueOf(keyValueArray[i * 2]),
                            List.of(URLEncoder.encode(keyValueArray[i * 2 + 1].toString(), StandardCharsets.UTF_8))),
                        HashMap::putAll);

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
                        url, headers, queryParameters, body, configuration, requestMethod, componentName,
                        componentVersion, componentOperationName, componentConnection, context);
                } catch (Exception e) {
                    if (e instanceof ProviderException pe) {
                        throw pe;
                    }

                    throw new RuntimeException("Unable to execute HTTP request", e);
                }
            }
        }
    }

    private record ConvertImpl() implements Convert {

        @Override
        public boolean canConvert(Object fromValue, Class<?> toValueType) {
            return ConvertUtils.canConvert(fromValue, toValueType);
        }

        @Override
        public <T> T value(Object fromValue, Class<T> toValueType) {
            return ConvertUtils.convertValue(fromValue, toValueType);
        }

        @Override
        public <T> T value(Object fromValue, Class<T> toValueType, boolean includeNulls) {
            return ConvertUtils.convertValue(fromValue, toValueType, includeNulls);
        }

        @Override
        public <T> T value(Object fromValue, TypeReference<T> toValueTypeRef) {
            return ConvertUtils.convertValue(fromValue, toValueTypeRef.getType());
        }

        @Override
        public Object string(String str) {
            return ConvertUtils.convertString(str);
        }
    }

    private record EncoderImpl() implements Encoder {

        @Override
        public byte[] base64Decode(String string) {
            return EncodingUtils.base64Decode(string);
        }

        @Override
        public String base64EncodeToString(byte[] bytes) {
            return EncodingUtils.base64EncodeToString(bytes);
        }

        @Override
        public byte[] urlDecodeBase64FromString(String string) {
            return EncodingUtils.urlDecodeBase64FromString(string);
        }

        @Override
        public String urlEncode(String string) {
            return EncodingUtils.urlEncode(string);
        }
    }

    private record FileImpl(TempFileStorage tempFileStorage) implements File {

        @Override
        public long getContentLength(FileEntry fileEntry) {
            return tempFileStorage.getContentLength(((FileEntryImpl) fileEntry).getFileEntry());
        }

        @Override
        public InputStream getInputStream(FileEntry fileEntry) {
            return tempFileStorage.getInputStream(((FileEntryImpl) fileEntry).getFileEntry());
        }

        @Override
        public OutputStream getOutputStream(FileEntry fileEntry) {
            return tempFileStorage.getOutputStream(((FileEntryImpl) fileEntry).getFileEntry());
        }

        @Override
        public String readToString(FileEntry fileEntry) {
            return tempFileStorage.readFileToString(((FileEntryImpl) fileEntry).getFileEntry());
        }

        @Override
        public FileEntry storeContent(String fileName, String data) {
            return new FileEntryImpl(tempFileStorage.storeFileContent(fileName, data));
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
                tempFilePath = Files.createTempFile("context_", fileEntry.getName());

                Files.copy(
                    tempFileStorage.getInputStream(toFileEntry(fileEntry)), tempFilePath,
                    StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return tempFilePath;
        }

        @Override
        public byte[] readAllBytes(FileEntry fileEntry) throws IOException {
            InputStream inputStream = getInputStream(fileEntry);

            return inputStream.readAllBytes();
        }

        @Override
        public FileEntry storeContent(String fileName, InputStream inputStream) {
            try {
                return new FileEntryImpl(tempFileStorage.storeFileContent(fileName, inputStream));
            } catch (Exception exception) {
                throw new RuntimeException("Unable to store file " + fileName, exception);
            }
        }

        private static com.bytechef.file.storage.domain.FileEntry toFileEntry(FileEntry fileEntry) {
            return new com.bytechef.file.storage.domain.FileEntry(
                fileEntry.getName(), fileEntry.getExtension(), fileEntry.getMimeType(), fileEntry.getUrl());
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

    private static class LogImpl implements Log {

        private final org.slf4j.Logger logger;

        public LogImpl(String componentName, String componentOperationName) {
            logger = LoggerFactory.getLogger(
                componentName + (componentOperationName == null ? "" : "." + componentOperationName));
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

    private record OutputSchemaImpl() implements OutputSchema {

        @Override
        public ValueProperty<?> getOutputSchema(String jsonSchema) {
            return JsonSchemaUtils.getProperty(jsonSchema);
        }

        @Override
        public ValueProperty<?> getOutputSchema(String propertyName, String jsonSchema) {
            return JsonSchemaUtils.getProperty(propertyName, jsonSchema);
        }

        @Override
        public ValueProperty<?> getOutputSchema(Object value) {
            return (ValueProperty<?>) SchemaUtils.getOutputSchema(value, PropertyFactory.PROPERTY_FACTORY);
        }
    }

    private record MimeTypeImpl() implements MimeType {
        @Override
        public String lookupMimeType(String ext) {
            return MimeTypeUtils.lookupMimeType(ext);
        }

        @Override
        public String lookupExt(String mimeType) {
            return MimeTypeUtils.lookupExt(mimeType);
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
