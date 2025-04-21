/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.web.rest.mapper;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.dto.ApiConnectorDTO;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.mapper.config.EePlatformApiConnectorConfigurationMapperSpringConfig;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.ApiConnectorModel;
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
public class ApiConnectorMapper {

    @Mapper(config = EePlatformApiConnectorConfigurationMapperSpringConfig.class)
    public interface ApiConnectorToApiConnectorModelMapper extends Converter<ApiConnector, ApiConnectorModel> {

        @Override
        @Mapping(target = "definition", ignore = true)
        @Mapping(target = "endpoints", ignore = true)
        @Mapping(target = "specification", ignore = true)
        @Mapping(target = "tags", ignore = true)
        ApiConnectorModel convert(ApiConnector apiConnector);

        @InheritInverseConfiguration
        @DelegatingConverter
        @Mapping(target = "definition", ignore = true)
        @Mapping(target = "specification", ignore = true)
        ApiConnector invertConvert(ApiConnectorModel apiConnectorModel);
    }

    @Mapper(config = EePlatformApiConnectorConfigurationMapperSpringConfig.class)
    public interface ApiConnectorDTOToApiConnectorModelMapper extends Converter<ApiConnectorDTO, ApiConnectorModel> {

        @Override
        @Mapping(target = "tags", ignore = true)
        ApiConnectorModel convert(ApiConnectorDTO apiConnectorDTO);

        @InheritInverseConfiguration
        @DelegatingConverter
        ApiConnectorDTO invertConvert(ApiConnectorModel apiConnectorModel);
    }
}
