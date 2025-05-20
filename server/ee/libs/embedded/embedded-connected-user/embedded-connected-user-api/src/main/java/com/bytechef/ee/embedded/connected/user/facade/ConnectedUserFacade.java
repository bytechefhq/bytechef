/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.constant.Environment;
import java.time.LocalDate;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserFacade {

    void enableConnectedUser(long id, boolean enable);

    ConnectedUserDTO getConnectedUser(long id);

    Page<ConnectedUserDTO> getConnectedUsers(
        Environment environment, String search, CredentialStatus credentialStatus, LocalDate createDateFrom,
        LocalDate createDateTo, Long integrationId, int pageNumber);
}
