/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.Context.Http;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GuestActionContextTest {

    private final RecordingHostBridge hostBridge = new RecordingHostBridge();
    private final GuestActionContext guestActionContext = new GuestActionContext(hostBridge, "trace-1");

    @Test
    void testJsonRoundTrip() {
        String json = guestActionContext.json(jsonContext -> jsonContext.write(Map.of("greeting", "hello")));

        assertEquals("{\"greeting\":\"hello\"}", json);

        Map<String, ?> map = guestActionContext.json(jsonContext -> jsonContext.readMap(json));

        assertEquals("hello", map.get("greeting"));
    }

    @Test
    void testEncoder() {
        String encoded = guestActionContext.encoder(encoder -> encoder.base64Encode("bytechef"));

        byte[] decoded = guestActionContext.encoder(encoder -> encoder.base64Decode(encoded));

        assertEquals("bytechef", new String(decoded, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void testConverter() {
        Integer converted = guestActionContext.converter(converter -> converter.value("42", Integer.class));

        assertEquals(42, converted);
    }

    @Test
    void testLogDelegatesToHostBridge() {
        guestActionContext.log(log -> log.info("processed {} items in {}", 5, "batch"));

        assertEquals(List.of("INFO|processed 5 items in batch|null"), hostBridge.logEntries);
    }

    @Test
    void testTraceIdAndEditorEnvironment() {
        assertEquals("trace-1", guestActionContext.getTraceId());
        assertTrue(guestActionContext.isEditorEnvironment());
    }

    @Test
    void testUnsupportedAreasNameTheFlag() {
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class, () -> guestActionContext.file(file -> file.readToString(null)));

        assertTrue(exception.getMessage()
            .contains("java-loader=class-loader"));
    }

    @Test
    void testHttpRequestSerializationAndResponse() throws Exception {
        hostBridge.httpResponseJson = """
            {"statusCode": 201, "headers": {"X-Request-Id": ["abc"]}, "body": {"id": 7, "name": "created"}}
            """;

        Http.Response response = guestActionContext.http(http -> http.post("https://api.example.com/items")
            .header("Authorization", "Bearer token")
            .queryParameter("expand", "all")
            .body(Http.Body.of(Map.of("name", "created")))
            .configuration(
                Http.responseType(Http.ResponseType.JSON)
                    .disableAuthorization(true))
            .execute());

        assertEquals(201, response.getStatusCode());
        assertEquals("abc", response.getFirstHeader("X-Request-Id"));

        Map<String, ?> responseBody = response.getBody(new com.bytechef.component.definition.TypeReference<>() {});

        assertEquals("created", responseBody.get("name"));

        ObjectMapper objectMapper = new ObjectMapper();

        Map<String, ?> request = objectMapper.readValue(hostBridge.httpRequests.getFirst(), Map.class);

        assertEquals("POST", request.get("method"));
        assertEquals("https://api.example.com/items", request.get("url"));
        assertEquals(Map.of("Authorization", List.of("Bearer token")), request.get("headers"));
        assertEquals(Map.of("expand", List.of("all")), request.get("queryParameters"));

        Map<String, ?> requestBody = (Map<String, ?>) request.get("body");

        assertEquals("JSON", requestBody.get("contentType"));
        assertEquals(Map.of("name", "created"), requestBody.get("content"));

        Map<String, ?> configuration = (Map<String, ?>) request.get("configuration");

        assertEquals("JSON", configuration.get("responseType"));
        assertEquals(Boolean.TRUE, configuration.get("disableAuthorization"));
    }

    @Test
    void testHttpBinaryBodyUnsupported() {
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> guestActionContext.http(http -> http.post("https://api.example.com/upload")
                .body(Http.Body.of("content", Http.BodyContentType.BINARY))
                .execute()));

        assertTrue(exception.getMessage()
            .contains("BINARY"));
    }

    private static class RecordingHostBridge implements HostBridge {

        private final List<String> httpRequests = new ArrayList<>();
        private String httpResponseJson = "{\"statusCode\": 200, \"headers\": {}, \"body\": null}";
        private final List<String> logEntries = new ArrayList<>();

        @Override
        public String httpExecute(String requestJson) {
            httpRequests.add(requestJson);

            return httpResponseJson;
        }

        @Override
        public boolean isEditorEnvironment() {
            return true;
        }

        @Override
        public void log(String level, String message, String exceptionMessage) {
            logEntries.add(level + "|" + message + "|" + exceptionMessage);
        }
    }
}
