/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.skill.remote.grpc;

import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
public class AiSkillGrpcServerConfiguration {

    @Bean
    AiSkillFacadeRemoteServiceGrpcServer aiSkillFacadeRemoteServiceGrpcServer(
        AiSkillFacade aiSkillFacade, ObjectMapper objectMapper) {

        return new AiSkillFacadeRemoteServiceGrpcServer(aiSkillFacade, new AiSkillProtoMapper(objectMapper));
    }
}
