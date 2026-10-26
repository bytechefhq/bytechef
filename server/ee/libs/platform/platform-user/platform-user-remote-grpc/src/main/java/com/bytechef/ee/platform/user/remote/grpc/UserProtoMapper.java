/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.grpc;

import com.bytechef.ee.platform.user.remote.grpc.proto.RemoteAuthority;
import com.bytechef.ee.platform.user.remote.grpc.proto.RemoteUser;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Maps users and authorities to and from their gRPC messages. The password hash, TOTP secret, activation key and reset
 * key are never mapped.
 *
 * @version ee
 */
public final class UserProtoMapper {

    private UserProtoMapper() {
    }

    public static Authority toAuthority(RemoteAuthority remoteAuthority) {
        Authority authority = new Authority();

        if (remoteAuthority.hasId()) {
            authority.setId(remoteAuthority.getId());
        }

        if (remoteAuthority.hasName()) {
            authority.setName(remoteAuthority.getName());
        }

        return authority;
    }

    public static RemoteAuthority toRemoteAuthority(Authority authority) {
        RemoteAuthority.Builder builder = RemoteAuthority.newBuilder();

        if (authority.getId() != null) {
            builder.setId(authority.getId());
        }

        if (authority.getName() != null) {
            builder.setName(authority.getName());
        }

        return builder.build();
    }

    public static RemoteUser toRemoteUser(User user) {
        RemoteUser.Builder builder = RemoteUser.newBuilder()
            .setActivated(user.isActivated())
            .setTotpEnabled(user.isTotpEnabled())
            .addAllAuthorityIds(user.getAuthorityIds());

        if (user.getAuthProvider() != null) {
            builder.setAuthProvider(user.getAuthProvider());
        }

        if (user.getCreatedDate() != null) {
            builder.setCreatedDate(toTimestamp(user.getCreatedDate()));
        }

        if (user.getEmail() != null) {
            builder.setEmail(user.getEmail());
        }

        if (user.getFirstName() != null) {
            builder.setFirstName(user.getFirstName());
        }

        if (user.getId() != null) {
            builder.setId(user.getId());
        }

        if (user.getImageUrl() != null) {
            builder.setImageUrl(user.getImageUrl());
        }

        if (user.getLangKey() != null) {
            builder.setLangKey(user.getLangKey());
        }

        if (user.getLastName() != null) {
            builder.setLastName(user.getLastName());
        }

        if (user.getLogin() != null) {
            builder.setLogin(user.getLogin());
        }

        if (user.getProviderId() != null) {
            builder.setProviderId(user.getProviderId());
        }

        if (user.getUuid() != null) {
            builder.setUuid(user.getUuidAsString());
        }

        return builder.build();
    }

    public static User toUser(RemoteUser remoteUser) {
        User user = new User();

        user.setActivated(remoteUser.getActivated());
        user.setAuthorityIds(remoteUser.getAuthorityIdsList());
        user.setTotpEnabled(remoteUser.getTotpEnabled());

        if (remoteUser.hasAuthProvider()) {
            user.setAuthProvider(remoteUser.getAuthProvider());
        }

        if (remoteUser.hasCreatedDate()) {
            user.setCreatedDate(toInstant(remoteUser.getCreatedDate()));
        }

        if (remoteUser.hasEmail()) {
            user.setEmail(remoteUser.getEmail());
        }

        if (remoteUser.hasFirstName()) {
            user.setFirstName(remoteUser.getFirstName());
        }

        if (remoteUser.hasId()) {
            user.setId(remoteUser.getId());
        }

        if (remoteUser.hasImageUrl()) {
            user.setImageUrl(remoteUser.getImageUrl());
        }

        if (remoteUser.hasLangKey()) {
            user.setLangKey(remoteUser.getLangKey());
        }

        if (remoteUser.hasLastName()) {
            user.setLastName(remoteUser.getLastName());
        }

        if (remoteUser.hasLogin()) {
            user.setLogin(remoteUser.getLogin());
        }

        if (remoteUser.hasProviderId()) {
            user.setProviderId(remoteUser.getProviderId());
        }

        if (remoteUser.hasUuid()) {
            user.setUuid(UUID.fromString(remoteUser.getUuid()));
        }

        return user;
    }

    private static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }
}
