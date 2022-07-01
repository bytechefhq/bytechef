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

package com.bytechef.hermes.descriptor.service;

import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandlerResolver;
import com.bytechef.hermes.descriptor.repository.ExtAuthenticationDescriptorHandlerRepository;
import com.bytechef.hermes.descriptor.resolver.AuthenticationDescriptorHandlerResolverChain;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
public class AuthenticationDescriptorHandlerServiceImpl implements AuthenticationDescriptorHandlerService {

    private final ExtAuthenticationDescriptorHandlerRepository extAuthenticationDescriptorHandlerRepository;
    private final AuthenticationDescriptorHandlerResolver authenticationDescriptorHandlerResolver;

    public AuthenticationDescriptorHandlerServiceImpl(
            ExtAuthenticationDescriptorHandlerRepository extAuthenticationDescriptorHandlerRepository,
            AuthenticationDescriptorHandlerResolverChain authenticationDescriptorHandlerResolver) {
        this.extAuthenticationDescriptorHandlerRepository = extAuthenticationDescriptorHandlerRepository;
        this.authenticationDescriptorHandlerResolver = authenticationDescriptorHandlerResolver;
    }

    @Override
    public AuthenticationDescriptorHandler getAuthenticationDescriptorHandler(String taskName) {
        Assert.notNull(taskName, "taskName cannot be null");

        return authenticationDescriptorHandlerResolver.resolve(taskName);
    }

    @Override
    public List<AuthenticationDescriptorHandler> getAuthenticationDescriptorHandlers() {
        return authenticationDescriptorHandlerResolver.getAuthenticationDescriptorHandlers();
    }

    @Override
    public void registerAuthenticationDescriptorHandler(String taskName, String type) {
        extAuthenticationDescriptorHandlerRepository.create(taskName, type);
    }

    @Override
    public void unregisterAuthenticationDescriptorHandler(String taskName) {
        extAuthenticationDescriptorHandlerRepository.delete(taskName);
    }

    @Override
    public AuthenticationDescriptorHandler resolve(String taskName) {
        return authenticationDescriptorHandlerResolver.resolve(taskName);
    }
}
