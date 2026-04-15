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

package com.bytechef.platform.connection.config;

import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assembles the {@link ResourceVisibilityPolicyRegistry} from every {@link ResourceVisibilityPolicy} on the classpath.
 *
 * <p>
 * The registry itself lives in {@code platform-api}, which carries no Spring stereotypes and is not component-scanned,
 * so it needs an explicit {@code @Bean} somewhere that is. This module is that somewhere because it owns the only
 * policy today. When a second resource family gains visibility and can be deployed without connections, move this
 * declaration to a module both depend on rather than adding a second registry.
 *
 * @author Ivica Cardic
 */
@Configuration
public class ResourceVisibilityConfiguration {

    /**
     * Injecting {@code List<ResourceVisibilityPolicy>} yields an empty list when no module contributes one, which the
     * registry accepts — an application with no visibility-bearing resources gets an empty registry rather than a
     * startup failure.
     */
    @Bean
    ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry(
        List<ResourceVisibilityPolicy> resourceVisibilityPolicies) {

        return new ResourceVisibilityPolicyRegistry(resourceVisibilityPolicies);
    }
}
