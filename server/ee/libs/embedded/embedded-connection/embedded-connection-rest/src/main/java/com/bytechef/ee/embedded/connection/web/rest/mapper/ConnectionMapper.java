/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connection.web.rest.mapper;

import com.bytechef.ee.embedded.connection.web.rest.mapper.config.EmbeddedConnectionMapperSpringConfig;
import com.bytechef.ee.embedded.connection.web.rest.model.ConnectionModel;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedConnectionMapperSpringConfig.class)
public interface ConnectionMapper extends Converter<ConnectionDTO, ConnectionModel> {

    @Override
    ConnectionModel convert(ConnectionDTO connectionDTO);
}
