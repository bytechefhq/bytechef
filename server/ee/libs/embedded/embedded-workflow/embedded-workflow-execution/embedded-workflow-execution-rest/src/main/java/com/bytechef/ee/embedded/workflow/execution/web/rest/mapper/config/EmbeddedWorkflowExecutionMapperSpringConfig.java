/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.workflow.execution.web.rest.mapper.config;

import com.bytechef.ee.embedded.configuration.web.rest.adapter.EmbeddedConfigurationConversionServiceAdapter;
import com.bytechef.ee.embedded.workflow.execution.web.rest.adapter.EmbeddedWorkflowExecutionConversionServiceAdapter;
import com.bytechef.platform.configuration.web.rest.adapter.PlatformConfigurationConversionServiceAdapter;
import com.bytechef.platform.workflow.execution.web.rest.adapter.PlatformWorkflowExecutionConversionServiceAdapter;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    DateTimeMapper.class, EmbeddedConfigurationConversionServiceAdapter.class,
    EmbeddedWorkflowExecutionConversionServiceAdapter.class, PlatformConfigurationConversionServiceAdapter.class,
    PlatformWorkflowExecutionConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.embedded.workflow.execution.web.rest.adapter",
    conversionServiceAdapterClassName = "EmbeddedWorkflowExecutionConversionServiceAdapter")
public interface EmbeddedWorkflowExecutionMapperSpringConfig {
}
