
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

package com.bytechef.atlas.repository.config;

import com.bytechef.atlas.repository.workflow.mapper.JsonWorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapper;
import com.bytechef.atlas.repository.workflow.mapper.WorkflowMapperChain;
import com.bytechef.atlas.repository.workflow.mapper.YamlWorkflowMapper;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Ivica Cardic
 */
@Configuration
public class WorkflowRepositoryConfiguration {

    @Bean
    @Primary
    WorkflowMapper workflowMapper() {
        return new WorkflowMapperChain(List.of(jsonWorkflowMapper(), yamlWorkflowMapper()));
    }

    @Bean
    JsonWorkflowMapper jsonWorkflowMapper() {
        return new JsonWorkflowMapper();
    }

    @Bean
    YamlWorkflowMapper yamlWorkflowMapper() {
        return new YamlWorkflowMapper();
    }
}
