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
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@Primary
public class AuthenticationDescriptorHandlerResolverChain implements AuthenticationDescriptorHandlerResolver {

    private final List<AuthenticationDescriptorHandlerResolver> authenticationDescriptorHandlerResolvers;

    public AuthenticationDescriptorHandlerResolverChain(
            List<AuthenticationDescriptorHandlerResolver> authenticationDescriptorHandlerResolvers) {
        this.authenticationDescriptorHandlerResolvers = authenticationDescriptorHandlerResolvers;
    }

    @Override
    public List<AuthenticationDescriptorHandler> getAuthenticationDescriptorHandlers() {
        return authenticationDescriptorHandlerResolvers.stream()
                .flatMap(authenticationDescriptorHandlerResolver ->
                        authenticationDescriptorHandlerResolver.getAuthenticationDescriptorHandlers().stream())
                .toList();
    }

    @Override
    public AuthenticationDescriptorHandler resolve(String taskName) {
        AuthenticationDescriptorHandler authenticationDescriptorHandler = null;

        for (AuthenticationDescriptorHandlerResolver authenticationDescriptorHandlerResolver :
                authenticationDescriptorHandlerResolvers) {
            authenticationDescriptorHandler = authenticationDescriptorHandlerResolver.resolve(taskName);

            if (authenticationDescriptorHandler != null) {
                break;
            }
        }

        return authenticationDescriptorHandler;
    }
}
