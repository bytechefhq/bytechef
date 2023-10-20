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

package com.bytechef.hermes.descriptor.resolver;

import com.bytechef.hermes.descriptor.domain.AuthenticationDescriptors;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandlerResolver;
import java.util.List;
import java.util.Objects;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Order(1)
public class InMemoryAuthenticationDescriptorHandlerResolver implements AuthenticationDescriptorHandlerResolver {

    private final List<AuthenticationDescriptorHandler> authenticationDescriptorHandlers;

    public InMemoryAuthenticationDescriptorHandlerResolver(
            List<AuthenticationDescriptorHandler> authenticationDescriptorHandlers) {
        this.authenticationDescriptorHandlers = authenticationDescriptorHandlers;
    }

    @Override
    public List<AuthenticationDescriptorHandler> getAuthenticationDescriptorHandlers() {
        return authenticationDescriptorHandlers;
    }

    @Override
    public AuthenticationDescriptorHandler resolve(String taskName) {
        return authenticationDescriptorHandlers.stream()
                .filter(taskDescriptorHandler -> {
                    AuthenticationDescriptors authenticationDescriptors =
                            taskDescriptorHandler.getAuthenticationDescriptors();

                    return Objects.equals(authenticationDescriptors.getTaskName(), taskName);
                })
                .findFirst()
                .orElse(null);
    }
}
