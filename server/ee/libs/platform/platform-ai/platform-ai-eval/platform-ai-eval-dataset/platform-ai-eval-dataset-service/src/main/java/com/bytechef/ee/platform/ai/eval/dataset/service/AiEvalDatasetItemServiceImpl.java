/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion;
import com.bytechef.ee.platform.ai.eval.dataset.repository.AiEvalDatasetItemRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class AiEvalDatasetItemServiceImpl implements AiEvalDatasetItemService {

    private final AiEvalDatasetItemRepository aiEvalDatasetItemRepository;
    private final AiEvalDatasetVersionService aiEvalDatasetVersionService;

    AiEvalDatasetItemServiceImpl(
        AiEvalDatasetItemRepository aiEvalDatasetItemRepository,
        AiEvalDatasetVersionService aiEvalDatasetVersionService) {

        this.aiEvalDatasetItemRepository = aiEvalDatasetItemRepository;
        this.aiEvalDatasetVersionService = aiEvalDatasetVersionService;
    }

    @Override
    public AiEvalDatasetItem addItem(long datasetId, String input, String expectedOutput, String metadata) {
        AiEvalDatasetVersion datasetVersion = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        AiEvalDatasetItem datasetItem = AiEvalDatasetItem.newItem(
            datasetId, datasetVersion.getId(), input, expectedOutput, metadata);

        return aiEvalDatasetItemRepository.save(datasetItem);
    }

    @Override
    public List<AiEvalDatasetItem> addItems(long datasetId, List<AddItem> items) {
        Validate.notNull(items, "items must not be null");

        if (items.isEmpty()) {
            return List.of();
        }

        // Resolve the unfrozen version ONCE for the whole batch — re-resolving per item would churn needlessly and
        // could surface the auto-create as a burst of empty versions if the caller were racing with freeze().
        AiEvalDatasetVersion datasetVersion = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        List<AiEvalDatasetItem> datasetItems = new ArrayList<>(items.size());

        for (AddItem item : items) {
            AiEvalDatasetItem datasetItem = AiEvalDatasetItem.newItem(
                datasetId, datasetVersion.getId(), item.input(), item.expectedOutput(), item.metadata());

            datasetItems.add(datasetItem);
        }

        return aiEvalDatasetItemRepository.saveAll(datasetItems);
    }

    @Override
    public AiEvalDatasetItem addItemFromTrace(
        long datasetId, long traceId, String input, String expectedOutput, String metadata) {

        AiEvalDatasetVersion datasetVersion = aiEvalDatasetVersionService.getOrCreateUnfrozenVersion(datasetId);

        // Build manually (not via addItem) so sourceTraceId is persisted in a single save — avoids a second write and
        // keeps the provenance link atomic with the item insertion. The caller (REST/GraphQL controller) has already
        // verified the trace's workspace matches the dataset's workspace; this method does not re-validate.
        AiEvalDatasetItem datasetItem = AiEvalDatasetItem.newItem(
            datasetId, datasetVersion.getId(), input, expectedOutput, metadata);

        datasetItem.linkSourceTrace(traceId);

        return aiEvalDatasetItemRepository.save(datasetItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiEvalDatasetItem> getItemsByVersion(Long versionId) {
        return aiEvalDatasetItemRepository.findAllByDatasetVersionId(versionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByVersion(Long versionId) {
        return aiEvalDatasetItemRepository.countByDatasetVersionId(versionId);
    }
}
