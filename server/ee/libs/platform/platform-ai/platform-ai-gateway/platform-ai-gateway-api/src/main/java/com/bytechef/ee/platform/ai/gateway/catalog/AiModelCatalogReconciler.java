/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.catalog;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.util.Collection;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiModelCatalogReconciler {

    Set<Long> catalogManagedModelIds(Collection<AiModel> models);

    void reconcile();
}
