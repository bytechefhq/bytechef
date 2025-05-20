/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest.crm.mapper;

import com.bytechef.ee.embedded.unified.pagination.CursorPageRequest;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.ListAccountsPageableParameterModel;
import com.bytechef.ee.embedded.unified.web.rest.mapper.config.EmbeddedUnifiedMapperSpringConfig;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedUnifiedMapperSpringConfig.class)
public interface CrmListAccountsPageableParameterModelMapper
    extends Converter<ListAccountsPageableParameterModel, CursorPageRequest> {

    @Override
    CursorPageRequest convert(ListAccountsPageableParameterModel source);
}
