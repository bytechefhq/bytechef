/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
public interface ConnectionMapper extends Converter<ConnectionDTO, ConnectionModel> {

    @Override
    ConnectionModel convert(ConnectionDTO connectionDTO);

    // status (connection lifecycle) and visibility are server-assigned, never supplied by an embedded
    // create request. Ignore them so ConnectionDTO.Builder's safe defaults (ACTIVE / PRIVATE) stand;
    // otherwise the generated mapper would call .status(null)/.visibility(null), overriding the
    // defaults and tripping the canonical constructor's requireNonNull guard.
    @InheritInverseConfiguration
    @DelegatingConverter
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "visibility", ignore = true)
    ConnectionDTO invertConvert(ConnectionModel connectionModel);
}
