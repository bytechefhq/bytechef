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

package com.bytechef.platform.configuration.workflow.connection;

import com.bytechef.platform.component.registry.domain.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class WorkflowConnectionFactoryResolverChain implements WorkflowConnectionFactoryResolver {

    private final List<WorkflowConnectionFactoryResolver> workflowConnectionFactoryResolvers;

    @SuppressFBWarnings("EI")
    public WorkflowConnectionFactoryResolverChain(
        List<WorkflowConnectionFactoryResolver> workflowConnectionFactoryResolvers) {

        this.workflowConnectionFactoryResolvers = workflowConnectionFactoryResolvers;
    }

    @Override
    public Optional<WorkflowConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        for (WorkflowConnectionFactoryResolver workflowConnectionFactoryResolver : workflowConnectionFactoryResolvers) {
            Optional<WorkflowConnectionFactory> workflowConnectionFactoryOptional =
                workflowConnectionFactoryResolver.resolve(componentDefinition);

            if (workflowConnectionFactoryOptional.isPresent()) {
                return workflowConnectionFactoryOptional;
            }
        }

        return Optional.empty();
    }
}
