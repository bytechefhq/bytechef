/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.service;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectedUserServiceImpl implements ConnectedUserService {

    private final ConnectedUserRepository connectedUserRepository;

    @SuppressFBWarnings("EI")
    public ConnectedUserServiceImpl(ConnectedUserRepository connectedUserRepository) {
        this.connectedUserRepository = connectedUserRepository;
    }

    @Override
    public ConnectedUser createConnectedUser(String externalId, Environment environment) {
        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnabled(true);
        connectedUser.setEnvironment(environment);
        connectedUser.setExternalId(externalId);

        return connectedUserRepository.save(connectedUser);
    }

    @Override
    public void deleteConnectedUser(long id) {
        connectedUserRepository.deleteById(id);
    }

    @Override
    public void enableConnectedUser(long id, boolean enable) {
        ConnectedUser connectedUser = getConnectedUser(id);

        connectedUser.setEnabled(enable);

        connectedUserRepository.save(connectedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConnectedUser> fetchConnectedUser(String externalId, Environment environment) {
        return connectedUserRepository.findByExternalIdAndEnvironment(externalId, environment.ordinal());
    }

    @Override
    public ConnectedUser getConnectedUser(String externalId, Environment environment) {
        return fetchConnectedUser(externalId, environment)
            .orElseThrow(() -> new IllegalArgumentException("Connected user not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUser getConnectedUser(long id) {
        return connectedUserRepository.findById(id)
            .orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConnectedUser> getConnectedUsers(
        Environment environment, String search, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId,
        int pageNumber) {

        PageRequest pageRequest = PageRequest.of(pageNumber, ConnectedUserRepository.DEFAULT_PAGE_SIZE);

        return connectedUserRepository.findAll(
            environment == null ? null : environment.ordinal(), search, createDateFrom, createDateTo, integrationId,
            pageRequest);
    }

    @Override
    public void updateConnectedUser(String externalUserId, Environment environment, Map<String, Object> metadata) {
        ConnectedUser connectedUser = getConnectedUser(externalUserId, environment);

        Map<String, String> filteredMetadata = new HashMap<>();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.equalsIgnoreCase("name") && value instanceof String) {
                connectedUser.setName((String) value);
            } else if (key.equalsIgnoreCase("email") && value instanceof String) {
                connectedUser.setEmail((String) value);
            } else {
                filteredMetadata.put(key, value == null ? null : value.toString());
            }
        }

        connectedUser.setMetadata(MapUtils.concat(connectedUser.getMetadata(), filteredMetadata));

        connectedUserRepository.save(connectedUser);
    }
}
