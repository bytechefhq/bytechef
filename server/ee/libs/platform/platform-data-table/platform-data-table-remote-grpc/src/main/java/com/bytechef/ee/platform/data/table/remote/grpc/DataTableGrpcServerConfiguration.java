/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.grpc;

import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
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
@ConditionalOnClass(name = "com.bytechef.platform.data.table.execution.service.DataTableRowServiceImpl")
public class DataTableGrpcServerConfiguration {

    @Bean
    DataTableRemoteServiceGrpcServer dataTableRemoteServiceGrpcServer(
        DataTableService dataTableService, DataTableRowService dataTableRowService,
        DataTableWebhookService dataTableWebhookService, ObjectMapper objectMapper) {

        return new DataTableRemoteServiceGrpcServer(
            dataTableService, dataTableRowService, dataTableWebhookService, new DataTableProtoMapper(objectMapper));
    }
}
