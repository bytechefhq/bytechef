
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.web.rest.mapper;

import com.bytechef.athena.configuration.dto.IntegrationDTO;
import com.bytechef.athena.configuration.web.rest.mapper.config.IntegrationMapperSpringConfiguration;
import com.bytechef.athena.configuration.web.rest.model.IntegrationModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = IntegrationMapperSpringConfiguration.class)
public interface IntegrationModelMapper extends Converter<IntegrationModel, IntegrationDTO> {

    @Override
    IntegrationDTO convert(IntegrationModel integrationModel);
}
