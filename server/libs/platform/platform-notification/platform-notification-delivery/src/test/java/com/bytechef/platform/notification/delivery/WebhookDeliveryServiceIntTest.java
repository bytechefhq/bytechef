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

package com.bytechef.platform.notification.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.commons.util.UrlValidationException;
import com.bytechef.commons.util.UrlValidatorUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.PayloadResult;
import com.bytechef.platform.notification.delivery.WebhookDeliveryOutcome.Status;
import com.bytechef.platform.notification.delivery.config.WebhookDeliveryIntTestConfiguration;
import com.bytechef.tenant.TenantContext;
import com.sun.net.httpserver.HttpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.context.ContextRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@Timeout(60)
@SpringBootTest(
    classes = WebhookDeliveryIntTestConfiguration.class,
    properties = {
        "bytechef.webhook-delivery.concurrency-limit=2", "bytechef.webhook-delivery.max-pending-deliveries=2"
    })
class WebhookDeliveryServiceIntTest {

    private static final String SOURCE = "test";
    private static final String TENANT_ID = "tenant";

    private final Semaphore arrivals = new Semaphore(0);
    private final Set<String> blockOncePaths = ConcurrentHashMap.newKeySet();
    private final AtomicInteger inFlightCount = new AtomicInteger();
    private final MutableClock mutableClock = new MutableClock(Instant.parse("2026-11-02T10:00:00Z"));
    private final AtomicInteger peakInFlightCount = new AtomicInteger();
    private final Map<String, List<String>> receivedBodies = new ConcurrentHashMap<>();
    private final CountDownLatch releaseLatch = new CountDownLatch(1);
    private final AtomicInteger requestCount = new AtomicInteger();
    private final Map<String, String> responseBodies = new ConcurrentHashMap<>();
    private final AtomicInteger startedTaskCount = new AtomicInteger();
    private final Map<String, Deque<Integer>> statusSequences = new ConcurrentHashMap<>();

    private ExecutorService httpServerExecutor;
    private HttpServer httpServer;
    private SimpleAsyncTaskExecutor webhookDeliveryExecutor;

    @Autowired
    private SimpleMeterRegistry meterRegistry;

    @MockitoSpyBean
    private WebhookNotificationClient webhookNotificationClient;

    @Autowired
    private WebhookDeliveryService webhookDeliveryService;

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        httpServerExecutor = Executors.newCachedThreadPool();

        httpServer.setExecutor(httpServerExecutor);
        httpServer.createContext("/", httpExchange -> {
            String path = httpExchange.getRequestURI()
                .getPath();
            byte[] requestBody = httpExchange.getRequestBody()
                .readAllBytes();

            receivedBodies.computeIfAbsent(path, key -> new CopyOnWriteArrayList<>())
                .add(new String(requestBody, StandardCharsets.UTF_8));
            requestCount.incrementAndGet();
            peakInFlightCount.accumulateAndGet(inFlightCount.incrementAndGet(), Math::max);
            arrivals.release();

            int status = 200;

            try {
                if (blockOncePaths.remove(path) && !releaseLatch.await(10, TimeUnit.SECONDS)) {
                    status = 599;
                }

                Deque<Integer> statusSequence = statusSequences.get(path);
                Integer nextStatus = statusSequence == null ? null : statusSequence.poll();

                if (nextStatus != null && status == 200) {
                    status = nextStatus;
                }
            } catch (InterruptedException interruptedException) {
                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();

                status = 599;
            } finally {
                inFlightCount.decrementAndGet();
            }

            String responseBody = responseBodies.get(path);

            if (responseBody == null) {
                httpExchange.sendResponseHeaders(status, -1);
            } else {
                byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);

                httpExchange.sendResponseHeaders(status, responseBytes.length);

                httpExchange.getResponseBody()
                    .write(responseBytes);
            }

            httpExchange.close();
        });
        httpServer.start();

        meterRegistry.clear();

        webhookDeliveryExecutor = createExecutor();

        webhookDeliveryService.clearEndpointState();
        webhookDeliveryService.setClock(mutableClock);
        webhookDeliveryService.setUrlValidator(url -> UrlValidatorUtils.validate(url, Set.of()));
    }

    @AfterEach
    void afterEach() {
        releaseLatch.countDown();
        webhookDeliveryExecutor.close();
        httpServer.stop(0);
        httpServerExecutor.shutdownNow();
    }

    @Test
    void testSendsEachPayloadInOrderToTheEndpoint() throws Exception {
        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/order", 3)));

        assertThat(statuses(outcome)).containsExactly(Status.SENT, Status.SENT, Status.SENT);
        assertThat(receivedBodies.get("/order")).containsExactly(
            "{\"index\":0}", "{\"index\":1}", "{\"index\":2}");
    }

    @Test
    void testRetriesOnlyIoFiveHundredsAndTooManyRequests() throws Exception {
        WebhookRetry webhookRetry = new WebhookRetry(3, Duration.ofMillis(1), 1.0);

        respondWith("/unavailable", 503, 200);
        respondWith("/throttled", 429, 200);
        respondWith("/missing", 404, 200);
        respondWith("/bad", 400, 200);

        assertThat(statuses(await(webhookDeliveryService.submit(delivery("/unavailable", 1, webhookRetry)))))
            .containsExactly(Status.SENT);
        assertThat(statuses(await(webhookDeliveryService.submit(delivery("/throttled", 1, webhookRetry)))))
            .containsExactly(Status.SENT);
        assertThat(statuses(await(webhookDeliveryService.submit(delivery("/missing", 1, webhookRetry)))))
            .containsExactly(Status.FAILED);
        assertThat(statuses(await(webhookDeliveryService.submit(delivery("/bad", 1, webhookRetry)))))
            .containsExactly(Status.FAILED);

        assertThat(receivedBodies.get("/unavailable")).hasSize(2);
        assertThat(receivedBodies.get("/throttled")).hasSize(2);
        assertThat(receivedBodies.get("/missing")).hasSize(1);
        assertThat(receivedBodies.get("/bad")).hasSize(1);
    }

    @Test
    void testRetriesConnectionFailures() throws Exception {
        WebhookDelivery webhookDelivery = WebhookDelivery.builder(SOURCE, closedPortUrl())
            .description("test delivery")
            .tenantId(TENANT_ID)
            .validateUrl(false)
            .retry(new WebhookRetry(2, Duration.ofMillis(1), 1.0))
            .payload("payload-0", "{\"index\":0}")
            .build();

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(webhookDelivery));

        assertThat(statuses(outcome)).containsExactly(Status.FAILED);

        verify(webhookNotificationClient, times(3)).post(any(), any());
    }

    @Test
    void testSendTestReturnsStatusAndBody() {
        respondWith("/test", 202);
        responseBodies.put("/test", "ok");

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(delivery("/test", 1));

        assertThat(webhookTestResult.success()).isTrue();
        assertThat(webhookTestResult.statusCode()).isEqualTo(202);
        assertThat(webhookTestResult.responseBody()).isEqualTo("ok");
        assertThat(webhookTestResult.error()).isNull();
    }

    @Test
    void testSendTestCapsResponseBody() {
        respondWith("/test", 500);
        responseBodies.put("/test", "x".repeat(5000));

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(delivery("/test", 1));

        assertThat(webhookTestResult.success()).isFalse();
        assertThat(webhookTestResult.statusCode()).isEqualTo(500);
        assertThat(webhookTestResult.responseBody()).hasSize(1001)
            .endsWith("…");
        assertThat(webhookTestResult.error()).doesNotContain("xxx");
    }

    @Test
    void testSendTestRejectsDisallowedUrl() {
        WebhookDelivery webhookDelivery = WebhookDelivery.builder(SOURCE, url("/"))
            .description("test delivery")
            .tenantId(TENANT_ID)
            .payload("payload-0", "{}")
            .build();

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(webhookDelivery);

        assertThat(webhookTestResult.success()).isFalse();
        assertThat(webhookTestResult.statusCode()).isNull();
        assertThat(webhookTestResult.error()).isNotNull();
        assertThat(requestCount).hasValue(0);
    }

    @Test
    void testAllowedHostsBypassThePrivateAddressCheck() throws Exception {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.WebhookDelivery webhookDeliveryProperties = applicationProperties.getWebhookDelivery();

        webhookDeliveryProperties.setAllowedHosts(List.of("LOCALHOST"));
        webhookDeliveryProperties.setConcurrencyLimit(2);
        webhookDeliveryProperties.setMaxPendingDeliveries(2);

        WebhookDeliveryService allowingWebhookDeliveryService = new WebhookDeliveryService(
            webhookNotificationClient, webhookDeliveryExecutor, applicationProperties,
            meterRegistryProvider(meterRegistry));

        WebhookDelivery validatedWebhookDelivery = WebhookDelivery.builder(SOURCE, url("/allowed"))
            .description("test delivery")
            .tenantId(TENANT_ID)
            .validateUrl(true)
            .payload("payload-0", "{\"index\":0}")
            .build();

        WebhookTestResult webhookTestResult = allowingWebhookDeliveryService.sendTest(validatedWebhookDelivery);

        assertThat(webhookTestResult.success()).isTrue();
        assertThat(webhookTestResult.statusCode()).isEqualTo(200);
        assertThat(statuses(await(allowingWebhookDeliveryService.submit(validatedWebhookDelivery))))
            .containsExactly(Status.SENT);
        assertThat(receivedBodies.get("/allowed")).hasSize(2);

        assertThat(webhookDeliveryService.sendTest(validatedWebhookDelivery)
            .success()).isFalse();
    }

    @Test
    void testSendTestReportsConnectionError() throws Exception {
        WebhookDelivery webhookDelivery = WebhookDelivery.builder(SOURCE, closedPortUrl())
            .description("test delivery")
            .tenantId(TENANT_ID)
            .validateUrl(false)
            .payload("payload-0", "{}")
            .build();

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(webhookDelivery);

        assertThat(webhookTestResult.success()).isFalse();
        assertThat(webhookTestResult.statusCode()).isNull();
        assertThat(webhookTestResult.error()).contains("localhost")
            .doesNotContain("SECRET");
    }

    @Test
    void testSendTestDoesNotRetry() {
        respondWith("/test", 503, 503, 503);

        WebhookDelivery webhookDelivery = WebhookDelivery.builder(SOURCE, url("/test"))
            .description("test delivery")
            .tenantId(TENANT_ID)
            .validateUrl(false)
            .retry(new WebhookRetry(3, Duration.ofMillis(1), 1.0))
            .payload("payload-0", "{}")
            .build();

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(webhookDelivery);

        assertThat(webhookTestResult.success()).isFalse();
        assertThat(webhookTestResult.statusCode()).isEqualTo(503);
        assertThat(requestCount).hasValue(1);
    }

    @Test
    void testSendTestDoesNotAffectEndpointState() throws Exception {
        int testCount = EndpointState.MAX_CONSECUTIVE_FAILURES + 1;

        Integer[] failureStatuses = new Integer[testCount];

        Arrays.fill(failureStatuses, 500);

        respondWith("/test", failureStatuses);

        for (int index = 0; index < testCount; index++) {
            webhookDeliveryService.sendTest(delivery("/test", 1));
        }

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/test", 1)));

        assertThat(outcome.anySent()).isTrue();
        assertThat(requestCount).hasValue(testCount + 1);
    }

    @Test
    void testSendTestRecordsNoMetrics() throws Exception {
        respondWith("/test", 500, 500);

        int meterCountBefore = meterRegistry.getMeters()
            .size();

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(delivery("/test", 1));

        assertThat(webhookTestResult.success()).isFalse();
        assertThat(meterRegistry.getMeters()).hasSize(meterCountBefore);
        assertThat(unsentCount("failed")).isZero();

        await(webhookDeliveryService.submit(delivery("/test", 1)));

        assertThat(unsentCount("failed")).isEqualTo(1);
    }

    @Test
    void testSendTestCapsMultiByteBody() {
        respondWith("/test", 200);
        responseBodies.put("/test", "é".repeat(3000));

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(delivery("/test", 1));

        assertThat(webhookTestResult.responseBody()).hasSizeLessThanOrEqualTo(1001)
            .endsWith("…")
            .doesNotContain("\uFFFD");
    }

    @Test
    void testSendTestMapsRuntimeExceptionWithoutLeakingMessage() {
        doThrow(new IllegalArgumentException("secret-value")).when(webhookNotificationClient)
            .post(any(), any());

        WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(delivery("/test", 1));

        assertThat(webhookTestResult.success()).isFalse();
        assertThat(webhookTestResult.error()).doesNotContain("secret-value")
            .contains("IllegalArgumentException");
    }

    @Test
    void testSendTestRestoresInterruptFlag() {
        doThrow(new IllegalStateException(new InterruptedException())).when(webhookNotificationClient)
            .post(any(), any());

        try {
            WebhookTestResult webhookTestResult = webhookDeliveryService.sendTest(delivery("/test", 1));

            assertThat(webhookTestResult.success()).isFalse();
            assertThat(Thread.interrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void testNoRetryMakesOneAttempt() throws Exception {
        respondWith("/once", 500, 200);

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/once", 1)));

        assertThat(statuses(outcome)).containsExactly(Status.FAILED);
        assertThat(receivedBodies.get("/once")).hasSize(1);
    }

    @Test
    void testRedirectPausesTheEndpoint() throws Exception {
        respondWith("/redirect", 302);

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/redirect", 2)));

        assertThat(statuses(outcome)).containsExactly(Status.FAILED, Status.SKIPPED);
        assertThat(outcome.suspendedUntil()).isEqualTo(mutableClock.instant()
            .plus(Duration.ofMinutes(5)));
        assertThat(suspensionCount()).isEqualTo(1.0);
    }

    @ParameterizedTest
    @ValueSource(ints = {
        404, 405, 410, 415
    })
    void testNotFoundGoneMethodNotAllowedAndUnsupportedMediaTypePause(int status) throws Exception {
        respondWith("/unusable", status);

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/unusable", 2)));

        assertThat(statuses(outcome)).containsExactly(Status.FAILED, Status.SKIPPED);
        assertThat(outcome.suspendedUntil()).isNotNull();
        assertThat(receivedBodies.get("/unusable")).hasSize(1);
    }

    @Test
    void testTenConsecutiveFailuresPause() throws Exception {
        respondWith("/failing", repeat(500, 11));

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/failing", 11)));

        assertThat(outcome.count(Status.FAILED)).isEqualTo(10);
        assertThat(outcome.payloadResults()
            .get(10)
            .status()).isEqualTo(Status.SKIPPED);
        assertThat(outcome.suspendedUntil()).isNotNull();
        assertThat(receivedBodies.get("/failing")).hasSize(10);
    }

    @Test
    void testPayloadRejectedResetsTheConsecutiveCount() throws Exception {
        List<Integer> statuses = new ArrayList<>(Arrays.asList(repeat(500, 9)));

        statuses.add(422);
        statuses.add(500);

        respondWith("/rejecting", statuses.toArray(Integer[]::new));

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/rejecting", 11)));

        assertThat(outcome.count(Status.FAILED)).isEqualTo(11);
        assertThat(outcome.suspendedUntil()).isNull();
        assertThat(receivedBodies.get("/rejecting")).hasSize(11);
    }

    @Test
    void testPausedEndpointSkipsWithoutAnAttemptAndResumesAfterFiveMinutes() throws Exception {
        respondWith("/paused", 404);

        await(webhookDeliveryService.submit(delivery("/paused", 1)));

        WebhookDeliveryOutcome pausedOutcome = await(webhookDeliveryService.submit(delivery("/paused", 2)));

        assertThat(statuses(pausedOutcome)).containsExactly(Status.SKIPPED, Status.SKIPPED);
        assertThat(pausedOutcome.suspendedUntil()).isNotNull();
        assertThat(receivedBodies.get("/paused")).hasSize(1);

        mutableClock.advance(Duration.ofMinutes(5)
            .plusSeconds(1));

        WebhookDeliveryOutcome resumedOutcome = await(webhookDeliveryService.submit(delivery("/paused", 1)));

        assertThat(statuses(resumedOutcome)).containsExactly(Status.SENT);
        assertThat(receivedBodies.get("/paused")).hasSize(2);
    }

    @Test
    void testEndpointsAreKeyedByTenantAndUrl() throws Exception {
        respondWith("/shared", 404);

        await(webhookDeliveryService.submit(delivery("a", "/shared", 1, WebhookRetry.none())));

        WebhookDeliveryOutcome outcome = await(
            webhookDeliveryService.submit(delivery("b", "/shared", 1, WebhookRetry.none())));

        assertThat(statuses(outcome)).containsExactly(Status.SENT);
    }

    @Test
    void testOneDeliveryAtATimePerEndpoint() throws Exception {
        blockOncePaths.add("/serial");

        CompletableFuture<WebhookDeliveryOutcome> firstFuture = webhookDeliveryService.submit(
            delivery("/serial", "first"));

        assertThat(arrivals.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<WebhookDeliveryOutcome> secondFuture = webhookDeliveryService.submit(
            delivery("/serial", "second"));

        assertThat(arrivals.tryAcquire(200, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(secondFuture).isNotDone();

        releaseLatch.countDown();

        assertThat(statuses(await(firstFuture))).containsExactly(Status.SENT);
        assertThat(statuses(await(secondFuture))).containsExactly(Status.SENT);
        assertThat(receivedBodies.get("/serial")).containsExactly("{\"name\":\"first\"}", "{\"name\":\"second\"}");
    }

    @Test
    void testDeliveriesToOneEndpointPostInSubmissionOrder() throws Exception {
        webhookDeliveryExecutor.setTaskDecorator(this::startAfterAVaryingDelay);

        for (int iteration = 0; iteration < 50; iteration++) {
            CountDownLatch firstPostLatch = new CountDownLatch(1);
            List<String> postedLabels = new CopyOnWriteArrayList<>();
            CountDownLatch releasePostLatch = new CountDownLatch(1);

            doAnswer(invocation -> {
                WebhookPayload webhookPayload = invocation.getArgument(1);

                postedLabels.add(webhookPayload.label());

                if (postedLabels.size() == 1) {
                    firstPostLatch.countDown();

                    awaitRelease(releasePostLatch);
                }

                return new WebhookNotificationClient.WebhookResponse(200, null);
            }).when(webhookNotificationClient)
                .post(any(), any());

            WebhookDeliveryService orderRecordingWebhookDeliveryService = createService(
                2, 20, meterRegistryProvider(null));

            List<CompletableFuture<WebhookDeliveryOutcome>> futures = new ArrayList<>();
            List<String> submittedLabels = new ArrayList<>();

            for (int index = 0; index < 10; index++) {
                String label = "delivery-" + index;

                submittedLabels.add(label);
                futures.add(orderRecordingWebhookDeliveryService.submit(delivery("/ordered", label)));

                if (index == 0) {
                    assertThat(firstPostLatch.await(10, TimeUnit.SECONDS)).isTrue();
                }
            }

            releasePostLatch.countDown();

            for (CompletableFuture<WebhookDeliveryOutcome> future : futures) {
                assertThat(statuses(await(future))).containsExactly(Status.SENT);
            }

            assertThat(postedLabels).as("iteration %d", iteration)
                .containsExactlyElementsOf(submittedLabels);
        }
    }

    @Test
    void testGlobalConcurrencyLimitBoundsInFlightPosts() throws Exception {
        List<CompletableFuture<WebhookDeliveryOutcome>> futures = new ArrayList<>();

        for (int index = 0; index < 4; index++) {
            blockOncePaths.add("/concurrent-" + index);

            futures.add(webhookDeliveryService.submit(delivery("/concurrent-" + index, 1)));
        }

        assertThat(arrivals.tryAcquire(2, 10, TimeUnit.SECONDS)).isTrue();
        assertThat(arrivals.tryAcquire(200, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(peakInFlightCount).hasValue(2);

        releaseLatch.countDown();

        for (CompletableFuture<WebhookDeliveryOutcome> future : futures) {
            assertThat(statuses(await(future))).containsExactly(Status.SENT);
        }

        assertThat(peakInFlightCount).hasValue(2);
    }

    @Test
    void testMaxPendingDeliveriesDropsTheExcess() throws Exception {
        blockOncePaths.add("/pending");

        CompletableFuture<WebhookDeliveryOutcome> firstFuture = webhookDeliveryService.submit(delivery("/pending", 1));

        assertThat(arrivals.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<WebhookDeliveryOutcome> secondFuture = webhookDeliveryService.submit(
            delivery("/pending", 1));
        CompletableFuture<WebhookDeliveryOutcome> thirdFuture = webhookDeliveryService.submit(delivery("/pending", 3));

        assertThat(thirdFuture).isDone();
        assertThat(statuses(await(thirdFuture))).containsExactly(Status.DROPPED, Status.DROPPED, Status.DROPPED);
        assertThat(unsentCount("dropped")).isEqualTo(3.0);

        releaseLatch.countDown();

        assertThat(statuses(await(firstFuture))).containsExactly(Status.SENT);
        assertThat(statuses(await(secondFuture))).containsExactly(Status.SENT);
    }

    @Test
    void testCompletedDeliveryNoLongerCountsAsPending() throws Exception {
        CountDownLatch releasePostLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            if (requestCount.incrementAndGet() == 1) {
                awaitRelease(releasePostLatch);
            }

            return new WebhookNotificationClient.WebhookResponse(200, null);
        }).when(webhookNotificationClient)
            .post(any(), any());

        WebhookDeliveryService singlePendingWebhookDeliveryService = createService(
            2, 1, meterRegistryProvider(null));

        CompletableFuture<WebhookDeliveryOutcome> firstFuture = singlePendingWebhookDeliveryService.submit(
            delivery("/resubmitted", 1));

        CompletableFuture<WebhookDeliveryOutcome> resubmittedFuture = firstFuture.thenCompose(
            outcome -> singlePendingWebhookDeliveryService.submit(delivery("/resubmitted", 1)));

        releasePostLatch.countDown();

        assertThat(statuses(await(firstFuture))).containsExactly(Status.SENT);
        assertThat(statuses(await(resubmittedFuture))).containsExactly(Status.SENT);
        assertThat(requestCount).hasValue(2);
    }

    @Test
    void testSubmitAfterShutdownCompletesAsDropped() throws Exception {
        WebhookDeliveryService closedWebhookDeliveryService = createService(2, 2, meterRegistryProvider(meterRegistry));

        webhookDeliveryExecutor.close();

        WebhookDeliveryOutcome outcome = await(closedWebhookDeliveryService.submit(delivery("/closed", 2)));

        assertThat(statuses(outcome)).containsExactly(Status.DROPPED, Status.DROPPED);
        assertThat(requestCount).hasValue(0);
    }

    @Test
    void testDrainerThatCannotStartDropsTheQueueAndLeavesTheEndpointUsable() throws Exception {
        AtomicInteger executeCount = new AtomicInteger();

        AsyncTaskExecutor failingOnceAsyncTaskExecutor = new AsyncTaskExecutor() {

            @Override
            public void execute(Runnable task) {
                if (executeCount.incrementAndGet() == 1) {
                    throw new IllegalStateException("thread factory failed");
                }

                webhookDeliveryExecutor.execute(task);
            }
        };

        WebhookDeliveryService failingOnceWebhookDeliveryService = createService(
            failingOnceAsyncTaskExecutor, 2, 2, meterRegistryProvider(meterRegistry));

        WebhookDeliveryOutcome droppedOutcome = await(
            failingOnceWebhookDeliveryService.submit(delivery("/unstartable", 1)));

        assertThat(statuses(droppedOutcome)).containsExactly(Status.DROPPED);
        assertThat(unsentCount("dropped")).isEqualTo(1.0);

        WebhookDeliveryOutcome laterOutcome = await(
            failingOnceWebhookDeliveryService.submit(delivery("/unstartable", 1)));

        assertThat(statuses(laterOutcome)).containsExactly(Status.SENT);
        assertThat(receivedBodies.get("/unstartable")).hasSize(1);
    }

    @Test
    void testDrainerThatFailsWithAnErrorDropsTheQueueAndRethrows() throws Exception {
        AtomicInteger executeCount = new AtomicInteger();

        AsyncTaskExecutor failingOnceAsyncTaskExecutor = new AsyncTaskExecutor() {

            @Override
            public void execute(Runnable task) {
                if (executeCount.incrementAndGet() == 1) {
                    throw new OutOfMemoryError("unable to create native thread");
                }

                webhookDeliveryExecutor.execute(task);
            }
        };

        WebhookDeliveryService failingOnceWebhookDeliveryService = createService(
            failingOnceAsyncTaskExecutor, 2, 2, meterRegistryProvider(meterRegistry));

        WebhookDelivery webhookDelivery = delivery("/erroring", 1);

        assertThatThrownBy(() -> failingOnceWebhookDeliveryService.submit(webhookDelivery))
            .isInstanceOf(OutOfMemoryError.class);
        assertThat(unsentCount("dropped")).isEqualTo(1.0);

        WebhookDeliveryOutcome laterOutcome = await(failingOnceWebhookDeliveryService.submit(webhookDelivery));

        assertThat(statuses(laterOutcome)).containsExactly(Status.SENT);
    }

    @Test
    void testUrlValidationDropsPrivateHosts() throws Exception {
        WebhookDelivery validatedWebhookDelivery = WebhookDelivery.builder(SOURCE, url("/private"))
            .description("test delivery")
            .tenantId(TENANT_ID)
            .validateUrl(true)
            .payload("payload-0", "{\"index\":0}")
            .build();

        assertThat(statuses(await(webhookDeliveryService.submit(validatedWebhookDelivery))))
            .containsExactly(Status.DROPPED);
        assertThat(requestCount).hasValue(0);

        assertThat(statuses(await(webhookDeliveryService.submit(delivery("/private", 1)))))
            .containsExactly(Status.SENT);
    }

    @Test
    void testUrlIsValidatedOnTheDeliveryThreadBeforeEveryAttempt() throws Exception {
        AtomicInteger validationCount = new AtomicInteger();
        List<Thread> validationThreads = new CopyOnWriteArrayList<>();

        webhookDeliveryService.setUrlValidator(url -> {
            validationThreads.add(Thread.currentThread());

            if (validationCount.incrementAndGet() > 1) {
                throw new UrlValidationException("Host resolves to a private address");
            }
        });

        respondWith("/rebinding", 503, 200);

        WebhookDelivery validatedWebhookDelivery = WebhookDelivery.builder(SOURCE, url("/rebinding"))
            .description("test delivery")
            .tenantId(TENANT_ID)
            .retry(new WebhookRetry(3, Duration.ofMillis(1), 1.0))
            .payload("payload-0", "{\"index\":0}")
            .build();

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(validatedWebhookDelivery));

        assertThat(statuses(outcome)).containsExactly(Status.DROPPED);
        assertThat(outcome.suspendedUntil()).isNull();
        assertThat(validationCount).hasValue(2);
        assertThat(validationThreads).doesNotContain(Thread.currentThread());
        assertThat(receivedBodies.get("/rebinding")).hasSize(1);
        assertThat(unsentCount("dropped")).isEqualTo(1.0);
    }

    @Test
    void testUrlThatIsNoLongerAllowedDropsTheRemainingPayloads() throws Exception {
        AtomicInteger validationCount = new AtomicInteger();

        webhookDeliveryService.setUrlValidator(url -> {
            if (validationCount.incrementAndGet() == 2) {
                throw new UrlValidationException("Host resolves to a private address");
            }
        });

        WebhookDelivery validatedWebhookDelivery = WebhookDelivery.builder(SOURCE, url("/partly"))
            .description("test delivery")
            .tenantId(TENANT_ID)
            .payload("payload-0", "{\"index\":0}")
            .payload("payload-1", "{\"index\":1}")
            .payload("payload-2", "{\"index\":2}")
            .build();

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(validatedWebhookDelivery));

        assertThat(statuses(outcome)).containsExactly(Status.SENT, Status.DROPPED, Status.DROPPED);
        assertThat(outcome.suspendedUntil()).isNull();
        assertThat(receivedBodies.get("/partly")).hasSize(1);
        assertThat(unsentCount("dropped")).isEqualTo(2.0);

        WebhookDeliveryOutcome laterOutcome = await(webhookDeliveryService.submit(validatedWebhookDelivery));

        assertThat(statuses(laterOutcome)).containsExactly(Status.SENT, Status.SENT, Status.SENT);
    }

    @Test
    void testTenantIdIsRestoredOnTheDeliveryThread() throws Exception {
        AtomicReference<String> recordedTenantId = new AtomicReference<>();

        doAnswer(invocation -> {
            recordedTenantId.set(TenantContext.getCurrentTenantId());

            return new WebhookNotificationClient.WebhookResponse(200, null);
        }).when(webhookNotificationClient)
            .post(any(), any());

        AtomicReference<CompletableFuture<WebhookDeliveryOutcome>> futureReference = new AtomicReference<>();

        TenantContext.runWithTenantId("t1", () -> futureReference.set(webhookDeliveryService.submit(
            WebhookDelivery.builder(SOURCE, url("/tenant"))
                .description("test delivery")
                .validateUrl(false)
                .payload("payload-0", "{\"index\":0}")
                .build())));

        assertThat(statuses(await(futureReference.get()))).containsExactly(Status.SENT);
        assertThat(recordedTenantId).hasValue("t1");
    }

    @Test
    void testEachQueuedDeliveryRunsWithTheContextOfItsOwnSubmission() throws Exception {
        String contextKey = "webhook-delivery-test-context";
        ThreadLocal<String> contextThreadLocal = new ThreadLocal<>();
        ContextRegistry contextRegistry = ContextRegistry.getInstance();

        contextRegistry.registerThreadLocalAccessor(
            contextKey, contextThreadLocal::get, contextThreadLocal::set, contextThreadLocal::remove);

        CountDownLatch firstPostLatch = new CountDownLatch(1);
        Map<String, String> observedContexts = new ConcurrentHashMap<>();
        CountDownLatch releasePostLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            WebhookPayload webhookPayload = invocation.getArgument(1);
            String observedContext = contextThreadLocal.get();

            observedContexts.put(webhookPayload.label(), observedContext == null ? "none" : observedContext);

            if (observedContexts.size() == 1) {
                firstPostLatch.countDown();

                awaitRelease(releasePostLatch);
            }

            return new WebhookNotificationClient.WebhookResponse(200, null);
        }).when(webhookNotificationClient)
            .post(any(), any());

        WebhookDeliveryService contextRecordingWebhookDeliveryService = createService(
            2, 10, meterRegistryProvider(null));

        try {
            contextThreadLocal.set("first-trace");

            CompletableFuture<WebhookDeliveryOutcome> firstFuture = contextRecordingWebhookDeliveryService.submit(
                delivery("/traced", "first"));

            assertThat(firstPostLatch.await(10, TimeUnit.SECONDS)).isTrue();

            contextThreadLocal.set("second-trace");

            CompletableFuture<WebhookDeliveryOutcome> secondFuture = contextRecordingWebhookDeliveryService.submit(
                delivery("/traced", "second"));

            contextThreadLocal.remove();

            CompletableFuture<WebhookDeliveryOutcome> thirdFuture = contextRecordingWebhookDeliveryService.submit(
                delivery("/traced", "third"));

            releasePostLatch.countDown();

            await(firstFuture);
            await(secondFuture);
            await(thirdFuture);
        } finally {
            contextThreadLocal.remove();
            contextRegistry.removeThreadLocalAccessor(contextKey);
        }

        assertThat(observedContexts).containsExactlyInAnyOrderEntriesOf(
            Map.of("first", "first-trace", "second", "second-trace", "third", "none"));
    }

    @Test
    void testDeliveryWhoseContextCannotBeRestoredStillCompletes() throws Exception {
        String contextKey = "webhook-delivery-failing-context";
        ThreadLocal<String> contextThreadLocal = new ThreadLocal<>();
        ContextRegistry contextRegistry = ContextRegistry.getInstance();

        contextRegistry.registerThreadLocalAccessor(
            contextKey, contextThreadLocal::get,
            value -> {
                if ("unrestorable".equals(value)) {
                    throw new IllegalStateException("context cannot be restored");
                }

                contextThreadLocal.set(value);
            },
            contextThreadLocal::remove);

        WebhookDeliveryService contextFailingWebhookDeliveryService = createService(
            2, 10, meterRegistryProvider(null));

        try {
            contextThreadLocal.set("unrestorable");

            CompletableFuture<WebhookDeliveryOutcome> unrestorableFuture = contextFailingWebhookDeliveryService.submit(
                delivery("/context", "unrestorable"));

            contextThreadLocal.remove();

            assertThat(statuses(await(unrestorableFuture))).containsExactly(Status.SKIPPED);

            CompletableFuture<WebhookDeliveryOutcome> laterFuture = contextFailingWebhookDeliveryService.submit(
                delivery("/context", "later"));

            assertThat(statuses(await(laterFuture))).containsExactly(Status.SENT);
        } finally {
            contextThreadLocal.remove();
            contextRegistry.removeThreadLocalAccessor(contextKey);
        }
    }

    @Test
    void testInterruptDuringShutdownSkipsTheRest() throws Exception {
        blockOncePaths.add("/interrupted");

        WebhookDeliveryService interruptedWebhookDeliveryService = createService(
            2, 2, meterRegistryProvider(meterRegistry));

        CompletableFuture<WebhookDeliveryOutcome> future = interruptedWebhookDeliveryService.submit(
            delivery("/interrupted", 3));

        assertThat(arrivals.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        webhookDeliveryExecutor.close();
        releaseLatch.countDown();

        WebhookDeliveryOutcome outcome = await(future);

        assertThat(outcome.count(Status.SKIPPED)).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testInterruptDuringRetryBackOffSkipsThePayloadAndTheRest() throws Exception {
        respondWith("/backing-off", 503, 503);

        WebhookDeliveryService backingOffWebhookDeliveryService = createService(
            2, 2, meterRegistryProvider(meterRegistry));

        CompletableFuture<WebhookDeliveryOutcome> future = backingOffWebhookDeliveryService.submit(
            delivery("/backing-off", 2, new WebhookRetry(3, Duration.ofSeconds(30), 1.0)));

        assertThat(arrivals.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        webhookDeliveryExecutor.close();

        WebhookDeliveryOutcome outcome = await(future);

        assertThat(statuses(outcome)).containsExactly(Status.SKIPPED, Status.SKIPPED);
        assertThat(outcome.firstFailure()).isNull();
        assertThat(unsentCount("failed")).isZero();
        assertThat(unsentCount("skipped")).isEqualTo(2.0);
        assertThat(receivedBodies.get("/backing-off")).hasSize(1);
    }

    @Test
    void testInterruptDuringShutdownSkipsEveryQueuedDelivery() throws Exception {
        WebhookDeliveryService queueingWebhookDeliveryService = createService(
            2, 10, meterRegistryProvider(meterRegistry));

        blockOncePaths.add("/queued");

        CompletableFuture<WebhookDeliveryOutcome> runningFuture = queueingWebhookDeliveryService.submit(
            delivery("/queued", 1));

        assertThat(arrivals.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

        List<CompletableFuture<WebhookDeliveryOutcome>> queuedFutures = new ArrayList<>();

        for (int index = 0; index < 3; index++) {
            queuedFutures.add(queueingWebhookDeliveryService.submit(delivery("/queued", 2)));
        }

        webhookDeliveryExecutor.close();
        releaseLatch.countDown();

        await(runningFuture);

        for (CompletableFuture<WebhookDeliveryOutcome> queuedFuture : queuedFutures) {
            assertThat(statuses(await(queuedFuture))).containsExactly(Status.SKIPPED, Status.SKIPPED);
        }

        assertThat(requestCount).hasValue(1);
    }

    @Test
    void testConcurrentSubmissionsToOneEndpointAreAllDelivered() throws Exception {
        AtomicInteger postCount = new AtomicInteger();

        doAnswer(invocation -> {
            postCount.incrementAndGet();

            return new WebhookNotificationClient.WebhookResponse(200, null);
        }).when(webhookNotificationClient)
            .post(any(), any());

        WebhookDeliveryService countingWebhookDeliveryService = createService(
            2, 10_000, meterRegistryProvider(null));

        List<CompletableFuture<WebhookDeliveryOutcome>> futures = new CopyOnWriteArrayList<>();
        ExecutorService submitterExecutor = Executors.newFixedThreadPool(4);

        try {
            for (int submitterIndex = 0; submitterIndex < 4; submitterIndex++) {
                submitterExecutor.execute(() -> {
                    for (int index = 0; index < 500; index++) {
                        futures.add(countingWebhookDeliveryService.submit(delivery("/concurrent", 1)));
                    }
                });
            }
        } finally {
            submitterExecutor.shutdown();
        }

        assertThat(submitterExecutor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(futures).hasSize(2000);

        for (CompletableFuture<WebhookDeliveryOutcome> future : futures) {
            assertThat(statuses(await(future))).containsExactly(Status.SENT);
        }

        assertThat(postCount).hasValue(2000);
    }

    @Test
    void testDeliveriesAbandonedByADyingDrainerAreLoggedWithAReason() throws Exception {
        CountDownLatch firstPostLatch = new CountDownLatch(1);
        CountDownLatch releasePostLatch = new CountDownLatch(1);

        doAnswer(invocation -> {
            if (requestCount.incrementAndGet() == 1) {
                firstPostLatch.countDown();

                awaitRelease(releasePostLatch);

                throw new LinkageError("drainer died");
            }

            return new WebhookNotificationClient.WebhookResponse(200, null);
        }).when(webhookNotificationClient)
            .post(any(), any());

        WebhookDeliveryService dyingWebhookDeliveryService = createService(
            2, 10, meterRegistryProvider(null));

        Logger logger = (Logger) LoggerFactory.getLogger(WebhookDeliveryService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();
        logger.addAppender(listAppender);

        List<CompletableFuture<WebhookDeliveryOutcome>> futures = new ArrayList<>();

        try {
            futures.add(dyingWebhookDeliveryService.submit(delivery("/dying", 1)));

            assertThat(firstPostLatch.await(10, TimeUnit.SECONDS)).isTrue();

            futures.add(dyingWebhookDeliveryService.submit(delivery("/dying", 1)));
            futures.add(dyingWebhookDeliveryService.submit(delivery("/dying", 1)));

            releasePostLatch.countDown();

            for (CompletableFuture<WebhookDeliveryOutcome> future : futures) {
                assertThat(statuses(await(future))).containsExactly(Status.SKIPPED);
            }
        } finally {
            logger.detachAppender(listAppender);
        }

        assertThat(listAppender.list)
            .filteredOn(loggingEvent -> loggingEvent.getLevel() == Level.ERROR)
            .hasSize(3)
            .allSatisfy(loggingEvent -> assertThat(loggingEvent.getFormattedMessage())
                .contains("the delivery task ended unexpectedly"));

        assertThat(statuses(await(dyingWebhookDeliveryService.submit(delivery("/dying", 1)))))
            .containsExactly(Status.SENT);
    }

    @Test
    void testLogLinesNameTheHostAndNeverTheFullUrl() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(WebhookDeliveryService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        Level previousLevel = logger.getLevel();

        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);

        try {
            respondWith("/hook", 404);

            WebhookDelivery notFoundWebhookDelivery = WebhookDelivery.builder(SOURCE, url("/hook") + "?token=SECRET")
                .description("webhook " + url("/hook") + "?token=SECRET")
                .tenantId(TENANT_ID)
                .validateUrl(false)
                .payload("payload-0", "{\"index\":0}")
                .build();

            await(webhookDeliveryService.submit(notFoundWebhookDelivery));
            await(webhookDeliveryService.submit(notFoundWebhookDelivery));

            WebhookDelivery refusedWebhookDelivery = WebhookDelivery.builder(SOURCE, closedPortUrl())
                .description("test delivery")
                .tenantId(TENANT_ID)
                .validateUrl(false)
                .payload("payload-0", "{\"index\":0}")
                .build();

            assertThat(statuses(await(webhookDeliveryService.submit(refusedWebhookDelivery))))
                .containsExactly(Status.FAILED);

            WebhookDelivery validatedWebhookDelivery = WebhookDelivery.builder(
                SOURCE, url("/hook-validated") + "?token=SECRET")
                .description("test delivery")
                .tenantId(TENANT_ID)
                .payload("payload-0", "{\"index\":0}")
                .build();

            await(webhookDeliveryService.submit(validatedWebhookDelivery));
        } finally {
            logger.detachAppender(listAppender);
            logger.setLevel(previousLevel);
        }

        assertThat(listAppender.list).hasSizeGreaterThanOrEqualTo(4);
        assertThat(listAppender.list).allSatisfy(loggingEvent -> {
            assertThat(loggingEvent.getFormattedMessage()).contains("localhost")
                .doesNotContain("SECRET")
                .doesNotContain("/hook");
            assertThat(loggingEvent.getThrowableProxy()).isNull();
        });
        assertThat(listAppender.list).anySatisfy(loggingEvent -> assertThat(loggingEvent.getFormattedMessage())
            .contains("Connect"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {
        true, false
    })
    void testExpiredFailureCountsAndPausesArePruned(boolean paused) throws Exception {
        webhookDeliveryService.clearEndpointState();

        if (paused) {
            respondWith("/expiring", 404);

            await(webhookDeliveryService.submit(delivery("/expiring", 1)));
        } else {
            respondWith("/expiring", repeat(500, 9));

            WebhookDeliveryOutcome failingOutcome = await(webhookDeliveryService.submit(delivery("/expiring", 9)));

            assertThat(failingOutcome.count(Status.FAILED)).isEqualTo(9);
            assertThat(failingOutcome.suspendedUntil()).isNull();
        }

        mutableClock.advance(Duration.ofHours(1)
            .plusMinutes(1));

        await(webhookDeliveryService.submit(delivery("/other", 1)));

        respondWith("/expiring", 500);

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/expiring", 1)));

        assertThat(statuses(outcome)).containsExactly(Status.FAILED);
        assertThat(outcome.suspendedUntil()).isNull();
    }

    @Test
    void testMetricsCountPayloadsBySourceAndOutcome() throws Exception {
        respondWith("/mixed", 200, 500, 404);

        WebhookDeliveryOutcome outcome = await(webhookDeliveryService.submit(delivery("/mixed", 4)));

        assertThat(statuses(outcome)).containsExactly(Status.SENT, Status.FAILED, Status.FAILED, Status.SKIPPED);

        WebhookDelivery validatedWebhookDelivery = WebhookDelivery.builder(SOURCE, url("/mixed-validated"))
            .description("test delivery")
            .tenantId(TENANT_ID)
            .payload("payload-0", "{\"index\":0}")
            .build();

        await(webhookDeliveryService.submit(validatedWebhookDelivery));

        assertThat(unsentCount("failed")).isEqualTo(outcome.count(Status.FAILED));
        assertThat(unsentCount("skipped")).isEqualTo(1.0);
        assertThat(unsentCount("dropped")).isEqualTo(1.0);
        assertThat(suspensionCount()).isEqualTo(1.0);
    }

    @Test
    void testWorksWithoutAMeterRegistry() throws Exception {
        WebhookDeliveryService unmeteredWebhookDeliveryService = createService(2, 2, meterRegistryProvider(null));

        respondWith("/unmetered", 500, 404);

        WebhookDeliveryOutcome outcome = await(unmeteredWebhookDeliveryService.submit(delivery("/unmetered", 3)));

        assertThat(statuses(outcome)).containsExactly(Status.FAILED, Status.FAILED, Status.SKIPPED);
    }

    @Test
    void testLimitBelowOneFailsConstruction() {
        ObjectProvider<MeterRegistry> meterRegistryProvider = meterRegistryProvider(meterRegistry);

        assertThatThrownBy(() -> createService(0, 2, meterRegistryProvider))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.webhook-delivery.concurrency-limit");
        assertThatThrownBy(() -> createService(2, 0, meterRegistryProvider))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.webhook-delivery.max-pending-deliveries");
    }

    @Test
    void testContextClosedWarnsAboutPendingDeliveries() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(WebhookDeliveryService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

        listAppender.start();
        logger.addAppender(listAppender);

        blockOncePaths.add("/closing");

        CompletableFuture<WebhookDeliveryOutcome> future = webhookDeliveryService.submit(delivery("/closing", 1));

        try {
            assertThat(arrivals.tryAcquire(10, TimeUnit.SECONDS)).isTrue();

            webhookDeliveryService.onContextClosed();
        } finally {
            logger.detachAppender(listAppender);
        }

        releaseLatch.countDown();

        assertThat(statuses(await(future))).containsExactly(Status.SENT);
        assertThat(listAppender.list).anySatisfy(loggingEvent -> {
            assertThat(loggingEvent.getLevel()).isEqualTo(Level.WARN);
            assertThat(loggingEvent.getFormattedMessage()).contains("1 outbound webhook deliveries");
        });
    }

    private static WebhookDeliveryOutcome await(CompletableFuture<WebhookDeliveryOutcome> future)
        throws ExecutionException, InterruptedException, TimeoutException {

        return future.get(10, TimeUnit.SECONDS);
    }

    private static void awaitRelease(CountDownLatch releasePostLatch) {
        try {
            if (!releasePostLatch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("The blocked POST was not released");
            }
        } catch (InterruptedException interruptedException) {
            Thread currentThread = Thread.currentThread();

            currentThread.interrupt();
        }
    }

    private Runnable startAfterAVaryingDelay(Runnable runnable) {
        int delayMillis = startedTaskCount.getAndIncrement() % 3;

        return () -> {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException interruptedException) {
                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();
            }

            runnable.run();
        };
    }

    @SuppressFBWarnings(
        value = "UNENCRYPTED_SERVER_SOCKET",
        justification = "Loopback-only socket opened and closed at once to find a port nothing listens on")
    private static String closedPortUrl() throws IOException {
        int port;

        try (ServerSocket serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            port = serverSocket.getLocalPort();
        }

        return "http://localhost:" + port + "/hook?token=SECRET";
    }

    private static ObjectProvider<MeterRegistry> meterRegistryProvider(MeterRegistry meterRegistry) {
        DefaultListableBeanFactory defaultListableBeanFactory = new DefaultListableBeanFactory();

        if (meterRegistry != null) {
            defaultListableBeanFactory.registerSingleton("meterRegistry", meterRegistry);
        }

        return defaultListableBeanFactory.getBeanProvider(MeterRegistry.class);
    }

    private static Integer[] repeat(int status, int count) {
        Integer[] statuses = new Integer[count];

        Arrays.fill(statuses, status);

        return statuses;
    }

    private static List<Status> statuses(WebhookDeliveryOutcome outcome) {
        return outcome.payloadResults()
            .stream()
            .map(PayloadResult::status)
            .toList();
    }

    private static SimpleAsyncTaskExecutor createExecutor() {
        SimpleAsyncTaskExecutor simpleAsyncTaskExecutor = new SimpleAsyncTaskExecutor("webhook-delivery-test-");

        simpleAsyncTaskExecutor.setCancelRemainingTasksOnClose(true);
        simpleAsyncTaskExecutor.setVirtualThreads(true);

        return simpleAsyncTaskExecutor;
    }

    private WebhookDeliveryService createService(
        int concurrencyLimit, int maxPendingDeliveries, ObjectProvider<MeterRegistry> meterRegistryProvider) {

        return createService(webhookDeliveryExecutor, concurrencyLimit, maxPendingDeliveries, meterRegistryProvider);
    }

    private WebhookDeliveryService createService(
        AsyncTaskExecutor asyncTaskExecutor, int concurrencyLimit, int maxPendingDeliveries,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.WebhookDelivery webhookDeliveryProperties = applicationProperties.getWebhookDelivery();

        webhookDeliveryProperties.setConcurrencyLimit(concurrencyLimit);
        webhookDeliveryProperties.setMaxPendingDeliveries(maxPendingDeliveries);

        WebhookDeliveryService createdWebhookDeliveryService = new WebhookDeliveryService(
            webhookNotificationClient, asyncTaskExecutor, applicationProperties, meterRegistryProvider);

        createdWebhookDeliveryService.setClock(mutableClock);

        return createdWebhookDeliveryService;
    }

    private WebhookDelivery delivery(String path, int payloadCount) {
        return delivery(TENANT_ID, path, payloadCount, WebhookRetry.none());
    }

    private WebhookDelivery delivery(String path, int payloadCount, WebhookRetry webhookRetry) {
        return delivery(TENANT_ID, path, payloadCount, webhookRetry);
    }

    private WebhookDelivery delivery(String tenantId, String path, int payloadCount, WebhookRetry webhookRetry) {
        WebhookDelivery.Builder builder = WebhookDelivery.builder(SOURCE, url(path))
            .description("test delivery")
            .tenantId(tenantId)
            .validateUrl(false)
            .retry(webhookRetry);

        for (int index = 0; index < payloadCount; index++) {
            builder.payload("payload-" + index, "{\"index\":" + index + "}");
        }

        return builder.build();
    }

    private WebhookDelivery delivery(String path, String name) {
        return WebhookDelivery.builder(SOURCE, url(path))
            .description("test delivery")
            .tenantId(TENANT_ID)
            .validateUrl(false)
            .payload(name, "{\"name\":\"" + name + "\"}")
            .build();
    }

    private void respondWith(String path, Integer... statuses) {
        statusSequences.put(path, new ConcurrentLinkedDeque<>(Arrays.asList(statuses)));
    }

    private double suspensionCount() {
        Counter counter = meterRegistry.find(WebhookDeliveryService.SUSPENSIONS_COUNTER)
            .tag("source", SOURCE)
            .counter();

        return counter == null ? 0 : counter.count();
    }

    private double unsentCount(String outcome) {
        Counter counter = meterRegistry.find(WebhookDeliveryService.UNSENT_COUNTER)
            .tag("source", SOURCE)
            .tag("outcome", outcome)
            .counter();

        return counter == null ? 0 : counter.count();
    }

    private String url(String path) {
        return "http://localhost:" + httpServer.getAddress()
            .getPort() + path;
    }

    private static final class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        private void advance(Duration duration) {
            instant.updateAndGet(currentInstant -> currentInstant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
