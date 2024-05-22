/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.workflow.execution.web.rest.mapper.config;

import com.bytechef.embedded.configuration.web.rest.adapter.IntegrationConfigurationConversionServiceAdapter;
import com.bytechef.embedded.workflow.execution.web.rest.adapter.IntegrationWorkflowExecutionConversionServiceAdapter;
import com.bytechef.platform.configuration.web.rest.adapter.PlatformConfigurationConversionServiceAdapter;
import com.bytechef.platform.workflow.execution.web.rest.adapter.PlatformWorkflowExecutionConversionServiceAdapter;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    IntegrationConfigurationConversionServiceAdapter.class, IntegrationWorkflowExecutionConversionServiceAdapter.class,
    PlatformConfigurationConversionServiceAdapter.class, PlatformWorkflowExecutionConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.embedded.workflow.execution.web.rest.adapter",
    conversionServiceAdapterClassName = "IntegrationWorkflowExecutionConversionServiceAdapter")
public interface IntegrationWorkflowExecutionMapperSpringConfig {
}
