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

package com.bytechef.hermes.descriptor.web.rest;

import com.bytechef.atlas.annotation.ConditionalOnCoordinator;
import com.bytechef.hermes.descriptor.domain.AuthenticationDescriptors;
import com.bytechef.hermes.descriptor.handler.AuthenticationDescriptorHandler;
import com.bytechef.hermes.descriptor.service.AuthenticationDescriptorHandlerService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@ConditionalOnCoordinator
public class AuthenticationDescriptorController {

    private final AuthenticationDescriptorHandlerService authenticationDescriptorHandlerService;

    public AuthenticationDescriptorController(
            AuthenticationDescriptorHandlerService authenticationDescriptorHandlerService) {
        this.authenticationDescriptorHandlerService = authenticationDescriptorHandlerService;
    }

    @GetMapping(value = "/authentication-descriptors", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AuthenticationDescriptors> getAuthenticationDescriptors() {
        return authenticationDescriptorHandlerService.getAuthenticationDescriptorHandlers().stream()
                .map(AuthenticationDescriptorHandler::getAuthenticationDescriptors)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/authentication-descriptors/{taskName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public AuthenticationDescriptors getAuthenticationDescriptors(@PathVariable("taskName") String taskName) {
        AuthenticationDescriptorHandler authenticationDescriptorHandler =
                authenticationDescriptorHandlerService.getAuthenticationDescriptorHandler(taskName);

        return authenticationDescriptorHandler.getAuthenticationDescriptors();
    }

    @PostMapping(value = "/authentication-descriptors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> postAuthenticationDescriptors(
            @PathVariable("taskName") String taskName, @PathVariable("type") String type) {
        authenticationDescriptorHandlerService.registerAuthenticationDescriptorHandler(taskName, type);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/authentication-descriptors", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteAuthenticationDescriptors(@PathVariable("taskName") String taskName) {
        authenticationDescriptorHandlerService.unregisterAuthenticationDescriptorHandler(taskName);

        return ResponseEntity.ok().build();
    }
}
