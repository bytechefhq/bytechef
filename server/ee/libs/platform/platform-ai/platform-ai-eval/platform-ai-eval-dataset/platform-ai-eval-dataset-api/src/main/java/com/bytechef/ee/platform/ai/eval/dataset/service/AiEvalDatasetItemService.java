/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetItem;
import java.util.List;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetItemService {

    AiEvalDatasetItem addItem(long datasetId, String input, String expectedOutput, String metadata);

    List<AiEvalDatasetItem> addItems(long datasetId, List<AddItem> items);

    AiEvalDatasetItem addItemFromTrace(
        long datasetId, long traceId, String input, String expectedOutput, String metadata);

    List<AiEvalDatasetItem> getItemsByVersion(Long versionId);

    long countByVersion(Long versionId);

    record AddItem(String input, String expectedOutput, String metadata) {
    }
}
