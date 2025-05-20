/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest.crm.mapper;

import com.bytechef.component.definition.unified.crm.model.AccountUnifiedOutputModel;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.AccountModel;
import com.bytechef.ee.embedded.unified.web.rest.mapper.config.EmbeddedUnifiedMapperSpringConfig;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedUnifiedMapperSpringConfig.class, uses = {
    CrmLifecycleStageMapper.class
})
public interface CrmAccountUnifiedOutputModelMapper extends Converter<AccountUnifiedOutputModel, AccountModel> {

    @Override
    AccountModel convert(AccountUnifiedOutputModel source);
}
