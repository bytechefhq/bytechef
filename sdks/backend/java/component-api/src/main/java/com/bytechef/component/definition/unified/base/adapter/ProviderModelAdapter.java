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

package com.bytechef.component.definition.unified.base.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * Adapter for converting provider-specific input and output models to unified input and output models.
 *
 * @param <PI>
 * @param <PO>
 *
 * @author Ivica Cardic
 */
public interface ProviderModelAdapter<PI extends ProviderInputModel, PO extends ProviderOutputModel> {

    String create(PI inputModel, Parameters connectionParameters, Context context);

    void delete(String id, Parameters connectionParameters, Context context);

    PO get(String id, Parameters connectionParameters, Context context);

    UnifiedApiDefinition.ModelType getModelType();

    Page<PO> getPage(Parameters connectionParameters, Parameters cursorParameters, Context context);

    void update(String id, PI inputModel, Parameters connectionParameters, Context context);

    @SuppressFBWarnings("EI")
    record Page<PO>(List<PO> content, int size, Map<String, ?> cursorParameters) {
    }
}
