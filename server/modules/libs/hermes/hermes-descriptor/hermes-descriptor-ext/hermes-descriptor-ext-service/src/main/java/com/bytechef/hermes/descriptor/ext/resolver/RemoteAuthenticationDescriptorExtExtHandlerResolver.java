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

package com.bytechef.hermes.descriptor.ext.resolver;

import com.bytechef.hermes.descriptor.ext.domain.DescriptorExtHandler;
import com.bytechef.hermes.descriptor.ext.service.DescriptorExtHandlerService;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import com.bytechef.hermes.descriptor.model.AuthenticationDescriptors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ivica Cardic
 */
@Component
@Order(2)
public class RemoteAuthenticationDescriptorExtExtHandlerResolver
        extends AbstractAuthenticationDescriptorExtHandlerResolver {

    public static final String REMOTE = "REMOTE";

    public RemoteAuthenticationDescriptorExtExtHandlerResolver(
            DescriptorExtHandlerService descriptorExtHandlerService) {
        super(descriptorExtHandlerService, REMOTE);
    }

    @Override
    protected AuthenticationDescriptorHandler createAuthenticationDescriptorHandler(
            DescriptorExtHandler descriptorExtHandler) {
        return new RemoteAuthenticationDescriptorHandlerProxy(
                descriptorExtHandler.getName(),
                descriptorExtHandler.getProperty("hostAddress"),
                descriptorExtHandler.getProperty("port"));
    }

    private static class RemoteAuthenticationDescriptorHandlerProxy implements AuthenticationDescriptorHandler {

        private static final RestTemplate restTemplate = new RestTemplate();

        private final String url;

        private RemoteAuthenticationDescriptorHandlerProxy(String name, String hostAddress, int port) {
            this.url = "http://%s:%s/authentication-descriptors/%s".formatted(hostAddress, port, name);
        }

        @Override
        public AuthenticationDescriptors getAuthenticationDescriptors() {
            return restTemplate.getForObject(url, AuthenticationDescriptors.class);
        }
    }
}
