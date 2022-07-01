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
import com.bytechef.hermes.descriptor.repository.ExtAuthenticationDescriptorHandlerRepository;
import com.bytechef.hermes.descriptor.resolver.RemoteExtTaskDescriptorHandlerResolver;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest
public class AuthenticationDescriptorHandlerServiceIntTest {

    @Autowired
    private AuthenticationDescriptorHandlerServiceImpl authenticationDescriptorHandlerService;

    @Autowired
    private ExtAuthenticationDescriptorHandlerRepository extAuthenticationDescriptorHandlerRepository;

    @BeforeEach
    public void beforeEach() {
        for (Map.Entry<String, String> entry :
                extAuthenticationDescriptorHandlerRepository.findAll().entrySet()) {
            extAuthenticationDescriptorHandlerRepository.delete(entry.getKey());
        }
    }

    @Test
    public void testGetTaskAuthDescriptorHandler() {
        AuthenticationDescriptorHandler authenticationDescriptorHandler =
                authenticationDescriptorHandlerService.getAuthenticationDescriptorHandler("csvFile");

        Assertions.assertNotNull(authenticationDescriptorHandler);

        authenticationDescriptorHandler =
                authenticationDescriptorHandlerService.getAuthenticationDescriptorHandler("jsonFile");

        Assertions.assertNull(authenticationDescriptorHandler);

        authenticationDescriptorHandlerService.registerAuthenticationDescriptorHandler(
                "jsonFile", RemoteExtTaskDescriptorHandlerResolver.REMOTE);

        authenticationDescriptorHandler =
                authenticationDescriptorHandlerService.getAuthenticationDescriptorHandler("jsonFile");

        Assertions.assertNotNull(authenticationDescriptorHandler);
    }

    @Test
    public void testGetTaskAuthDescriptorHandlers() {
        Assertions.assertEquals(
                1,
                authenticationDescriptorHandlerService
                        .getAuthenticationDescriptorHandlers()
                        .size());
    }

    @Test
    public void testRegisterExtTaskAuthDescriptorHandler() {
        authenticationDescriptorHandlerService.registerAuthenticationDescriptorHandler(
                "jsonFile", RemoteExtTaskDescriptorHandlerResolver.REMOTE);

        AuthenticationDescriptorHandler authenticationDescriptorHandler =
                authenticationDescriptorHandlerService.getAuthenticationDescriptorHandler("jsonFile");

        Assertions.assertNotNull(authenticationDescriptorHandler);
    }

    @Test
    public void testUnregisterAuthenticationDescriptorHandler() {
        authenticationDescriptorHandlerService.registerAuthenticationDescriptorHandler(
                "jsonFile", RemoteExtTaskDescriptorHandlerResolver.REMOTE);

        AuthenticationDescriptorHandler authenticationDescriptorHandler =
                authenticationDescriptorHandlerService.getAuthenticationDescriptorHandler("jsonFile");

        Assertions.assertNotNull(authenticationDescriptorHandler);

        authenticationDescriptorHandlerService.unregisterAuthenticationDescriptorHandler("jsonFile");

        authenticationDescriptorHandler =
                authenticationDescriptorHandlerService.getAuthenticationDescriptorHandler("jsonFile");

        Assertions.assertNull(authenticationDescriptorHandler);
    }
}
