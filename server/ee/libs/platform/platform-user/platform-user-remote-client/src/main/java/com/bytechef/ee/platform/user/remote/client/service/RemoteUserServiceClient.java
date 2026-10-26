/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.service;

import com.bytechef.ee.platform.user.remote.grpc.UserProtoMapper;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserByEmailRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserByIdRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserByLoginRequest;
import com.bytechef.ee.platform.user.remote.grpc.proto.FetchUserResponse;
import com.bytechef.ee.platform.user.remote.grpc.proto.UserRemoteServiceGrpc;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.UserNotFoundException;
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

    private static final String SERVICE_ID = "api-gateway-app";

    private final GrpcChannels grpcChannels;

    public RemoteUserServiceClient(GrpcChannels grpcChannels) {
        this.grpcChannels = grpcChannels;
    }

    @Override
    public Optional<User> activateInvitation(String key, String newPassword) {
        throw new UnsupportedOperationException();
    }

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
    public void disableTotp(String login) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableTotp(String login) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Page<User> getAllActiveUsers(Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> fetchCurrentUser() {
        Optional<String> login = SecurityUtils.fetchCurrentUserLogin();

        return login.flatMap(this::fetchUserByLogin);
    }

    @Override
    public Optional<User> fetchUser(long id) {
        return toUser(
            getStub().fetchUserById(
                FetchUserByIdRequest.newBuilder()
                    .setId(id)
                    .build()));
    }

    @Override
    public Optional<User> fetchUserByAuthProviderAndProviderId(String authProvider, String providerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<User> fetchUserByEmail(String email) {
        return toUser(
            getStub().fetchUserByEmail(
                FetchUserByEmailRequest.newBuilder()
                    .setEmail(email)
                    .build()));
    }

    @Override
    public Optional<User> fetchUserByLogin(String login) {
        return toUser(
            getStub().fetchUserByLogin(
                FetchUserByLoginRequest.newBuilder()
                    .setLogin(login)
                    .build()));
    }

    @Override
    public Page<User> getAllManagedUsers(Pageable pageable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public User getCurrentUser() {
        Optional<User> user = fetchCurrentUser();

        return user.orElseThrow(UserNotFoundException::new);
    }

    @Override
    public User getUser(long id) {
        Optional<User> user = fetchUser(id);

        return user.orElseThrow(UserNotFoundException::new);
    }

    @Override
    public User getUser(String login) {
        Optional<User> user = fetchUserByLogin(login);

        return user.orElseThrow(UserNotFoundException::new);
    }

    @Override
    public User findOrCreateSocialUser(
        String email, String firstName, String lastName, String imageUrl, String authProvider, String providerId,
        boolean autoProvision, String defaultAuthority) {

        throw new UnsupportedOperationException();
    }

    @Override
    public String generateTotpSecret(String login) {
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
    public void unlinkProvider(String login) {
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

    @Override
    public boolean verifyTotpCode(String login, String code) {
        throw new UnsupportedOperationException();
    }

    private UserRemoteServiceGrpc.UserRemoteServiceBlockingStub getStub() {
        return UserRemoteServiceGrpc.newBlockingStub(grpcChannels.getChannel(SERVICE_ID));
    }

    private static Optional<User> toUser(FetchUserResponse fetchUserResponse) {
        if (!fetchUserResponse.hasUser()) {
            return Optional.empty();
        }

        return Optional.of(UserProtoMapper.toUser(fetchUserResponse.getUser()));
    }
}
