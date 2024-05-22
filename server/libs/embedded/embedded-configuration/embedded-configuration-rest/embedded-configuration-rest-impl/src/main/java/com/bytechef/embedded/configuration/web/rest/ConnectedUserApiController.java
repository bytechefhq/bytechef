/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.configuration.web.rest;

import com.bytechef.embedded.configuration.facade.ConnectedUserFacade;
import com.bytechef.embedded.configuration.service.ConnectedUserService;
import com.bytechef.embedded.configuration.web.rest.model.ConnectedUserModel;
import com.bytechef.platform.annotation.ConditionalOnEndpoint;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.web.rest.model.CredentialStatusModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}")
@ConditionalOnEndpoint
public class ConnectedUserApiController implements ConnectedUserApi {

    private final ConnectedUserFacade connectedUserFacade;
    private final ConnectedUserService connectedUserService;
    private final ConversionService conversionService;

    @SuppressFBWarnings("EI")
    public ConnectedUserApiController(
        ConnectedUserFacade connectedUserFacade, ConnectedUserService connectedUserService,
        ConversionService conversionService) {

        this.connectedUserFacade = connectedUserFacade;
        this.connectedUserService = connectedUserService;
        this.conversionService = conversionService;
    }

    @Override
    public ResponseEntity<Void> deleteConnectedUser(Long id) {
        connectedUserService.deleteConnectedUser(id);

        return ResponseEntity.noContent()
            .build();
    }

    @Override
    public ResponseEntity<ConnectedUserModel> getConnectedUser(Long id) {
        return ResponseEntity.ok(
            conversionService.convert(connectedUserFacade.getConnectedUser(id), ConnectedUserModel.class));
    }

    @Override
    public ResponseEntity<Page> getConnectedUsers(
        String search, CredentialStatusModel credentialStatus, Long integrationId, LocalDate createDateFrom,
        LocalDate createDateTo, Integer pageNumber) {

        return ResponseEntity.ok(
            connectedUserFacade
                .getConnectedUsers(
                    search, credentialStatus == null ? null : CredentialStatus.valueOf(credentialStatus.name()),
                    createDateFrom, createDateTo, integrationId, pageNumber)
                .map(connectedUser -> conversionService.convert(connectedUser, ConnectedUserModel.class)));
    }

    @Override
    public ResponseEntity<Void> enableConnectedUser(Long id, Boolean enable) {
        connectedUserService.enableConnectedUser(id, enable);

        return ResponseEntity.noContent()
            .build();
    }
}
