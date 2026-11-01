/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.remote.grpc;

import com.bytechef.ee.platform.audit.facade.AuditEventFacade;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnClass(name = "com.bytechef.ee.platform.audit.service.AuditEventServiceImpl")
public class AuditGrpcServerConfiguration {

    @Bean
    AuditEventRemoteServiceGrpcServer auditEventRemoteServiceGrpcServer(
        AuditEventFacade auditEventFacade, AuditEventService auditEventService,
        AuditEventRepository auditEventRepository) {

        return new AuditEventRemoteServiceGrpcServer(
            auditEventFacade, auditEventService, auditEventRepository, new AuditEventProtoMapper());
    }
}
