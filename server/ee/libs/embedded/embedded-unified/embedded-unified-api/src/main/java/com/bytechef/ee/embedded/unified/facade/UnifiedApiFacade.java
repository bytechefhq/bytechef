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
        String externalUserId, UnifiedInputModel unifiedInputModel, UnifiedApiCategory category,
        Long integrationInstanceId, Environment environment, ModelType modelType);

    void delete(
        String externalUserId, String id, UnifiedApiCategory category, Long integrationInstanceId,
        Environment environment, ModelType modelType);

    UnifiedOutputModel get(
        String externalUserId, String id, UnifiedApiCategory category, Long integrationInstanceId,
        Environment environment, ModelType modelType);

    CursorPageSlice<? extends UnifiedOutputModel> getPage(
        String externalUserId, CursorPageable cursorPageable, UnifiedApiCategory category, Long integrationInstanceId,
        Environment environment, ModelType modelType);

    void update(
        String externalUserId, String id, UnifiedInputModel unifiedInputModel, UnifiedApiCategory category,
        Long integrationInstanceId, Environment environment, ModelType modelType);
}
