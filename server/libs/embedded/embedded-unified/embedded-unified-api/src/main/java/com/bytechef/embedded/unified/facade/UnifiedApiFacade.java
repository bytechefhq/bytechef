/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.unified.facade;

import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.UnifiedApiDefinition.UnifiedApiCategory;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.embedded.unified.pagination.CursorPageSlice;
import com.bytechef.embedded.unified.pagination.CursorPageable;
import com.bytechef.platform.constant.Environment;

/**
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
