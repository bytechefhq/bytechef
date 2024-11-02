/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.remote.client.service;

import com.bytechef.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.constant.Environment;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectedUserServiceClient implements ConnectedUserService {

    @Override
    public ConnectedUser createConnectedUser(@NonNull Environment environment, @NonNull String externalId) {
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
    public Optional<ConnectedUser> fetchConnectedUser(@NonNull Environment environment, @NonNull String externalId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConnectedUser getConnectedUser(@NonNull Environment environment, @NonNull String externalId) {
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
}
