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

package com.bytechef.platform.configuration.config;

import com.bytechef.platform.configuration.context.EnvironmentContextThreadLocalAccessor;
import com.bytechef.tenant.TenantContextThreadLocalAccessor;
import io.micrometer.context.ContextRegistry;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Registers the {@link EnvironmentContextThreadLocalAccessor} and {@link TenantContextThreadLocalAccessor} in the
 * global {@link ContextRegistry} and enables Reactor's automatic context propagation, so the environment and tenant
 * bound on the calling thread survive reactive thread hops. Spring AI schedules advisor chains (RAG similarity search,
 * embedding, chat model resolution) on {@code Schedulers.boundedElastic()}; without propagation those threads see an
 * unset {@code EnvironmentContext} (falling back to {@code PRODUCTION}) and the default tenant, so the catalog-backed
 * models resolve the wrong environment's / tenant's provider.
 *
 * @author Ivica Cardic
 */
@Configuration
class ReactorContextPropagationConfiguration {

    ReactorContextPropagationConfiguration() {
        ContextRegistry contextRegistry = ContextRegistry.getInstance();

        contextRegistry.registerThreadLocalAccessor(new EnvironmentContextThreadLocalAccessor());
        contextRegistry.registerThreadLocalAccessor(new TenantContextThreadLocalAccessor());

        Hooks.enableAutomaticContextPropagation();
    }
}
