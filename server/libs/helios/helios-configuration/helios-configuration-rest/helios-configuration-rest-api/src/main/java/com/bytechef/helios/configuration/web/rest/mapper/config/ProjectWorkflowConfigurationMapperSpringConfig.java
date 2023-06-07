
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.configuration.web.rest.mapper.config;

import com.bytechef.hermes.configuration.web.rest.adapter.WorkflowConfigurationConversionServiceAdapter;
import com.bytechef.category.web.rest.mapper.adapter.CategoryConversionServiceAdapter;
import com.bytechef.tag.web.rest.mapper.adapter.TagConversionServiceAdapter;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    CategoryConversionServiceAdapter.class, TagConversionServiceAdapter.class,
    com.bytechef.helios.configuration.web.rest.adapter.ProjectWorkflowConfigurationConversionServiceAdapter.class,
    WorkflowConfigurationConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.helios.configuration.web.rest.adapter",
    conversionServiceAdapterClassName = "ProjectWorkflowConfigurationConversionServiceAdapter")
public interface ProjectWorkflowConfigurationMapperSpringConfig {
}
