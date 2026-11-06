/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.dataset.public_.web.rest;

import com.bytechef.ee.automation.ai.eval.dataset.dto.AddItemRequest;
import com.bytechef.ee.automation.ai.eval.dataset.dto.BulkAddItemsRequest;
import com.bytechef.ee.automation.ai.eval.dataset.dto.CreateDatasetRequest;
import com.bytechef.ee.automation.ai.eval.dataset.dto.DatasetView;
import com.bytechef.ee.automation.ai.eval.dataset.dto.ItemView;
import com.bytechef.ee.automation.ai.eval.dataset.service.WorkspaceAiEvalDatasetService;
import com.bytechef.ee.automation.ai.gateway.public_.workspace.AiGatewayWorkspaceHeaderResolver;
import com.bytechef.ee.automation.ai.gateway.ratelimit.AiGatewayRateLimitChecker;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetItemService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetService;
import com.bytechef.ee.platform.ai.eval.dataset.service.AiEvalDatasetVersionService;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayRateLimitResult;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetNotFoundException;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreTargetType;
import com.bytechef.ee.platform.ai.gateway.exception.AiScoreWorkspaceBoundaryException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 * @version ee
 */
@RestController
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@RequestMapping("/api/ai-gateway/v1/datasets")
@SuppressFBWarnings("EI")
class AiEvalDatasetController {

    private final AiEvalDatasetService aiEvalDatasetService;
    private final WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService;
    private final AiEvalDatasetVersionService aiEvalDatasetVersionService;
    private final AiEvalDatasetItemService aiEvalDatasetItemService;
    private final AiGatewayRateLimitChecker aiGatewayRateLimitChecker;
    private final AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver;
    private final ObjectMapper objectMapper;

    AiEvalDatasetController(
        AiEvalDatasetService aiEvalDatasetService,
        WorkspaceAiEvalDatasetService workspaceAiEvalDatasetService,
        AiEvalDatasetVersionService aiEvalDatasetVersionService,
        AiEvalDatasetItemService aiEvalDatasetItemService,
        @Nullable AiGatewayRateLimitChecker aiGatewayRateLimitChecker,
        AiGatewayWorkspaceHeaderResolver workspaceHeaderResolver,
        ObjectMapper objectMapper) {

        this.aiEvalDatasetService = aiEvalDatasetService;
        this.workspaceAiEvalDatasetService = workspaceAiEvalDatasetService;
        this.aiEvalDatasetVersionService = aiEvalDatasetVersionService;
        this.aiEvalDatasetItemService = aiEvalDatasetItemService;
        this.aiGatewayRateLimitChecker = aiGatewayRateLimitChecker;
        this.workspaceHeaderResolver = workspaceHeaderResolver;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Object> create(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @RequestBody CreateDatasetRequest request) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        AiEvalDataset dataset = new AiEvalDataset(request.name());

        dataset.setDescription(request.description());
        dataset.setTags(serializeTags(request.tags()));

        AiEvalDataset created = workspaceAiEvalDatasetService.createInWorkspace(dataset, workspaceId);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DatasetView.from(created, workspaceId));
    }

    @GetMapping
    public ResponseEntity<Object> list(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        List<DatasetView> views = workspaceAiEvalDatasetService.findAllByWorkspace(workspaceId)
            .stream()
            .map(dataset -> DatasetView.from(dataset, workspaceId))
            .toList();

        return ResponseEntity.ok(views);
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<Object> addItem(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("id") Long datasetId,
        @RequestBody AddItemRequest request) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        assertSameWorkspace(workspaceId, requireDataset(workspaceId, datasetId));

        AiEvalDatasetItem item = aiEvalDatasetItemService.addItem(
            datasetId,
            serializeJson(request.input()),
            serializeJson(request.expectedOutput()),
            serializeMetadata(request.metadata()));

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ItemView.from(item));
    }

    @PostMapping("/{id}/items/bulk")
    public ResponseEntity<Object> addItemsBulk(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("id") Long datasetId,
        @RequestBody BulkAddItemsRequest request) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        assertSameWorkspace(workspaceId, requireDataset(workspaceId, datasetId));

        List<AiEvalDatasetItemService.AddItem> addItems = request.items()
            .stream()
            .map(addItemRequest -> new AiEvalDatasetItemService.AddItem(
                serializeJson(addItemRequest.input()),
                serializeJson(addItemRequest.expectedOutput()),
                serializeMetadata(addItemRequest.metadata())))
            .toList();

        List<AiEvalDatasetItem> created = aiEvalDatasetItemService.addItems(datasetId, addItems);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "createdCount", created.size(),
                "ids", created.stream()
                    .map(AiEvalDatasetItem::getId)
                    .toList()));
    }

    @PostMapping("/{id}/versions/{versionId}/freeze")
    public ResponseEntity<Object> freezeVersion(
        @RequestHeader(name = "X-ByteChef-Workspace-Id", required = false) String workspaceHeader,
        @PathVariable("id") Long datasetId,
        @PathVariable("versionId") Long versionId) {

        Long workspaceId = workspaceHeaderResolver.resolveAndVerify(workspaceHeader);

        if (workspaceId == null) {
            return missingWorkspaceResponse();
        }

        ResponseEntity<Object> rateLimitResponse = checkRateLimit(workspaceId);

        if (rateLimitResponse != null) {
            return rateLimitResponse;
        }

        assertSameWorkspace(workspaceId, requireDataset(workspaceId, datasetId));

        AiEvalDatasetVersion version = requireDatasetVersion(workspaceId, versionId);

        if (!datasetId.equals(version.getDatasetId())) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                workspaceId, AiScoreTargetType.DATASET_VERSION, versionId);
        }

        AiEvalDatasetVersion frozen = aiEvalDatasetVersionService.freeze(versionId);

        return ResponseEntity.ok(Map.of(
            "versionId", frozen.getId(),
            "frozen", true));
    }

    private static ResponseEntity<Object> missingWorkspaceResponse() {
        return ResponseEntity.badRequest()
            .body(Map.of(
                "error", Map.of(
                    "message", "Request must include header 'X-ByteChef-Workspace-Id' with a numeric workspace id.",
                    "type", "missing_workspace_id")));
    }

    @Nullable
    private ResponseEntity<Object> checkRateLimit(Long workspaceId) {
        if (aiGatewayRateLimitChecker == null) {
            return null;
        }

        AiGatewayRateLimitResult result = aiGatewayRateLimitChecker.checkWorkspaceRequest(workspaceId, "datasets");

        if (!result.allowed()) {
            long retryAfterSeconds = Math.max(0L, (result.resetAtEpochMs() - System.currentTimeMillis()) / 1000L);

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(retryAfterSeconds))
                .body(Map.of("error", Map.of(
                    "type", "rate_limit_exceeded",
                    "message", "Rate limit exceeded; retry after " + retryAfterSeconds + " seconds.")));
        }

        return null;
    }

    private AiEvalDataset requireDataset(Long workspaceId, Long id) {
        try {
            return aiEvalDatasetService.getDataset(id);
        } catch (IllegalArgumentException notFound) {
            throw AiScoreTargetNotFoundException.forTarget(workspaceId, AiScoreTargetType.DATASET, id, notFound);
        }
    }

    private AiEvalDatasetVersion requireDatasetVersion(Long workspaceId, Long id) {
        try {
            return aiEvalDatasetVersionService.getVersion(id);
        } catch (IllegalArgumentException notFound) {
            throw AiScoreTargetNotFoundException.forTarget(
                workspaceId, AiScoreTargetType.DATASET_VERSION, id, notFound);
        }
    }

    private void assertSameWorkspace(Long callerWorkspaceId, AiEvalDataset dataset) {
        Long datasetWorkspaceId = workspaceAiEvalDatasetService.getWorkspaceId(dataset.getId());

        if (!callerWorkspaceId.equals(datasetWorkspaceId)) {
            throw AiScoreWorkspaceBoundaryException.forTarget(
                callerWorkspaceId, AiScoreTargetType.DATASET, dataset.getId());
        }
    }

    private String serializeJson(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String string) {
            return string;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Value is not serializable as JSON", exception);
        }
    }

    private String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Tags are not serializable as JSON", exception);
        }
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Metadata is not serializable as JSON", exception);
        }
    }
}
