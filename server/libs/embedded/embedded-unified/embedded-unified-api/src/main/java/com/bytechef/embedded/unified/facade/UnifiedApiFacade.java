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

import com.bytechef.component.definition.UnifiedApiDefinition.Category;
import com.bytechef.component.definition.UnifiedApiDefinition.ModelType;
import com.bytechef.component.definition.unified.base.model.UnifiedInputModel;
import com.bytechef.component.definition.unified.base.model.UnifiedOutputModel;
import com.bytechef.embedded.unified.pagination.CursorPageSlice;
import com.bytechef.embedded.unified.pagination.CursorPageable;
import com.bytechef.platform.connection.domain.ConnectionEnvironment;

public interface UnifiedApiFacade {

    String create(
        UnifiedInputModel unifiedInputModel, Category category, ModelType modelType, ConnectionEnvironment environment,
        long connectionId);

    void delete(
        String id, Category category, ModelType modelType, ConnectionEnvironment environment, long connectionId);

    UnifiedOutputModel get(
        String id, Category category, ModelType modelType, ConnectionEnvironment environment, long connectionId);

    CursorPageSlice<? extends UnifiedOutputModel> getPage(
        CursorPageable cursorPageable, Category category, ModelType modelType, ConnectionEnvironment environment,
        long connectionId);

    void update(
        String id, UnifiedInputModel unifiedInputModel, Category category, ModelType modelType,
        ConnectionEnvironment environment, long connectionId);
}
