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

package com.bytechef.component.definition;

import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface UnifiedApiDefinition {

    /**
     *
     */
    enum Category {
        ACCOUNTING, ATS, CRM, E_COMMERCE, FILE_STORAGE, HRIS, MARKETING_AUTOMATION, TICKETING
    }

    /**
     *
     * @return
     */
    Category getCategory();

    /**
     *
     * @return
     */
    List<? extends ProviderModelAdapter<?, ?>> getProviderAdapters();

    /**
     *
     * @return
     */
    List<? extends ProviderModelMapper<?, ?, ?, ?>> getProviderMappers();

    interface ModelType {
    }
}
