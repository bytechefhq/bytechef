/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.notification.remote.grpc;

import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.platform.notification.service.NotificationServiceImpl")
public class NotificationGrpcServerConfiguration {

    @Bean
    NotificationRemoteServiceGrpcServer notificationRemoteServiceGrpcServer(
        NotificationService notificationService, NotificationSenderRegistry notificationSenderRegistry,
        ObjectMapper objectMapper) {

        return new NotificationRemoteServiceGrpcServer(
            notificationService, notificationSenderRegistry, new NotificationProtoMapper(objectMapper));
    }
}
