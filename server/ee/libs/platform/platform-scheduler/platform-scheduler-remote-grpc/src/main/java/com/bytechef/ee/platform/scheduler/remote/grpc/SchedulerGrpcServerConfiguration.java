/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.scheduler.remote.grpc;

import com.bytechef.platform.scheduler.ConnectionRefreshScheduler;
import com.bytechef.platform.scheduler.TriggerScheduler;
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
@ConditionalOnClass(name = "com.bytechef.platform.scheduler.QuartzTriggerScheduler")
public class SchedulerGrpcServerConfiguration {

    @Bean
    ConnectionRefreshSchedulerRemoteServiceGrpcServer connectionRefreshSchedulerRemoteServiceGrpcServer(
        ConnectionRefreshScheduler connectionRefreshScheduler) {

        return new ConnectionRefreshSchedulerRemoteServiceGrpcServer(connectionRefreshScheduler);
    }

    @Bean
    TriggerSchedulerRemoteServiceGrpcServer triggerSchedulerRemoteServiceGrpcServer(
        TriggerScheduler triggerScheduler, ObjectMapper objectMapper) {

        return new TriggerSchedulerRemoteServiceGrpcServer(triggerScheduler, objectMapper);
    }
}
