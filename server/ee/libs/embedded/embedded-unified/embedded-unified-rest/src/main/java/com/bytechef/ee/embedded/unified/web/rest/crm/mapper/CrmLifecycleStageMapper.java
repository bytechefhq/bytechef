/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest.crm.mapper;

import com.bytechef.component.definition.unified.crm.model.common.LifecycleStage;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.LifecycleStageModel;
import com.bytechef.ee.embedded.unified.web.rest.mapper.config.EmbeddedUnifiedMapperSpringConfig;
import org.mapstruct.Mapper;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedUnifiedMapperSpringConfig.class)
public interface CrmLifecycleStageMapper extends Converter<LifecycleStage, LifecycleStageModel> {

    @Override
    LifecycleStageModel convert(LifecycleStage source);

    default JsonNullable<LifecycleStageModel> map(LifecycleStage value) {
        return JsonNullable.of(convert(value));
    }
}
