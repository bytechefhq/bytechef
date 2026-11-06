/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.service;

import com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDataset;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalDatasetService {

    AiEvalDataset create(AiEvalDataset dataset);

    void archive(long id);

    AiEvalDataset getDataset(long id);

    AiEvalDataset update(AiEvalDataset dataset);
}
