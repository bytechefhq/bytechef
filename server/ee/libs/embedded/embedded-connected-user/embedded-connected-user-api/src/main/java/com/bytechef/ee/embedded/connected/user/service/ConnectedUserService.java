/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.service;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.platform.constant.Environment;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserService {

    ConnectedUser createConnectedUser(String externalId, Environment environment);

    void deleteConnectedUser(long id);

    void enableConnectedUser(long id, boolean enable);

    Optional<ConnectedUser> fetchConnectedUser(String externalId, Environment environment);

    ConnectedUser getConnectedUser(String externalId, Environment environment);

    ConnectedUser getConnectedUser(long id);

    Page<ConnectedUser> getConnectedUsers(
        Environment environment, String name, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        int pageNumber);

    void updateConnectedUser(String externalUserId, Environment environment, Map<String, Object> metadata);
}
