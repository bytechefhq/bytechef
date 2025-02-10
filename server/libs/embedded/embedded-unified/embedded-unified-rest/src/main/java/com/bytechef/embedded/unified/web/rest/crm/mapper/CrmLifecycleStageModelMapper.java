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

package com.bytechef.embedded.unified.web.rest.crm.mapper;

import com.bytechef.component.definition.unified.crm.model.common.LifecycleStage;
import com.bytechef.embedded.unified.web.rest.crm.model.LifecycleStageModel;
import com.bytechef.embedded.unified.web.rest.mapper.config.EmbeddedUnifiedMapperSpringConfig;
import org.mapstruct.Mapper;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = EmbeddedUnifiedMapperSpringConfig.class)
public interface CrmLifecycleStageModelMapper extends Converter<LifecycleStageModel, LifecycleStage> {

    @Override
    LifecycleStage convert(LifecycleStageModel source);

    default LifecycleStage mapLifecycleStageModel(JsonNullable<LifecycleStageModel> value) {
        return convert(value.get());
    }
}
