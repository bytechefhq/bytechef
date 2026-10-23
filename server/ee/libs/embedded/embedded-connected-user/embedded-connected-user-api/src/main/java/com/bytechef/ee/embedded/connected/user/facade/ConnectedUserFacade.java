/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserImportResultDTO;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserFacade {

    ConnectedUserDTO createConnectedUser(
        String externalId, Environment environment, @Nullable String name, @Nullable String email,
        @Nullable Map<String, String> metadata);

    void deleteConnectedUser(long id);

    void enableConnectedUser(long id, boolean enable);

    ConnectedUser getConnectedUser(long id);

    ConnectedUserDTO getConnectedUserDTO(long id);

    Page<ConnectedUserDTO> getConnectedUserDTOs(
        Long environmentId, String search, CredentialStatus credentialStatus, LocalDate createDateFrom,
        LocalDate createDateTo, Long integrationId, int pageNumber);

    Page<ConnectedUser> getConnectedUsers(
        Long environmentId, String name, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        int pageNumber);

    ConnectedUserImportResultDTO importConnectedUsers(Environment environment, InputStream csvInputStream);

    ConnectedUserDTO updateConnectedUser(
        long id, @Nullable String name, @Nullable String email, @Nullable Map<String, String> metadata, int version);
}
