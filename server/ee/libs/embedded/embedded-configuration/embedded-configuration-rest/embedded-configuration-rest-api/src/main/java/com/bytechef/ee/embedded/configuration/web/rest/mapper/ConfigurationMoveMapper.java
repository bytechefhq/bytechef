/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveImpactDTO;
import com.bytechef.ee.embedded.configuration.dto.ConfigurationMoveResultDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationConnectedUserDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConfigurationConnectedUserModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConfigurationMoveImpactModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConfigurationMoveResultModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ConfigurationMoveMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface ConfigurationConnectedUserDTOToConfigurationConnectedUserModelMapper
        extends Converter<IntegrationInstanceConfigurationConnectedUserDTO, ConfigurationConnectedUserModel> {

        @Override
        ConfigurationConnectedUserModel convert(
            IntegrationInstanceConfigurationConnectedUserDTO integrationInstanceConfigurationConnectedUserDTO);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface ConfigurationMoveImpactDTOToConfigurationMoveImpactModelMapper
        extends Converter<ConfigurationMoveImpactDTO, ConfigurationMoveImpactModel> {

        @Override
        ConfigurationMoveImpactModel convert(ConfigurationMoveImpactDTO configurationMoveImpactDTO);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface ConfigurationMoveResultDTOToConfigurationMoveResultModelMapper
        extends Converter<ConfigurationMoveResultDTO, ConfigurationMoveResultModel> {

        @Override
        ConfigurationMoveResultModel convert(ConfigurationMoveResultDTO configurationMoveResultDTO);
    }
}
