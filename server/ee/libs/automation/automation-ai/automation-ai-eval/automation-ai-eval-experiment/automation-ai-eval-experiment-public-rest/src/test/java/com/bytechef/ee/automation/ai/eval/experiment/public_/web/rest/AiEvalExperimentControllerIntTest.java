/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.public_.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayFacade;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.gateway.dto.AiGatewayChatCompletionResponse;
import com.bytechef.ee.platform.ai.gateway.metrics.AiGatewayMetrics;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayChatModelFactory;
import com.bytechef.ee.platform.ai.gateway.provider.AiGatewayEmbeddingModelFactory;
import com.bytechef.file.storage.service.FileStorageService;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(
    classes = AiEvalExperimentPublicRestIntTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    AiGatewayChatModelFactory.class, AiGatewayEmbeddingModelFactory.class, AiGatewayMetrics.class,
    AiGatewayWorkspaceHeaderResolver.class, FileStorageService.class, PropertyService.class
})
public class AiEvalExperimentControllerIntTest {

    private static final Long WORKSPACE_ID = 42L;

    @LocalServerPort
    private int port;

    @Autowired
    private AiEvalDatasetItemService aiEvalDatasetItemService;

    @Autowired
    private WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;

    @Autowired
    private AiEvalDatasetVersionService aiEvalDatasetVersionService;

    @Autowired
    private AiEvalExperimentRunService aiEvalExperimentRunService;

    @Autowired
    private AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;

    @MockitoBean
    private AiGatewayFacade aiGatewayFacade;

    @BeforeEach
    public void setUpWorkspaceResolverAndFacade() {
        when(workspaceHeaderResolver.resolveAndVerify(String.valueOf(WORKSPACE_ID)))
            .thenReturn(WORKSPACE_ID);
        when(workspaceHeaderResolver.resolveAndVerify((String) null))
            .thenReturn(null);

        when(aiGatewayFacade.chatCompletion(any()))
            .thenAnswer(invocation -> stubChatCompletionResponse());
    }

    @Test
    public void testCreateExperimentThroughHttpCompletesAllRuns() {
        long datasetVersionId = seedDatasetWithTwoItems();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-ByteChef-Workspace-Id", String.valueOf(WORKSPACE_ID));

        Map<String, Object> body = Map.of(
            "datasetVersionId", datasetVersionId,
            "model", "gpt-4o");

        RestClient restClient = RestClient.create();

        ResponseEntity<Map<String, Object>> createResponse = restClient
            .method(HttpMethod.POST)
            .uri(baseUrl() + "/api/ai-gateway/v1/experiments")
            .headers(httpHeaders -> httpHeaders.putAll(headers))
            .body(body)
            .retrieve()
            .toEntity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        Map<String, Object> createBody = createResponse.getBody();

        assertThat(createBody).isNotNull();
        assertThat(createBody.get("status")).isEqualTo("PENDING");
        assertThat(createBody.get("workspaceId")).isEqualTo(WORKSPACE_ID.intValue());
        assertThat(createBody.get("id"))
            .isNotNull()
            .isInstanceOf(Number.class);

        long experimentId = ((Number) createBody.get("id")).longValue();

        await()
            .atMost(Duration.ofSeconds(10))
            .until(() -> {
                Map<String, Object> summary = fetchSummary(experimentId);

                String status = (String) summary.get("status");

                return "COMPLETED".equals(status) || "FAILED".equals(status);
            });

        Map<String, Object> finalSummary = fetchSummary(experimentId);

        assertThat(finalSummary.get("status")).isEqualTo("COMPLETED");
        assertThat(((Number) finalSummary.get("totalRuns")).longValue()).isEqualTo(2L);
        assertThat(((Number) finalSummary.get("completedRuns")).longValue()).isEqualTo(2L);
        assertThat(((Number) finalSummary.get("failedRuns")).longValue()).isEqualTo(0L);

        List<AiEvalExperimentRun> runs = aiEvalExperimentRunService.findAllByExperiment(experimentId);

        assertThat(runs).hasSize(2);
        assertThat(runs).allMatch(run -> run.getStatus() == AiEvalExperimentRunStatus.COMPLETED);
        assertThat(runs).allMatch(run -> run.getLatencyMs() != null);
    }

    @Test
    public void testReturns400WhenWorkspaceHeaderMissing() {
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
            "datasetVersionId", 1,
            "model", "gpt-4o");

        RestClient restClient = RestClient.create();

        ResponseEntity<String> response = restClient
            .method(HttpMethod.POST)
            .uri(baseUrl() + "/api/ai-gateway/v1/experiments")
            .headers(httpHeaders -> httpHeaders.putAll(headers))
            .body(requestBody)
            .retrieve()
            .onStatus(status -> status.value() == 400, (request, clientResponse) -> {})
            .toEntity(String.class);

        assertThat(response.getStatusCode()
            .value()).isEqualTo(400);
        assertThat(response.getBody())
            .isNotNull()
            .contains("missing_workspace_id");
    }

    private long seedDatasetWithTwoItems() {
        AiEvalDataset dataset = new AiEvalDataset("int-test-http-dataset");
        AiEvalDataset createdDataset = workspaceAiEvalDatasetService.createInWorkspace(dataset, WORKSPACE_ID);

        String jsonInput = "{\"model\":\"gpt-4o\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}]," +
            "\"stream\":false}";

        aiEvalDatasetItemService.addItem(createdDataset.getId(), jsonInput, null, null);
        aiEvalDatasetItemService.addItem(createdDataset.getId(), jsonInput, null, null);

        AiEvalDatasetVersion version = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(createdDataset.getId());

        return version.getId();
    }

    private Map<String, Object> fetchSummary(long experimentId) {
        HttpHeaders headers = new HttpHeaders();

        headers.set("X-ByteChef-Workspace-Id", String.valueOf(WORKSPACE_ID));

        RestClient restClient = RestClient.create();

        return restClient
            .method(HttpMethod.GET)
            .uri(baseUrl() + "/api/ai-gateway/v1/experiments/" + experimentId)
            .headers(httpHeaders -> httpHeaders.putAll(headers))
            .retrieve()
            .toEntity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
            .getBody();
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private static AiGatewayChatCompletionResponse stubChatCompletionResponse() {
        return new AiGatewayChatCompletionResponse(
            "resp-1", "chat.completion", System.currentTimeMillis(), "gpt-4o", List.of(),
            new AiGatewayChatCompletionResponse.Usage(5, 5, 10));
    }
}
