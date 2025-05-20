/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest.accounting.mapper;

import com.bytechef.component.definition.unified.accounting.model.AccountUnifiedOutputModel;
import com.bytechef.ee.embedded.unified.web.rest.accounting.model.AccountModel;
import com.bytechef.ee.embedded.unified.web.rest.mapper.config.EmbeddedUnifiedMapperSpringConfig;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedUnifiedMapperSpringConfig.class)
public interface AccountingAccountUnifiedOutputModelMapper extends Converter<AccountUnifiedOutputModel, AccountModel> {

    @Override
    AccountModel convert(AccountUnifiedOutputModel source);
}
