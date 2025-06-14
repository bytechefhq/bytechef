/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.client.service;

import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.constant.Environment;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectedUserServiceClient implements ConnectedUserService {

    @Override
    public ConnectedUser createConnectedUser(String externalId, Environment environment) {
        return null;
    }

    @Override
    public void deleteConnectedUser(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableConnectedUser(long id, boolean enable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ConnectedUser> fetchConnectedUser(String externalId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUser getConnectedUser(String externalId, Environment environment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUser getConnectedUser(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<ConnectedUser> getConnectedUsers(
        Environment environment, String name, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        int pageNumber) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void updateConnectedUser(String externalUserId, Environment environment, Map<String, Object> metadata) {
        throw new UnsupportedOperationException();
    }
}
