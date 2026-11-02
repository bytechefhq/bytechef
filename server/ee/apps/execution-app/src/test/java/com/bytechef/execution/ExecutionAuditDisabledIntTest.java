/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.execution;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.config.testcontainers.RedisContainerConfiguration;
import io.grpc.BindableService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Import({
    PostgreSQLContainerConfiguration.class, RedisContainerConfiguration.class
})
@SpringBootTest(properties = "bytechef.audit.enabled=false")
class ExecutionAuditDisabledIntTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testAuditGrpcServiceIsServedWithoutRecordingEvents() {
        List<String> serviceNames = applicationContext.getBeansOfType(BindableService.class)
            .values()
            .stream()
            .map(bindableService -> bindableService.bindService()
                .getServiceDescriptor()
                .getName())
            .toList();

        assertThat(serviceNames).contains("bytechef.platform.audit.remote.v1.AuditEventRemoteService");
        assertThat(applicationContext.getBeanNamesForType(AuditEventRepository.class)).isEmpty();
    }
}
