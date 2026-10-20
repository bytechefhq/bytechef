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

package com.bytechef.platform.data.table.execution.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.data.table.config.DataTableIntTestConfiguration;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableIntTestConfiguration.class)
@Import(H2DataSourceConfiguration.class)
class DataTableWebhookEventListenerIntTest {

    private static final long DEV_ENVIRONMENT_ID = 0;

    @MockitoBean
    private DataTableWebhookService dataTableWebhookService;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final BlockingQueue<String> deletedBodies = new LinkedBlockingQueue<>();
    private final AtomicInteger failuresLeft = new AtomicInteger();
    private final AtomicInteger failureStatus = new AtomicInteger(400);
    private HttpServer httpServer;
    private final BlockingQueue<String> updatedBodies = new LinkedBlockingQueue<>();

    @AfterEach
    void afterEach() {
        dataTableService.dropTable("items", DEV_ENVIRONMENT_ID);

        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @BeforeEach
    void beforeEach() {
        dataTableService.dropTable("items", DEV_ENVIRONMENT_ID);
        dataTableService.createTable(
            "items", null,
            List.of(new ColumnSpec("title", ColumnType.STRING), new ColumnSpec("amount", ColumnType.INTEGER)),
            DEV_ENVIRONMENT_ID);
    }

    @Test
    void testInsertRowOutsideATransactionCallsWebhooks() {
        dataTableRowService.insertRow("items", Map.of("title", "Apple"), DEV_ENVIRONMENT_ID);

        verify(dataTableWebhookService, timeout(5000)).listWebhooks("items", DEV_ENVIRONMENT_ID);
    }

    @Test
    void testDeleteRowsOfSeveralRowsListsWebhooksOnce() {
        dataTableRowService.insertRow("items", Map.of("title", "Apple"), DEV_ENVIRONMENT_ID);
        dataTableRowService.insertRow("items", Map.of("title", "Banana"), DEV_ENVIRONMENT_ID);

        verify(dataTableWebhookService, timeout(5000).times(2)).listWebhooks("items", DEV_ENVIRONMENT_ID);

        clearInvocations(dataTableWebhookService);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        verify(dataTableWebhookService, timeout(5000).times(1)).listWebhooks("items", DEV_ENVIRONMENT_ID);
        verify(dataTableWebhookService, after(500).times(1)).listWebhooks(anyString(), anyLong());
    }

    @Test
    void testDeleteRowsPostsOnePayloadPerDeletedRowOnlyToDeletedWebhooks() throws Exception {
        startWebhookServer();

        List<Long> ids = insertItems("Apple", "Banana");

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        List<String> bodies = takeBodies(deletedBodies, 2);

        for (String body : bodies) {
            assertTrue(body.contains("\"type\":\"RECORD_DELETED\""), body);
            assertEquals(
                1, ids.stream()
                    .filter(id -> containsId(body, id))
                    .count(),
                body);
        }

        assertTrue(
            ids.stream()
                .allMatch(id -> bodies.stream()
                    .anyMatch(body -> containsId(body, id))),
            bodies::toString);

        assertNull(deletedBodies.poll(500, TimeUnit.MILLISECONDS));
        assertNull(updatedBodies.poll(0, TimeUnit.MILLISECONDS));
    }

    @Test
    void testRejectedPayloadDoesNotStopTheRemainingPayloads() throws Exception {
        startWebhookServer();

        insertItems("Apple", "Banana", "Cherry");

        failuresLeft.set(1);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        assertEquals(3, takeBodies(deletedBodies, 3).size());
        assertNull(deletedBodies.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testServerErrorIsRetried() throws Exception {
        startWebhookServer();

        List<Long> ids = insertItems("Apple");

        failureStatus.set(503);
        failuresLeft.set(1);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        List<String> bodies = takeBodies(deletedBodies, 2);

        for (String body : bodies) {
            assertTrue(containsId(body, ids.getFirst()), body);
        }

        assertNull(deletedBodies.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testUpdateRowsPostsOnePayloadPerUpdatedRowWithTheUpdatedValues() throws Exception {
        startWebhookServer();

        List<Long> ids = insertItems("Apple", "Banana");

        dataTableRowService.updateRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            Map.of("title", "sold"), DEV_ENVIRONMENT_ID);

        List<String> bodies = takeBodies(updatedBodies, 2);

        for (String body : bodies) {
            assertTrue(body.contains("\"type\":\"RECORD_UPDATED\""), body);
            assertTrue(body.contains("\"title\":\"sold\""), body);
        }

        assertTrue(
            ids.stream()
                .allMatch(id -> bodies.stream()
                    .anyMatch(body -> containsId(body, id))),
            bodies::toString);

        assertNull(updatedBodies.poll(500, TimeUnit.MILLISECONDS));
        assertNull(deletedBodies.poll(0, TimeUnit.MILLISECONDS));
    }

    @Test
    void testWebhooksAreCalledOffThePublishingThread() throws Exception {
        CompletableFuture<Thread> listenerThread = new CompletableFuture<>();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenAnswer(invocation -> {
            listenerThread.complete(Thread.currentThread());

            return List.of();
        });

        dataTableRowService.insertRow("items", Map.of("title", "Apple"), DEV_ENVIRONMENT_ID);

        Thread thread = listenerThread.get(5, TimeUnit.SECONDS);

        assertNotNull(thread);
        assertNotEquals(Thread.currentThread(), thread);
    }

    @Test
    void testRolledBackImportCsvCallsNoWebhooks() {
        assertThrows(
            IllegalArgumentException.class,
            () -> dataTableRowService.importCsv("items", "title,amount\nApple,1\nBanana,abc\n", DEV_ENVIRONMENT_ID));

        verify(dataTableWebhookService, after(500).never()).listWebhooks(anyString(), anyLong());
    }

    @Test
    void testRolledBackUpdateRowsCallsNoWebhooks() {
        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "items", Map.of("title", "Apple"), DEV_ENVIRONMENT_ID);

        verify(dataTableWebhookService, timeout(5000)).listWebhooks("items", DEV_ENVIRONMENT_ID);

        clearInvocations(dataTableWebhookService);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            dataTableRowService.updateRows(
                "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Apple"))),
                Map.of("title", "sold"), DEV_ENVIRONMENT_ID);

            transactionStatus.setRollbackOnly();
        });

        verify(dataTableWebhookService, after(500).never()).listWebhooks(anyString(), anyLong());

        assertEquals(
            "Apple", Objects.requireNonNull(dataTableRowService.getRow("items", dataTableRow.id(), DEV_ENVIRONMENT_ID))
                .values()
                .get("title"));
    }

    private static boolean containsId(String body, long id) {
        return Pattern.compile("\"id\":" + id + "[,}]")
            .matcher(body)
            .find();
    }

    private List<Long> insertItems(String... titles) {
        List<Long> ids = new ArrayList<>();

        for (String title : titles) {
            DataTableRow dataTableRow = dataTableRowService.insertRow(
                "items", Map.of("title", title), DEV_ENVIRONMENT_ID);

            ids.add(dataTableRow.id());
        }

        return ids;
    }

    private void startWebhookServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        httpServer.createContext("/deleted", exchange -> receive(exchange, deletedBodies));
        httpServer.createContext("/updated", exchange -> receive(exchange, updatedBodies));
        httpServer.start();

        InetSocketAddress address = httpServer.getAddress();
        InetAddress inetAddress = address.getAddress();

        String baseUrl = "http://" + inetAddress.getHostAddress() + ":" + address.getPort();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
            List.of(
                new DataTableWebhookService.Webhook(
                    1, 1, baseUrl + "/deleted", DataTableWebhookType.RECORD_DELETED, DEV_ENVIRONMENT_ID),
                new DataTableWebhookService.Webhook(
                    2, 1, baseUrl + "/updated", DataTableWebhookType.RECORD_UPDATED, DEV_ENVIRONMENT_ID)));
    }

    private void receive(HttpExchange exchange, BlockingQueue<String> bodies) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            bodies.add(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }

        int status = failuresLeft.getAndUpdate(count -> Math.max(0, count - 1)) > 0 ? failureStatus.get() : 200;

        exchange.sendResponseHeaders(status, 0);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(new byte[0]);
        }
    }

    private static List<String> takeBodies(BlockingQueue<String> bodies, int count) throws InterruptedException {
        List<String> takenBodies = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            String body = bodies.poll(10, TimeUnit.SECONDS);

            assertTrue(body != null, "Expected webhook call " + (index + 1) + " of " + count);

            takenBodies.add(body);
        }

        return takenBodies;
    }
}
