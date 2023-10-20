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

import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandlerResolver;
import com.bytechef.hermes.descriptor.repository.ExtAuthenticationDescriptorHandlerRepository;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractAuthenticationDescriptorHandlerResolver
        implements AuthenticationDescriptorHandlerResolver {

    private final ExtAuthenticationDescriptorHandlerRepository extAuthenticationDescriptorHandlerRepository;
    private final String type;

    protected AbstractAuthenticationDescriptorHandlerResolver(
            ExtAuthenticationDescriptorHandlerRepository extAuthenticationDescriptorHandlerRepository, String type) {
        this.extAuthenticationDescriptorHandlerRepository = extAuthenticationDescriptorHandlerRepository;
        this.type = type;
    }

    @Override
    public AuthenticationDescriptorHandler resolve(String taskName) {
        AuthenticationDescriptorHandler authenticationDescriptorHandler = null;

        if (extAuthenticationDescriptorHandlerRepository.existByTaskNameAndType(taskName, type)) {
            authenticationDescriptorHandler = createAuthenticationDescriptorHandler(taskName);
        }

        return authenticationDescriptorHandler;
    }

    @Override
    public List<AuthenticationDescriptorHandler> getAuthenticationDescriptorHandlers() {
        return extAuthenticationDescriptorHandlerRepository.findAllByType(type).stream()
                .map(this::createAuthenticationDescriptorHandler)
                .toList();
    }

    protected abstract AuthenticationDescriptorHandler createAuthenticationDescriptorHandler(String taskName);
}
