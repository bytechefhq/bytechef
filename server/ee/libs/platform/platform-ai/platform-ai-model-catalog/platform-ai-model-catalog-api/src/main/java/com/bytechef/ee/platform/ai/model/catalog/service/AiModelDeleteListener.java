/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.model.catalog.service;

/**
 * Callback invoked inside {@link AiModelService#delete(long)}'s transaction before the row is removed. Modules that
 * hang their own rows off a model id — the AI Gateway's model deployments, for instance — contribute a bean so the
 * catalog can cascade without depending on them.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiModelDeleteListener {

    void beforeDelete(long modelId);
}
