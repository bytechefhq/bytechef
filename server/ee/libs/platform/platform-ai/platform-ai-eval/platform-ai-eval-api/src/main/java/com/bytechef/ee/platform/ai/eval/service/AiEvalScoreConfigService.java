/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.service;

import com.bytechef.ee.platform.ai.eval.domain.AiEvalScoreConfig;

/**
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalScoreConfigService {

    AiEvalScoreConfig create(AiEvalScoreConfig scoreConfig);

    void delete(long id);

    AiEvalScoreConfig getScoreConfig(long id);

    AiEvalScoreConfig update(AiEvalScoreConfig scoreConfig);
}
