/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.facade;

import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.ee.embedded.unified.pagination.CursorPageSlice;
import com.bytechef.ee.embedded.unified.pagination.CursorPageable;
import com.bytechef.platform.constant.Environment;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface UnifiedApiFacade {

    String create(
        UnifiedInputModel unifiedInputModel, UnifiedApiCategory category, ModelType modelType, Environment environment,
        Long integrationInstanceId);

    void delete(
        String id, UnifiedApiCategory category, ModelType modelType, Environment environment,
        Long integrationInstanceId);

    UnifiedOutputModel get(
        String id, UnifiedApiCategory category, ModelType modelType, Environment environment,
        Long integrationInstanceId);

    CursorPageSlice<? extends UnifiedOutputModel> getPage(
        CursorPageable cursorPageable, UnifiedApiCategory category, ModelType modelType, Environment environment,
        Long integrationInstanceId);

    void update(
        String id, UnifiedInputModel unifiedInputModel, UnifiedApiCategory category, ModelType modelType,
        Environment environment, Long integrationInstanceId);
}
