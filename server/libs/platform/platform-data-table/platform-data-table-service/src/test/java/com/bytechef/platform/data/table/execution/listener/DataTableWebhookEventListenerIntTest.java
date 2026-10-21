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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
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
import com.bytechef.platform.data.table.execution.event.DataTableWebhookEvent;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.config.h2.H2DataSourceConfiguration;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
    private DataTableWebhookEventListener dataTableWebhookEventListener;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean(name = "webhookDeliveryExecutor")
    private AsyncTaskExecutor webhookDeliveryExecutor;

    @Autowired
    @Qualifier("workerExecutor")
    private AsyncTaskExecutor workerExecutor;

    private final CountDownLatch blockedReleaseLatch = new CountDownLatch(1);
    private final AtomicInteger blockedInFlightCount = new AtomicInteger();
    private final AtomicInteger blockedMaxInFlightCount = new AtomicInteger();
    private final BlockingQueue<String> blockedBodies = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> createdBodies = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> deletedBodies = new LinkedBlockingQueue<>();
    private final AtomicInteger failuresLeft = new AtomicInteger();
    private final AtomicInteger failureStatus = new AtomicInteger(400);
    private HttpServer httpServer;
    private final BlockingQueue<String> missingBodies = new LinkedBlockingQueue<>();
    private final BlockingQueue<Integer> scriptedStatuses = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> updatedBodies = new LinkedBlockingQueue<>();

    @AfterEach
    void afterEach() {
        blockedReleaseLatch.countDown();

        dataTableWebhookEventListener.clearSuspensions();
        dataTableWebhookEventListener.setClock(Clock.systemUTC());

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
    void testInsertRowOutsideATransactionCallsWebhooks() throws Exception {
        String baseUrl = startWebhookServer();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
            List.of(
                new DataTableWebhookService.Webhook(
                    3, 1, baseUrl + "/created", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "items", Map.of("title", "Apple"), DEV_ENVIRONMENT_ID);

        String body = takeBodies(createdBodies, 1).getFirst();

        assertTrue(body.contains("\"type\":\"RECORD_CREATED\""), body);
        assertTrue(containsId(body, dataTableRow.id()), body);
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

    @ParameterizedTest
    @ValueSource(ints = {
        301, 302, 307, 308, 404, 405, 410, 415
    })
    void testUnusableEndpointStopsTheRemainingPayloads(int status) throws Exception {
        startWebhookServer();

        insertItems("Apple", "Banana", "Cherry");

        failureStatus.set(status);
        failuresLeft.set(3);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        assertEquals(1, takeBodies(deletedBodies, 1).size());
        assertNull(
            deletedBodies.poll(
                DataTableWebhookEventListener.INITIAL_RETRY_INTERVAL.toMillis() + 1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void testUnusableEndpointSuspendsLaterDeliveriesUntilItsUrlChanges() throws Exception {
        String baseUrl = startWebhookServer();

        List<Long> ids = insertItems("Apple", "Banana", "Cherry");

        failureStatus.set(404);
        failuresLeft.set(1);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Apple"))),
            DEV_ENVIRONMENT_ID);

        assertTrue(containsId(takeBodies(deletedBodies, 1).getFirst(), ids.getFirst()));

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Banana"))),
            DEV_ENVIRONMENT_ID);

        assertNull(deletedBodies.poll(1000, TimeUnit.MILLISECONDS));

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
            List.of(
                new DataTableWebhookService.Webhook(
                    1, 1, baseUrl + "/deleted?moved=true", DataTableWebhookType.RECORD_DELETED,
                    DEV_ENVIRONMENT_ID)));

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Cherry"))),
            DEV_ENVIRONMENT_ID);

        assertTrue(containsId(takeBodies(deletedBodies, 1).getFirst(), ids.getLast()));
    }

    @Test
    void testSuspendedWebhookResumesOnceTheSuspensionEnds() throws Exception {
        String baseUrl = startWebhookServer();

        List<Long> ids = insertItems("Apple", "Banana", "Cherry");

        MutableClock mutableClock = new MutableClock(Instant.now());

        dataTableWebhookEventListener.setClock(mutableClock);

        failureStatus.set(404);
        failuresLeft.set(1);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Apple"))),
            DEV_ENVIRONMENT_ID);

        assertTrue(containsId(takeBodies(deletedBodies, 1).getFirst(), ids.getFirst()));

        DataTableWebhookService.Webhook deletedWebhook = new DataTableWebhookService.Webhook(
            1, 1, baseUrl + "/deleted", DataTableWebhookType.RECORD_DELETED, DEV_ENVIRONMENT_ID);

        await()
            .atMost(Duration.ofSeconds(5))
            .until(() -> dataTableWebhookEventListener.getSuspendedUntil(deletedWebhook) != null);

        mutableClock.advance(DataTableWebhookEventListener.SUSPENSION_DURATION.minusSeconds(1));

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Banana"))),
            DEV_ENVIRONMENT_ID);

        assertNull(deletedBodies.poll(1000, TimeUnit.MILLISECONDS));

        mutableClock.advance(Duration.ofSeconds(2));

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Cherry"))),
            DEV_ENVIRONMENT_ID);

        assertTrue(containsId(takeBodies(deletedBodies, 1).getFirst(), ids.getLast()));
    }

    @Test
    void testFailureMessagesShowOnlyTheHostOfUrls() {
        assertEquals(
            "I/O error on POST request for \"hooks.example.com\": Connection refused; see <url>",
            DataTableWebhookEventListener.redactUrls(
                "I/O error on POST request for \"https://hooks.example.com/t/secret%20token?key=abc%7C1\": " +
                    "Connection refused; see file:///etc/hosts"));
    }

    @ParameterizedTest
    @ValueSource(ints = {
        401, 403
    })
    void testRefusedAccessDoesNotStopTheRemainingPayloads(int status) throws Exception {
        startWebhookServer();

        insertItems("Apple", "Banana", "Cherry");

        failureStatus.set(status);
        failuresLeft.set(1);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        assertEquals(3, takeBodies(deletedBodies, 3).size());
        assertNull(deletedBodies.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testConsecutiveFailuresStopTheRemainingPayloads() throws Exception {
        startWebhookServer();

        int itemCount = DataTableWebhookEventListener.MAX_CONSECUTIVE_FAILURES + 2;

        insertItems(
            IntStream.range(0, itemCount)
                .mapToObj(index -> "Item " + index)
                .toArray(String[]::new));

        failureStatus.set(403);
        failuresLeft.set(itemCount);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        assertEquals(
            DataTableWebhookEventListener.MAX_CONSECUTIVE_FAILURES,
            takeBodies(deletedBodies, DataTableWebhookEventListener.MAX_CONSECUTIVE_FAILURES).size());
        assertNull(deletedBodies.poll(1000, TimeUnit.MILLISECONDS));

        insertItems("Later");

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Later"))),
            DEV_ENVIRONMENT_ID);

        assertNull(deletedBodies.poll(1000, TimeUnit.MILLISECONDS));
    }

    @ParameterizedTest
    @ValueSource(ints = {
        400, 409, 422
    })
    void testRejectedPayloadsDoNotSuspendTheEndpoint(int status) throws Exception {
        startWebhookServer();

        int itemCount = DataTableWebhookEventListener.MAX_CONSECUTIVE_FAILURES + 2;

        insertItems(
            IntStream.range(0, itemCount)
                .mapToObj(index -> "Item " + index)
                .toArray(String[]::new));

        failureStatus.set(status);
        failuresLeft.set(itemCount);

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        assertEquals(itemCount, takeBodies(deletedBodies, itemCount).size());

        List<Long> ids = insertItems("Later");

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.EQUALS, "Later"))),
            DEV_ENVIRONMENT_ID);

        assertTrue(containsId(takeBodies(deletedBodies, 1).getFirst(), ids.getFirst()));
    }

    @Test
    void testSuccessResetsTheConsecutiveFailures() throws Exception {
        startWebhookServer();

        int failureRunCount = DataTableWebhookEventListener.MAX_CONSECUTIVE_FAILURES - 1;
        int itemCount = failureRunCount * 2 + 1;

        insertItems(
            IntStream.range(0, itemCount)
                .mapToObj(index -> "Item " + index)
                .toArray(String[]::new));

        for (int index = 0; index < itemCount; index++) {
            scriptedStatuses.add(index == failureRunCount ? 200 : 403);
        }

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        assertEquals(itemCount, takeBodies(deletedBodies, itemCount).size());
        assertNull(deletedBodies.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testConsecutiveFailuresOfSingleRowDeliveriesSuspendTheEndpoint() throws Exception {
        String baseUrl = startWebhookServer();

        List<Long> ids = insertItems(
            IntStream.rangeClosed(0, DataTableWebhookEventListener.MAX_CONSECUTIVE_FAILURES)
                .mapToObj(index -> "Item " + index)
                .toArray(String[]::new));

        failureStatus.set(403);
        failuresLeft.set(DataTableWebhookEventListener.MAX_CONSECUTIVE_FAILURES);

        for (int index = 0; index < DataTableWebhookEventListener.MAX_CONSECUTIVE_FAILURES; index++) {
            dataTableRowService.deleteRow("items", ids.get(index), DEV_ENVIRONMENT_ID);

            takeBodies(deletedBodies, 1);
        }

        DataTableWebhookService.Webhook deletedWebhook = new DataTableWebhookService.Webhook(
            1, 1, baseUrl + "/deleted", DataTableWebhookType.RECORD_DELETED, DEV_ENVIRONMENT_ID);

        await()
            .atMost(Duration.ofSeconds(5))
            .until(() -> dataTableWebhookEventListener.getSuspendedUntil(deletedWebhook) != null);

        dataTableRowService.deleteRow("items", ids.getLast(), DEV_ENVIRONMENT_ID);

        assertNull(deletedBodies.poll(1000, TimeUnit.MILLISECONDS));
    }

    @Test
    void testSuspensionDoesNotSuspendTheSameWebhookIdOfAnotherTenant() throws Exception {
        String baseUrl = startWebhookServer();

        List<Long> ids = insertItems("Apple");

        failureStatus.set(404);
        failuresLeft.set(1);

        dataTableRowService.deleteRow("items", ids.getFirst(), DEV_ENVIRONMENT_ID);

        takeBodies(deletedBodies, 1);

        DataTableWebhookService.Webhook deletedWebhook = new DataTableWebhookService.Webhook(
            1, 1, baseUrl + "/deleted", DataTableWebhookType.RECORD_DELETED, DEV_ENVIRONMENT_ID);

        await()
            .atMost(Duration.ofSeconds(5))
            .until(() -> dataTableWebhookEventListener.getSuspendedUntil(deletedWebhook) != null);

        TenantContext.runWithTenantId("other", () -> {
            assertNull(dataTableWebhookEventListener.getSuspendedUntil(deletedWebhook));

            dataTableWebhookEventListener.onDataTableWebhookEvent(
                new DataTableWebhookEvent(
                    "items", DataTableWebhookType.RECORD_DELETED, Map.of("id", 99L, "values", Map.of()),
                    DEV_ENVIRONMENT_ID));
        });

        assertTrue(containsId(takeBodies(deletedBodies, 1).getFirst(), 99L));
    }

    @Test
    void testSlowWebhookDoesNotDelayAnotherWebhook() throws Exception {
        String baseUrl = startWebhookServer();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
            List.of(
                new DataTableWebhookService.Webhook(
                    3, 1, baseUrl + "/blocked", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID),
                new DataTableWebhookService.Webhook(
                    4, 1, baseUrl + "/created", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

        insertItems("Apple");

        String createdBody = createdBodies.poll(5, TimeUnit.SECONDS);

        assertNotNull(createdBody);
        assertEquals(0, blockedBodies.size());

        blockedReleaseLatch.countDown();

        takeBodies(blockedBodies, 1);
    }

    @Test
    void testUnusableWebhookDoesNotStopAnotherWebhook() throws Exception {
        String baseUrl = startWebhookServer();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
            List.of(
                new DataTableWebhookService.Webhook(
                    1, 1, baseUrl + "/missing", DataTableWebhookType.RECORD_DELETED, DEV_ENVIRONMENT_ID),
                new DataTableWebhookService.Webhook(
                    2, 1, "not a url", DataTableWebhookType.RECORD_DELETED, DEV_ENVIRONMENT_ID),
                new DataTableWebhookService.Webhook(
                    3, 1, baseUrl + "/deleted", DataTableWebhookType.RECORD_DELETED, DEV_ENVIRONMENT_ID)));

        insertItems("Apple", "Banana", "Cherry");

        dataTableRowService.deleteRows(
            "items", DataTableRowConditions.of(List.of(new DataTableRowFilter("title", Operation.NOT_EMPTY, null))),
            DEV_ENVIRONMENT_ID);

        assertEquals(3, takeBodies(deletedBodies, 3).size());
        assertEquals(1, takeBodies(missingBodies, 1).size());
        assertNull(deletedBodies.poll(500, TimeUnit.MILLISECONDS));
        assertNull(missingBodies.poll(500, TimeUnit.MILLISECONDS));
    }

    @ParameterizedTest
    @ValueSource(ints = {
        429, 503
    })
    void testServerErrorAndTooManyRequestsAreRetried(int status) throws Exception {
        startWebhookServer();

        List<Long> ids = insertItems("Apple");

        failureStatus.set(status);
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
    void testRowChangeOnAWorkerThreadDoesNotWaitForAnotherWorkerPermit() throws Exception {
        String baseUrl = startWebhookServer();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
            List.of(
                new DataTableWebhookService.Webhook(
                    3, 1, baseUrl + "/created", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

        CompletableFuture<DataTableRow> insertedDataTableRow = new CompletableFuture<>();

        workerExecutor.execute(() -> insertedDataTableRow.complete(
            dataTableRowService.insertRow("items", Map.of("title", "Apple"), DEV_ENVIRONMENT_ID)));

        assertNotNull(insertedDataTableRow.get(5, TimeUnit.SECONDS));

        List<String> bodies = takeBodies(createdBodies, 1);

        assertTrue(bodies.getFirst()
            .contains("Apple"), bodies.getFirst());
    }

    @Test
    void testWebhooksAreListedOnTheChangingThreadAndCalledOnTheWebhookDeliveryExecutor() throws Exception {
        CompletableFuture<Thread> listingThread = new CompletableFuture<>();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenAnswer(invocation -> {
            listingThread.complete(Thread.currentThread());

            return List.of();
        });

        dataTableRowService.insertRow("items", Map.of("title", "Apple"), DEV_ENVIRONMENT_ID);

        assertEquals(Thread.currentThread(), listingThread.get(5, TimeUnit.SECONDS));

        verify(webhookDeliveryExecutor, after(500).never()).execute(any(Runnable.class));

        String baseUrl = startWebhookServer();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
            List.of(
                new DataTableWebhookService.Webhook(
                    3, 1, baseUrl + "/created", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

        dataTableRowService.insertRow("items", Map.of("title", "Banana"), DEV_ENVIRONMENT_ID);

        takeBodies(createdBodies, 1);

        verify(webhookDeliveryExecutor).execute(any(Runnable.class));
    }

    @Test
    void testImportCsvListsWebhooksOnceAndPostsOnePayloadPerImportedRow() throws Exception {
        String baseUrl = startWebhookServer();

        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
            List.of(
                new DataTableWebhookService.Webhook(
                    3, 1, baseUrl + "/created", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

        dataTableRowService.importCsv("items", "title,amount\nApple,1\nBanana,2\nCherry,3\n", DEV_ENVIRONMENT_ID);

        List<String> bodies = takeBodies(createdBodies, 3);

        assertTrue(bodies.get(0)
            .contains("Apple"), bodies.get(0));
        assertTrue(bodies.get(2)
            .contains("Cherry"), bodies.get(2));

        verify(dataTableWebhookService, times(1)).listWebhooks(anyString(), anyLong());
        verify(webhookDeliveryExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    void testFailingWebhookLookupDoesNotFailTheChange() {
        when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenThrow(
            new IllegalStateException("database unavailable"));

        DataTableRow dataTableRow = dataTableRowService.insertRow(
            "items", Map.of("title", "Apple"), DEV_ENVIRONMENT_ID);

        assertNotNull(dataTableRowService.getRow("items", dataTableRow.id(), DEV_ENVIRONMENT_ID));

        verify(webhookDeliveryExecutor, never()).execute(any(Runnable.class));
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

    @ParameterizedTest
    @ValueSource(ints = {
        0, -1
    })
    void testLimitsBelowOneAreRejected(int limit) {
        ApplicationProperties concurrencyLimitProperties = createApplicationProperties(limit, 1);

        IllegalArgumentException concurrencyLimitException = assertThrows(
            IllegalArgumentException.class,
            () -> new DataTableWebhookEventListener(
                dataTableWebhookService, webhookDeliveryExecutor, concurrencyLimitProperties));

        assertEquals(
            "bytechef.webhook-delivery.concurrency-limit must be at least 1, was " + limit,
            concurrencyLimitException.getMessage());

        ApplicationProperties maxPendingDeliveriesProperties = createApplicationProperties(1, limit);

        IllegalArgumentException maxPendingDeliveriesException = assertThrows(
            IllegalArgumentException.class,
            () -> new DataTableWebhookEventListener(
                dataTableWebhookService, webhookDeliveryExecutor, maxPendingDeliveriesProperties));

        assertEquals(
            "bytechef.webhook-delivery.max-pending-deliveries must be at least 1, was " + limit,
            maxPendingDeliveriesException.getMessage());
    }

    @Nested
    @TestPropertySource(properties = "bytechef.webhook-delivery.concurrency-limit=1")
    class DeliveryConcurrencyLimitOfOne {

        @Test
        void testBusyWebhookDoesNotBlockTheThreadThatChangesRowsAndPostsOneAtATime() throws Exception {
            String baseUrl = startWebhookServer();

            when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
                List.of(
                    new DataTableWebhookService.Webhook(
                        3, 1, baseUrl + "/blocked", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

            assertTimeoutPreemptively(
                Duration.ofSeconds(5),
                () -> insertItems("Apple", "Banana", "Cherry"));

            blockedReleaseLatch.countDown();

            takeBodies(blockedBodies, 3);

            assertEquals(1, blockedMaxInFlightCount.get());
        }

        @Test
        void testSuspensionStopsDeliveriesWaitingForAPermit() throws Exception {
            String baseUrl = startWebhookServer();

            when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
                List.of(
                    new DataTableWebhookService.Webhook(
                        3, 1, baseUrl + "/blocked", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

            failureStatus.set(404);
            failuresLeft.set(2);

            insertItems("Apple");

            await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> blockedInFlightCount.get() == 1);

            insertItems("Banana");

            verify(webhookDeliveryExecutor, timeout(5000).times(2)).execute(any(Runnable.class));

            Thread.sleep(200);

            blockedReleaseLatch.countDown();

            List<String> bodies = takeBodies(blockedBodies, 1);

            assertTrue(bodies.getFirst()
                .contains("Apple"), bodies.getFirst());
            assertNull(blockedBodies.poll(1000, TimeUnit.MILLISECONDS));
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "bytechef.webhook-delivery.concurrency-limit=2", "bytechef.webhook-delivery.max-pending-deliveries=1"
        })
    class MaxPendingDeliveriesOfOnePerWebhook {

        @Test
        void testPendingDeliveryOfOneWebhookDoesNotDropAnotherWebhook() throws Exception {
            String baseUrl = startWebhookServer();

            when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
                List.of(
                    new DataTableWebhookService.Webhook(
                        3, 1, baseUrl + "/blocked", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID),
                    new DataTableWebhookService.Webhook(
                        4, 1, baseUrl + "/created", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

            insertItems("Apple");

            List<String> createdAppleBodies = takeBodies(createdBodies, 1);

            assertTrue(createdAppleBodies.getFirst()
                .contains("Apple"), createdAppleBodies.getFirst());

            blockedReleaseLatch.countDown();

            List<String> blockedAppleBodies = takeBodies(blockedBodies, 1);

            assertTrue(blockedAppleBodies.getFirst()
                .contains("Apple"), blockedAppleBodies.getFirst());
        }

        @Test
        void testPendingDeliveryDoesNotDropTheSameWebhookIdOfAnotherTenant() throws Exception {
            String baseUrl = startWebhookServer();

            when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
                List.of(
                    new DataTableWebhookService.Webhook(
                        3, 1, baseUrl + "/blocked", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

            insertItems("Apple");

            await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> blockedInFlightCount.get() == 1);

            when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
                List.of(
                    new DataTableWebhookService.Webhook(
                        3, 1, baseUrl + "/created", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

            TenantContext.runWithTenantId(
                "other", () -> dataTableWebhookEventListener.onDataTableWebhookEvent(
                    new DataTableWebhookEvent(
                        "items", DataTableWebhookType.RECORD_CREATED, Map.of("id", 99L, "values", Map.of()),
                        DEV_ENVIRONMENT_ID)));

            assertTrue(containsId(takeBodies(createdBodies, 1).getFirst(), 99L));
        }
    }

    @Nested
    @TestPropertySource(
        properties = {
            "bytechef.webhook-delivery.concurrency-limit=1", "bytechef.webhook-delivery.max-pending-deliveries=1"
        })
    class MaxPendingDeliveriesOfOne {

        @Test
        void testDeliveriesBeyondTheLimitAreDropped() throws Exception {
            String baseUrl = startWebhookServer();

            when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
                List.of(
                    new DataTableWebhookService.Webhook(
                        3, 1, baseUrl + "/blocked", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

            insertItems("Apple");

            verify(webhookDeliveryExecutor, timeout(5000)).execute(any(Runnable.class));

            insertItems("Banana", "Cherry");

            blockedReleaseLatch.countDown();

            List<String> bodies = takeBodies(blockedBodies, 1);

            assertTrue(bodies.getFirst()
                .contains("Apple"), bodies.getFirst());
            assertNull(blockedBodies.poll(1000, TimeUnit.MILLISECONDS));
            verify(webhookDeliveryExecutor, times(1)).execute(any(Runnable.class));
        }

        @Test
        void testFinishedDeliveryFreesItsPendingSlot() throws Exception {
            String baseUrl = startWebhookServer();

            when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
                List.of(
                    new DataTableWebhookService.Webhook(
                        3, 1, baseUrl + "/blocked", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

            insertItems("Apple");

            blockedReleaseLatch.countDown();

            List<String> bodies = takeBodies(blockedBodies, 1);

            assertTrue(bodies.getFirst()
                .contains("Apple"), bodies.getFirst());

            await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> {
                    insertItems("Banana");

                    String body = blockedBodies.poll(200, TimeUnit.MILLISECONDS);

                    return body != null && body.contains("Banana");
                });
        }

        @Test
        void testRejectedDeliveryFreesItsPendingSlot() throws Exception {
            String baseUrl = startWebhookServer();

            when(dataTableWebhookService.listWebhooks("items", DEV_ENVIRONMENT_ID)).thenReturn(
                List.of(
                    new DataTableWebhookService.Webhook(
                        3, 1, baseUrl + "/created", DataTableWebhookType.RECORD_CREATED, DEV_ENVIRONMENT_ID)));

            doThrow(new TaskRejectedException("rejected"))
                .doCallRealMethod()
                .when(webhookDeliveryExecutor)
                .execute(any(Runnable.class));

            insertItems("Apple", "Banana");

            List<String> bodies = takeBodies(createdBodies, 1);

            assertTrue(bodies.getFirst()
                .contains("Banana"), bodies.getFirst());
            assertNull(createdBodies.poll(500, TimeUnit.MILLISECONDS));
        }
    }

    private static ApplicationProperties createApplicationProperties(int concurrencyLimit, int maxPendingDeliveries) {
        ApplicationProperties.WebhookDelivery webhookDelivery = new ApplicationProperties.WebhookDelivery();

        webhookDelivery.setConcurrencyLimit(concurrencyLimit);
        webhookDelivery.setMaxPendingDeliveries(maxPendingDeliveries);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setWebhookDelivery(webhookDelivery);

        return applicationProperties;
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

    private String startWebhookServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        httpServer.createContext("/blocked", this::receiveBlocked);
        httpServer.createContext("/created", exchange -> receive(exchange, createdBodies));
        httpServer.createContext("/deleted", exchange -> receive(exchange, deletedBodies));
        httpServer.createContext("/missing", this::receiveMissing);
        httpServer.createContext("/updated", exchange -> receive(exchange, updatedBodies));
        httpServer.setExecutor(Executors.newCachedThreadPool());
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

        return baseUrl;
    }

    private void receive(HttpExchange exchange, BlockingQueue<String> bodies) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            bodies.add(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }

        Integer scriptedStatus = scriptedStatuses.poll();

        int status;

        if (scriptedStatus == null) {
            status = failuresLeft.getAndUpdate(count -> Math.max(0, count - 1)) > 0 ? failureStatus.get() : 200;
        } else {
            status = scriptedStatus;
        }

        exchange.sendResponseHeaders(status, 0);

        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(new byte[0]);
        }
    }

    private void receiveMissing(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            missingBodies.add(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
        }

        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private void receiveBlocked(HttpExchange exchange) throws IOException {
        int inFlightCount = blockedInFlightCount.incrementAndGet();

        blockedMaxInFlightCount.accumulateAndGet(inFlightCount, Math::max);

        try {
            if (!blockedReleaseLatch.await(20, TimeUnit.SECONDS)) {
                throw new IOException("Blocked webhook was not released");
            }

            receive(exchange, blockedBodies);
        } catch (InterruptedException interruptedException) {
            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();

            throw new IOException(interruptedException);
        } finally {
            blockedInFlightCount.decrementAndGet();
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

    private static final class MutableClock extends Clock {

        private volatile Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
