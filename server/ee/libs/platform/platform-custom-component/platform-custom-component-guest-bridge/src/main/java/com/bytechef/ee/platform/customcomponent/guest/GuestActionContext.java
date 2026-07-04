/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.guest;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * {@link ActionContext} implementation available to custom component code running inside the GraalVM Espresso guest
 * JVM. JSON, encoding and conversion run guest-locally; logging and HTTP delegate to the host through the narrow
 * {@link HostBridge} callback; everything else is unsupported in the sandbox and reachable only via the in-process
 * classloader flag.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GuestActionContext implements ActionContext {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        .findAndAddModules()
        .build();

    private final HostBridge hostBridge;
    private final String traceId;

    public GuestActionContext(HostBridge hostBridge, String traceId) {
        this.hostBridge = hostBridge;
        this.traceId = traceId;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    @Override
    public Approval.Links approval(ContextFunction<Approval, Approval.Links> approvalFunction) {
        throw newUnsupportedException("approval");
    }

    @Override
    public <R> R data(ContextFunction<Data, R> dataFunction) {
        throw newUnsupportedException("data");
    }

    @Override
    public void event(Consumer<Event> eventConsumer) {
        throw newUnsupportedException("event");
    }

    @Override
    public void suspend(Suspend suspend) {
        throw newUnsupportedException("suspend");
    }

    @Override
    public <R> R converter(ContextFunction<Converter, R> converterFunction) {
        return applyContextFunction(converterFunction, new GuestConverter());
    }

    @Override
    public <R> R encoder(ContextFunction<Encoder, R> encoderFunction) {
        return applyContextFunction(encoderFunction, new GuestEncoder());
    }

    @Override
    public <R> R escaper(ContextFunction<Escaper, R> escaperFunction) {
        throw newUnsupportedException("escaper");
    }

    @Override
    public <R> R file(ContextFunction<File, R> fileFunction) {
        throw newUnsupportedException("file");
    }

    @Override
    public <R> R http(ContextFunction<Http, R> httpFunction) {
        return applyContextFunction(httpFunction, new GuestHttp(hostBridge));
    }

    @Override
    public boolean isEditorEnvironment() {
        return hostBridge.isEditorEnvironment();
    }

    @Override
    public <R> R json(ContextFunction<Json, R> jsonFunction) {
        return applyContextFunction(jsonFunction, new GuestJson());
    }

    @Override
    public void log(ContextConsumer<Log> logConsumer) {
        try {
            logConsumer.accept(new GuestLog(hostBridge));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <R> R mimeType(ContextFunction<MimeType, R> mimeTypeFunction) {
        throw newUnsupportedException("mimeType");
    }

    @Override
    public <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction) {
        throw newUnsupportedException("outputSchema");
    }

    @Override
    public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
        throw newUnsupportedException("xml");
    }

    private static <T, R> R applyContextFunction(ContextFunction<T, R> contextFunction, T argument) {
        try {
            return contextFunction.apply(argument);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static UnsupportedOperationException newUnsupportedException(String area) {
        return new UnsupportedOperationException(
            "Context area '%s' is not supported in the Espresso custom component sandbox; set ".formatted(area) +
                "bytechef.component.custom-component.java-loader=class-loader to use the in-process loader");
    }

    private record GuestConverter() implements Converter {

        @Override
        public boolean canConvert(Object fromValue, Class<?> toValueType) {
            try {
                OBJECT_MAPPER.convertValue(fromValue, toValueType);

                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        @Override
        public <T> T value(Object fromValue, Class<T> toValueType) {
            return OBJECT_MAPPER.convertValue(fromValue, toValueType);
        }

        @Override
        public <T> T value(Object fromValue, TypeReference<T> toValueTypeRef) {
            TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

            return OBJECT_MAPPER.convertValue(fromValue, typeFactory.constructType(toValueTypeRef.getType()));
        }

        @Override
        public Object string(String str) {
            return str;
        }
    }

    private record GuestEncoder() implements Encoder {

        @Override
        public byte[] base64Decode(String string) {
            Base64.Decoder decoder = Base64.getDecoder();

            return decoder.decode(string);
        }

        @Override
        public String base64Encode(String... values) {
            Base64.Encoder base64Encoder = Base64.getEncoder();

            String joined = String.join("", values);

            return base64Encoder.encodeToString(joined.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String base64Encode(byte[] bytes) {
            Base64.Encoder base64Encoder = Base64.getEncoder();

            return base64Encoder.encodeToString(bytes);
        }

        @Override
        public byte[] base64UrlDecode(String value) {
            Base64.Decoder decoder = Base64.getUrlDecoder();

            return decoder.decode(value);
        }

        @Override
        public String base64UrlEncode(String value) {
            Base64.Encoder base64Encoder = Base64.getUrlEncoder();

            return base64Encoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String base64UrlEncode(byte[] bytes) {
            Base64.Encoder base64Encoder = Base64.getUrlEncoder();

            return base64Encoder.encodeToString(bytes);
        }
    }

    private record GuestJson() implements Json {

        @Override
        public Object read(InputStream inputStream) {
            return readValue(inputStream, Object.class);
        }

        @Override
        public <T> T read(InputStream inputStream, Class<T> valueType) {
            return readValue(inputStream, valueType);
        }

        @Override
        public <T> T read(InputStream inputStream, TypeReference<T> typeReference) {
            return OBJECT_MAPPER.convertValue(readValue(inputStream, Object.class), constructType(typeReference));
        }

        @Override
        public Object read(InputStream inputStream, String path) {
            throw newPathUnsupportedException();
        }

        @Override
        public <T> T read(InputStream inputStream, String path, Class<T> valueType) {
            throw newPathUnsupportedException();
        }

        @Override
        public <T> T read(InputStream inputStream, String path, TypeReference<T> typeReference) {
            throw newPathUnsupportedException();
        }

        @Override
        public Object read(String json) {
            return readValue(json, Object.class);
        }

        @Override
        public <T> T read(String json, Class<T> valueType) {
            return readValue(json, valueType);
        }

        @Override
        public <T> T read(String json, TypeReference<T> typeReference) {
            return OBJECT_MAPPER.convertValue(readValue(json, Object.class), constructType(typeReference));
        }

        @Override
        public Object read(String json, String path) {
            throw newPathUnsupportedException();
        }

        @Override
        public <T> T read(String json, String path, Class<T> valueType) {
            throw newPathUnsupportedException();
        }

        @Override
        public <T> T read(String json, String path, TypeReference<T> typeReference) {
            throw newPathUnsupportedException();
        }

        @Override
        public List<?> readList(InputStream inputStream) {
            return readValue(inputStream, List.class);
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, Class<T> elementType) {
            return toTypedList(readValue(inputStream, List.class), elementType);
        }

        @Override
        public List<?> readList(InputStream inputStream, String path) {
            throw newPathUnsupportedException();
        }

        @Override
        public <T> List<T> readList(InputStream inputStream, String path, Class<T> elementType) {
            throw newPathUnsupportedException();
        }

        @Override
        public List<?> readList(String json) {
            return readValue(json, List.class);
        }

        @Override
        public <T> List<T> readList(String json, Class<T> elementType) {
            return toTypedList(readValue(json, List.class), elementType);
        }

        @Override
        public List<?> readList(String json, String path) {
            throw newPathUnsupportedException();
        }

        @Override
        public <T> List<T> readList(String json, String path, Class<T> elementType) {
            throw newPathUnsupportedException();
        }

        @Override
        public <V> Map<String, V> readMap(InputStream inputStream, Class<V> valueType) {
            return toTypedMap(readValue(inputStream, Map.class), valueType);
        }

        @Override
        public Map<String, ?> readMap(InputStream inputStream, String path) {
            throw newPathUnsupportedException();
        }

        @Override
        public <V> Map<String, V> readMap(InputStream inputStream, String path, Class<V> valueType) {
            throw newPathUnsupportedException();
        }

        @Override
        public Map<String, ?> readMap(String json) {
            return toTypedMap(readValue(json, Map.class), Object.class);
        }

        @Override
        public <V> Map<String, V> readMap(String json, Class<V> valueType) {
            return toTypedMap(readValue(json, Map.class), valueType);
        }

        @Override
        public Map<String, ?> readMap(String json, String path) {
            throw newPathUnsupportedException();
        }

        @Override
        public <V> Map<String, V> readMap(String json, String path, Class<V> valueType) {
            throw newPathUnsupportedException();
        }

        @Override
        public Stream<Map<String, ?>> stream(InputStream inputStream) {
            throw newPathUnsupportedException();
        }

        @Override
        public String write(Object object) {
            try {
                return OBJECT_MAPPER.writeValueAsString(object);
            } catch (Exception e) {
                throw new UncheckedIOException(new java.io.IOException(e));
            }
        }

        private static <T> T readValue(String json, Class<T> valueType) {
            try {
                return OBJECT_MAPPER.readValue(json, valueType);
            } catch (Exception e) {
                throw new UncheckedIOException(new java.io.IOException(e));
            }
        }

        private static <T> T readValue(InputStream inputStream, Class<T> valueType) {
            try {
                return OBJECT_MAPPER.readValue(inputStream, valueType);
            } catch (Exception e) {
                throw new UncheckedIOException(new java.io.IOException(e));
            }
        }

        private static com.fasterxml.jackson.databind.JavaType constructType(TypeReference<?> typeReference) {
            TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

            return typeFactory.constructType(typeReference.getType());
        }

        private static <T> List<T> toTypedList(List<?> list, Class<T> elementType) {
            TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

            return OBJECT_MAPPER.convertValue(list, typeFactory.constructCollectionType(List.class, elementType));
        }

        @SuppressWarnings("unchecked")
        private static <V> Map<String, V> toTypedMap(Map<?, ?> map, Class<V> valueType) {
            TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();

            return (Map<String, V>) OBJECT_MAPPER.convertValue(
                map, typeFactory.constructMapType(Map.class, String.class, valueType));
        }

        private static UnsupportedOperationException newPathUnsupportedException() {
            return new UnsupportedOperationException(
                "JSON path reads and streaming are not supported in the Espresso custom component sandbox; set " +
                    "bytechef.component.custom-component.java-loader=class-loader to use the in-process loader");
        }
    }

    private record GuestLog(HostBridge hostBridge) implements Log {

        @Override
        public void debug(String message) {
            hostBridge.log("DEBUG", message, null);
        }

        @Override
        public void debug(String format, Object... args) {
            hostBridge.log("DEBUG", format(format, args), null);
        }

        @Override
        public void debug(String message, Exception exception) {
            hostBridge.log("DEBUG", message, String.valueOf(exception));
        }

        @Override
        public void error(String message) {
            hostBridge.log("ERROR", message, null);
        }

        @Override
        public void error(String format, Object... args) {
            hostBridge.log("ERROR", format(format, args), null);
        }

        @Override
        public void error(String message, Exception exception) {
            hostBridge.log("ERROR", message, String.valueOf(exception));
        }

        @Override
        public void info(String message) {
            hostBridge.log("INFO", message, null);
        }

        @Override
        public void info(String format, Object... args) {
            hostBridge.log("INFO", format(format, args), null);
        }

        @Override
        public void info(String message, Exception exception) {
            hostBridge.log("INFO", message, String.valueOf(exception));
        }

        @Override
        public void warn(String message) {
            hostBridge.log("WARN", message, null);
        }

        @Override
        public void warn(String format, Object... args) {
            hostBridge.log("WARN", format(format, args), null);
        }

        @Override
        public void warn(String message, Exception exception) {
            hostBridge.log("WARN", message, String.valueOf(exception));
        }

        @Override
        public void trace(String message) {
            hostBridge.log("TRACE", message, null);
        }

        @Override
        public void trace(String format, Object... args) {
            hostBridge.log("TRACE", format(format, args), null);
        }

        @Override
        public void trace(String message, Exception exception) {
            hostBridge.log("TRACE", message, String.valueOf(exception));
        }

        private static String format(String format, Object... args) {
            StringBuilder message = new StringBuilder();

            int argumentIndex = 0;
            int searchFrom = 0;

            while (true) {
                int placeholderIndex = format.indexOf("{}", searchFrom);

                if (placeholderIndex < 0 || argumentIndex >= args.length) {
                    break;
                }

                message.append(format, searchFrom, placeholderIndex);
                message.append(args[argumentIndex]);

                argumentIndex++;
                searchFrom = placeholderIndex + 2;
            }

            message.append(format.substring(searchFrom));

            return message.toString();
        }
    }
}
