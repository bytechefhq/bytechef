/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.service;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteUserServiceClient implements UserService {

    @Override
    public Optional<User> activateRegistration(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void changePassword(String currentClearTextPassword, String newPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> completePasswordReset(String newPassword, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countActiveUsers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public User create(AdminUserDTO userDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String login) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<User> getAllActiveUsers(Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> fetchCurrentUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> fetchUser(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> fetchUserByAuthProviderAndProviderId(String authProvider, String providerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> fetchUserByEmail(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> fetchUserByLogin(String login) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<User> getAllManagedUsers(Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User getCurrentUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public User getUser(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User findOrCreateSocialUser(
        String email, String firstName, String lastName, String imageUrl, String authProvider, String providerId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public User registerUser(AdminUserDTO userDTO, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNotActivatedUsers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeOldPersistentTokens() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> requestPasswordReset(String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(User user) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> update(AdminUserDTO userDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(String firstName, String lastName, String email, String langKey, String imageUrl) {
        throw new UnsupportedOperationException();
    }
}
