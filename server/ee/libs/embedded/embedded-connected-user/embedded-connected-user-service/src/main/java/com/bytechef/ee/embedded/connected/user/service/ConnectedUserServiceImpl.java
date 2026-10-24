/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.service;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.connected.user.constant.ConnectedUserConstants;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.repository.ConnectedUserRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.OptimisticLockingFailureException;
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
@ConditionalOnEEVersion
public class ConnectedUserServiceImpl implements ConnectedUserService {

    private static final int MAX_LENGTH = 256;

    private final ConnectedUserRepository connectedUserRepository;

    @SuppressFBWarnings("EI")
    public ConnectedUserServiceImpl(ConnectedUserRepository connectedUserRepository) {
        this.connectedUserRepository = connectedUserRepository;
    }

    @Override
    public ConnectedUser createConnectedUser(String externalId, long environmentId) {
        return createConnectedUser(externalId, Environment.values()[(int) environmentId]);
    }

    @Override
    public ConnectedUser createConnectedUser(String externalId, Environment environment) {
        validateNotReserved(externalId);

        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEnabled(true);
        connectedUser.setEnvironment(environment);
        connectedUser.setExternalId(externalId);

        return connectedUserRepository.save(connectedUser);
    }

    @Override
    public ConnectedUser createConnectedUser(
        String externalId, Environment environment, @Nullable String name, @Nullable String email,
        @Nullable Map<String, String> metadata) {

        validateExternalId(externalId);

        String normalizedName = normalizeName(name);
        String normalizedEmail = normalizeEmail(email);
        Map<String, String> normalizedMetadata = metadata == null ? Map.of() : metadata;

        validateMetadata(normalizedMetadata);

        if (fetchConnectedUser(externalId, environment).isPresent()) {
            throw new IllegalArgumentException(
                "A connected user with externalId '%s' already exists".formatted(externalId));
        }

        ConnectedUser connectedUser = new ConnectedUser();

        connectedUser.setEmail(normalizedEmail);
        connectedUser.setEnabled(true);
        connectedUser.setEnvironment(environment);
        connectedUser.setExternalId(externalId);
        connectedUser.setMetadata(normalizedMetadata);
        connectedUser.setName(normalizedName);

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
    public Optional<ConnectedUser> fetchConnectedUser(long id) {
        return connectedUserRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConnectedUser> fetchConnectedUser(String externalId, long environmentId) {
        return connectedUserRepository.findByExternalIdAndEnvironment(externalId, (int) environmentId);
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
    @Transactional(readOnly = true)
    public List<ConnectedUser> getConnectedUsers(List<Long> ids) {
        return connectedUserRepository.findAllById(ids);
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

    @Override
    public ConnectedUser updateConnectedUser(
        long id, @Nullable String name, @Nullable String email, @Nullable Map<String, String> metadata, int version) {

        ConnectedUser connectedUser = getConnectedUser(id);

        if (connectedUser.getVersion() != version) {
            throw new OptimisticLockingFailureException("Connected user " + id + " was modified");
        }

        String normalizedName = normalizeName(name);
        String normalizedEmail = normalizeEmail(email);
        Map<String, String> normalizedMetadata = metadata == null ? Map.of() : metadata;

        validateMetadata(normalizedMetadata);

        connectedUser.setEmail(normalizedEmail);
        connectedUser.setMetadata(normalizedMetadata);
        connectedUser.setName(normalizedName);

        return connectedUserRepository.save(connectedUser);
    }

    public static void validateExternalId(String externalId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId is required");
        }

        if (!externalId.equals(externalId.strip())) {
            throw new IllegalArgumentException("externalId '%s' must not have leading or trailing whitespace"
                .formatted(externalId));
        }

        if (externalId.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("externalId must not be longer than " + MAX_LENGTH + " characters");
        }

        validateNotReserved(externalId);
    }

    private static void validateNotReserved(String externalId) {
        if (ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS.contains(externalId)) {
            throw new IllegalArgumentException(
                "externalId '%s' is reserved and cannot be used for a connected user".formatted(externalId));
        }
    }

    private static String normalizeEmail(@Nullable String email) {
        String normalizedEmail = normalizeOptional(email, "email");

        if (normalizedEmail != null && !normalizedEmail.contains("@")) {
            throw new IllegalArgumentException("email '%s' is not a valid email address".formatted(normalizedEmail));
        }

        return normalizedEmail;
    }

    private static String normalizeName(@Nullable String name) {
        return normalizeOptional(name, "name");
    }

    private static String normalizeOptional(@Nullable String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(fieldName + " must not be longer than " + MAX_LENGTH + " characters");
        }

        return value;
    }

    private static void validateMetadata(Map<String, String> metadata) {
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key == null || key.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(
                    "metadata keys must not be longer than " + MAX_LENGTH + " characters");
            }

            if (value == null) {
                throw new IllegalArgumentException("metadata value for key '%s' must not be null".formatted(key));
            }

            if (value.length() > MAX_LENGTH) {
                throw new IllegalArgumentException(
                    "metadata value for key '%s' must not be longer than %d characters".formatted(key, MAX_LENGTH));
            }
        }
    }
}
