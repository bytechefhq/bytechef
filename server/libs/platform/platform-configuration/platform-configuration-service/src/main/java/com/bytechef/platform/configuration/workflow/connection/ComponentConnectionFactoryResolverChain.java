/*
 * Copyright 2025 ByteChef
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

import com.bytechef.platform.component.domain.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
class ComponentConnectionFactoryResolverChain implements ComponentConnectionFactoryResolver {

    private final List<ComponentConnectionFactoryResolver> componentConnectionFactoryResolvers;

    @SuppressFBWarnings("EI")
    public ComponentConnectionFactoryResolverChain(
        List<ComponentConnectionFactoryResolver> componentConnectionFactoryResolvers) {

        this.componentConnectionFactoryResolvers = componentConnectionFactoryResolvers;
    }

    @Override
    public Optional<ComponentConnectionFactory> resolve(ComponentDefinition componentDefinition) {
        for (ComponentConnectionFactoryResolver componentConnectionFactoryResolver : componentConnectionFactoryResolvers) {
            Optional<ComponentConnectionFactory> workflowConnectionFactoryOptional =
                componentConnectionFactoryResolver.resolve(componentDefinition);

            if (workflowConnectionFactoryOptional.isPresent()) {
                return workflowConnectionFactoryOptional;
            }
        }

        return Optional.empty();
    }
}
