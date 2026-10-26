/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.model.catalog.service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiModelDeleteListener {

    void beforeDelete(long modelId);
}
