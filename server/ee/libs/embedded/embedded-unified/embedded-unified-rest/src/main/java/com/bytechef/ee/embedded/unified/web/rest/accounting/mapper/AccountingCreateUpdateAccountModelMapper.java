/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest.accounting.mapper;

import com.bytechef.component.definition.unified.accounting.model.AccountUnifiedInputModel;
import com.bytechef.ee.embedded.unified.web.rest.accounting.model.CreateUpdateAccountModel;
import com.bytechef.ee.embedded.unified.web.rest.mapper.config.EmbeddedUnifiedMapperSpringConfig;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedUnifiedMapperSpringConfig.class)
public interface AccountingCreateUpdateAccountModelMapper
    extends Converter<CreateUpdateAccountModel, AccountUnifiedInputModel> {

    @Override
    AccountUnifiedInputModel convert(CreateUpdateAccountModel source);
}
