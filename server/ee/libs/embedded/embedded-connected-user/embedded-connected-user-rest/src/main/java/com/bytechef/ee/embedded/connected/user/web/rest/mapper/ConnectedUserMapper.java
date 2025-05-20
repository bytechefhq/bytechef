/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.web.rest.mapper;

import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.ee.embedded.connected.user.web.rest.mapper.config.EmbeddedConnectedUserMapperSpringConfig;
import com.bytechef.ee.embedded.connected.user.web.rest.model.ConnectedUserModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedConnectedUserMapperSpringConfig.class)
public interface ConnectedUserMapper extends Converter<ConnectedUserDTO, ConnectedUserModel> {

    @Override
    ConnectedUserModel convert(ConnectedUserDTO connectedUserDTO);
}
