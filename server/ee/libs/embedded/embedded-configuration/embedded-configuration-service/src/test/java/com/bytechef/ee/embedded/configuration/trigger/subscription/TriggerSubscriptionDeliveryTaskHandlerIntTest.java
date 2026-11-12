/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.trigger.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.worker.exception.TaskExecutionException;
import com.bytechef.atlas.worker.task.handler.TaskHandler;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionStatus;
import com.bytechef.ee.embedded.configuration.facade.TriggerSubscriptionWorkflowDefinitionFactory;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationRecorder;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Runs a delivery through the task handler registered under the deliver task type, the event sender and the real
 * webhook delivery lane to a local HTTP endpoint, and checks the signature, the recorded invocation and the delivery
 * columns. The deliver task is taken from the generated workflow definition and evaluated against the trigger output
 * with the real evaluator; the coordinator and the worker that would run it are not on this module's test classpath.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class,
        TriggerSubscriptionDeliveryTaskHandlerIntTest.WebhookDeliveryConfiguration.class
    },
    properties = {
        "bytechef.webhook-delivery.allowed-hosts=127.0.0.1", "bytechef.webhook-delivery.concurrency-limit=2",
        "bytechef.webhook-delivery.max-pending-deliveries=10", "spring.liquibase.contexts=configuration,user"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class TriggerSubscriptionDeliveryTaskHandlerIntTest {

    private static final long FOREIGN_JOB_ID = 43L;
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("t=(\\d+),v1=([0-9a-f]{64})");
    private static final long JOB_ID = 42L;
    private static final long PROJECT_DEPLOYMENT_ID = 7001L;
    private static final String SIGNING_SECRET = "whsec_test";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Evaluator evaluator;

    @MockitoBean
    private ExternalInvocationRecorder externalInvocationRecorder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PrincipalJobService principalJobService;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private TriggerSubscriptionService triggerSubscriptionService;

    private HttpServer httpServer;
    private final List<ReceivedRequest> receivedRequests = new CopyOnWriteArrayList<>();

    @BeforeEach
    void beforeEach() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);

        httpServer.createContext("/hook", httpExchange -> {
            try (InputStream inputStream = httpExchange.getRequestBody()) {
                receivedRequests.add(
                    new ReceivedRequest(
                        httpExchange.getRequestHeaders()
                            .getFirst("X-ByteChef-Signature"),
                        new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)));
            }

            httpExchange.sendResponseHeaders(200, 0);

            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                outputStream.flush();
            }
        });

        httpServer.start();

        Property property = mock(Property.class);

        Map<String, Object> value = Map.of("signingSecret", SIGNING_SECRET);

        doReturn(value).when(property)
            .getValue();
        when(propertyService.fetchProperty(any(), any(), any(), any())).thenReturn(Optional.of(property));
        when(principalJobService.fetchJobPrincipalId(JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(PROJECT_DEPLOYMENT_ID));
        when(principalJobService.fetchJobPrincipalId(FOREIGN_JOB_ID, PlatformType.AUTOMATION))
            .thenReturn(Optional.of(PROJECT_DEPLOYMENT_ID + 1));
    }

    @AfterEach
    void afterEach() {
        httpServer.stop(0);

        jdbcTemplate.update("DELETE FROM connected_user_trigger_subscription");
        jdbcTemplate.update("DELETE FROM connected_user");
    }

    @Test
    void testTheHandlerIsRegisteredUnderTheDeliverTaskType() {
        assertThat(applicationContext.getBean(TriggerSubscriptionWorkflowDefinitionFactory.DELIVER_TASK_TYPE))
            .isInstanceOf(TriggerSubscriptionDeliveryTaskHandler.class);
    }

    @Test
    void testDeliveryReachesTheEndpointSignedAndIsRecorded() throws TaskExecutionException {
        TriggerSubscription triggerSubscription = createTriggerSubscription();

        handle(createTaskExecution(JOB_ID, triggerSubscription.getId(), Map.of("id", "c1")));

        verify(externalInvocationRecorder, timeout(10_000)).record(
            argThat(externalInvocationEvent -> Objects.equals(externalInvocationEvent.jobId(), JOB_ID) &&
                externalInvocationEvent.source() == ExternalInvocationSource.API_TRIGGER &&
                externalInvocationEvent.outcome() == ExternalInvocationOutcome.SUCCESS));

        assertThat(receivedRequests).singleElement()
            .satisfies(receivedRequest -> {
                assertSignature(receivedRequest);

                Map<String, ?> envelope = JsonUtils.readMap(receivedRequest.body());

                assertThat(envelope.get("type")).isEqualTo("trigger.event");
                assertThat(envelope.get("data")).isEqualTo(Map.of("id", "c1"));
                assertThat(((Number) envelope.get("jobId")).longValue()).isEqualTo(JOB_ID);
                assertThat(((Number) envelope.get("subscriptionId")).longValue())
                    .isEqualTo(triggerSubscription.getId());
            });

        TriggerSubscription deliveredTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(deliveredTriggerSubscription.getLastDeliveryStatus()).isEqualTo("SENT");
        assertThat(deliveredTriggerSubscription.getLastDeliveryError()).isNull();
        assertThat(deliveredTriggerSubscription.getLastEventDate()).isNotNull();
    }

    @Test
    void testBatchTriggerOutputIsDeliveredAsTheListInData() throws TaskExecutionException {
        TriggerSubscription triggerSubscription = createTriggerSubscription();

        List<Map<String, String>> batchOutput = List.of(Map.of("id", "c1"), Map.of("id", "c2"));

        handle(createTaskExecution(JOB_ID, triggerSubscription.getId(), batchOutput));

        verify(externalInvocationRecorder, timeout(10_000)).record(
            argThat(externalInvocationEvent -> Objects.equals(externalInvocationEvent.jobId(), JOB_ID)));

        assertThat(receivedRequests).singleElement()
            .satisfies(receivedRequest -> {
                assertSignature(receivedRequest);

                Map<String, ?> envelope = JsonUtils.readMap(receivedRequest.body());

                assertThat(envelope.get("data")).isEqualTo(batchOutput);
            });
    }

    @Test
    void testDeliveryFromAJobOfAnotherDeploymentSendsNothing() throws TaskExecutionException {
        TriggerSubscription triggerSubscription = createTriggerSubscription();

        handle(createTaskExecution(FOREIGN_JOB_ID, triggerSubscription.getId(), Map.of("id", "c1")));
        handle(createTaskExecution(JOB_ID, triggerSubscription.getId(), Map.of("id", "c1")));

        verify(externalInvocationRecorder, timeout(10_000)).record(
            argThat(externalInvocationEvent -> Objects.equals(externalInvocationEvent.jobId(), JOB_ID)));
        verify(externalInvocationRecorder, never()).record(
            argThat(externalInvocationEvent -> Objects.equals(externalInvocationEvent.jobId(), FOREIGN_JOB_ID)));

        assertThat(receivedRequests).singleElement()
            .satisfies(receivedRequest -> assertThat(receivedRequest.body()).contains("\"jobId\":" + JOB_ID));
    }

    @Test
    void testDeliveryHeldWhileTheSubscriptionIsEnablingIsDeliveredOnceEnabled() throws TaskExecutionException {
        TriggerSubscription triggerSubscription = createEnablingTriggerSubscription();

        WorkflowTask workflowTask = createDeliverWorkflowTask(triggerSubscription.getId());

        assertThat(workflowTask.getMaxRetries()).isPositive();

        TaskExecution taskExecution = createTaskExecution(JOB_ID, triggerSubscription.getId(), Map.of("id", "c1"));

        assertThatThrownBy(() -> handle(taskExecution)).isInstanceOf(TriggerSubscriptionEnablingException.class);

        verify(externalInvocationRecorder, never()).record(any());

        TriggerSubscription heldTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(heldTriggerSubscription.getLastDeliveryStatus()).isNull();
        assertThat(heldTriggerSubscription.getLastEventDate()).isNull();

        heldTriggerSubscription.setRegistrationLeaseUntil(null);
        heldTriggerSubscription.setStatus(TriggerSubscriptionStatus.ENABLED);

        triggerSubscriptionService.update(heldTriggerSubscription);

        handle(createTaskExecution(JOB_ID, triggerSubscription.getId(), Map.of("id", "c1")));

        verify(externalInvocationRecorder, timeout(10_000)).record(
            argThat(externalInvocationEvent -> Objects.equals(externalInvocationEvent.jobId(), JOB_ID) &&
                externalInvocationEvent.outcome() == ExternalInvocationOutcome.SUCCESS));
        verify(externalInvocationRecorder, times(1)).record(any());

        assertThat(receivedRequests).singleElement()
            .satisfies(receivedRequest -> assertSignature(receivedRequest));

        TriggerSubscription deliveredTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(deliveredTriggerSubscription.getLastDeliveryStatus()).isEqualTo("SENT");
    }

    @Test
    void testDeliveryHeldWhileTheSubscriptionIsEnablingIsDroppedWhenTheEnableFails() throws TaskExecutionException {
        TriggerSubscription triggerSubscription = createEnablingTriggerSubscription();

        TaskExecution taskExecution = createTaskExecution(JOB_ID, triggerSubscription.getId(), Map.of("id", "c1"));

        assertThatThrownBy(() -> handle(taskExecution)).isInstanceOf(TriggerSubscriptionEnablingException.class);

        TriggerSubscription heldTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        heldTriggerSubscription.setRegistrationLeaseUntil(null);

        triggerSubscriptionService.update(heldTriggerSubscription);

        handle(createTaskExecution(JOB_ID, triggerSubscription.getId(), Map.of("id", "c1")));

        verify(externalInvocationRecorder, never()).record(any());

        assertThat(receivedRequests).isEmpty();

        TriggerSubscription droppedTriggerSubscription = triggerSubscriptionService.getTriggerSubscription(
            triggerSubscription.getId());

        assertThat(droppedTriggerSubscription.getLastDeliveryStatus()).isNull();
    }

    private TriggerSubscription createEnablingTriggerSubscription() {
        TriggerSubscription triggerSubscription = createTriggerSubscription();

        triggerSubscription.setRegistrationLeaseUntil(Instant.now()
            .plus(Duration.ofMinutes(5)));
        triggerSubscription.setStatus(TriggerSubscriptionStatus.FAILED);

        return triggerSubscriptionService.update(triggerSubscription);
    }

    private TriggerSubscription createTriggerSubscription() {
        TriggerSubscription triggerSubscription = new TriggerSubscription();

        triggerSubscription.setComponentName("slack");
        triggerSubscription.setComponentVersion(1);
        triggerSubscription.setConnectedUserId(insertConnectedUser());
        triggerSubscription.setEnvironment(Environment.PRODUCTION);
        triggerSubscription.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);
        triggerSubscription.setStatus(TriggerSubscriptionStatus.ENABLED);
        triggerSubscription.setTriggerName("newMessage");
        triggerSubscription.setWebhookUrl(
            "http://" + InetAddress.getLoopbackAddress()
                .getHostAddress() + ":"
                + httpServer.getAddress()
                    .getPort()
                + "/hook");

        return triggerSubscriptionService.create(triggerSubscription);
    }

    private static void assertSignature(ReceivedRequest receivedRequest) {
        Matcher matcher = SIGNATURE_PATTERN.matcher(Objects.requireNonNull(receivedRequest.signature()));

        assertThat(matcher.matches()).isTrue();

        String signedContent = matcher.group(1) + "." + receivedRequest.body();

        assertThat(matcher.group(2)).isEqualTo(computeSignature(signedContent));
    }

    private static String computeSignature(String signedContent) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException generalSecurityException) {
            throw new IllegalStateException(generalSecurityException);
        }
    }

    /**
     * Builds the deliver task from the workflow definition a subscription deploys and evaluates it against the job
     * inputs {@code TriggerCompletionHandler} creates, which key the trigger output by the trigger node name. Running
     * the coordinator and the worker is out of reach here because this module has neither on its test classpath.
     */
    private TaskExecution createTaskExecution(long jobId, long triggerSubscriptionId, Object triggerOutput) {
        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .workflowTask(createDeliverWorkflowTask(triggerSubscriptionId))
            .build();

        return taskExecution.evaluate(
            Map.of(TriggerSubscriptionWorkflowDefinitionFactory.TRIGGER_NAME, triggerOutput), evaluator);
    }

    @SuppressWarnings("unchecked")
    private static WorkflowTask createDeliverWorkflowTask(long triggerSubscriptionId) {
        String workflowDefinition = TriggerSubscriptionWorkflowDefinitionFactory.create(
            "slack", 1, "newMessage", Map.of(), triggerSubscriptionId, Duration.ofMinutes(5));

        Map<String, ?> workflowMap = JsonUtils.readMap(workflowDefinition);

        List<?> tasks = (List<?>) workflowMap.get("tasks");

        return new WorkflowTask((Map<String, ?>) tasks.getFirst());
    }

    private void handle(TaskExecution taskExecution) throws TaskExecutionException {
        TaskHandler<?> taskHandler = applicationContext.getBean(
            TriggerSubscriptionWorkflowDefinitionFactory.DELIVER_TASK_TYPE, TaskHandler.class);

        taskHandler.handle(taskExecution);
    }

    private long insertConnectedUser() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO connected_user
                        (external_id, enabled, environment, created_date, created_by, last_modified_date,
                         last_modified_by, version)
                    VALUES ('delivery-user', true, ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, Environment.PRODUCTION.ordinal()),
            "connected user id");
    }

    private record ReceivedRequest(String signature, String body) {
    }

    @Configuration
    static class WebhookDeliveryConfiguration {

        @Bean
        @Primary
        WebhookDeliveryService realWebhookDeliveryService(
            ApplicationProperties applicationProperties, ObjectProvider<MeterRegistry> meterRegistryProvider) {

            return new WebhookDeliveryService(
                new WebhookNotificationClient(), new SimpleAsyncTaskExecutor(), applicationProperties,
                meterRegistryProvider);
        }
    }
}
