/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class UserProtoMapperTest {

    @SuppressFBWarnings("HARD_CODE_PASSWORD")
    @Test
    void testUserRoundTrip() {
        User user = new User();

        user.setActivated(true);
        user.setActivationKey("activation-key");
        user.setAuthProvider("GOOGLE");
        user.setAuthorityIds(List.of(1L, 2L));
        user.setCreatedDate(Instant.parse("2026-10-26T08:00:00Z"));
        user.setEmail("user@localhost.com");
        user.setFirstName("First");
        user.setId(42L);
        user.setImageUrl("https://example.com/user.png");
        user.setLangKey("en");
        user.setLastName("Last");
        user.setLogin("user@localhost.com");
        user.setPassword("password-hash");
        user.setProviderId("provider-id");
        user.setResetKey("reset-key");
        user.setTotpEnabled(true);
        user.setTotpSecret("totp-secret");
        user.setUuid(UUID.fromString("0b3f3c3e-8f0e-4c55-9d3a-2f7c1f0e6a11"));

        User mappedUser = UserProtoMapper.toUser(UserProtoMapper.toRemoteUser(user));

        assertThat(mappedUser.isActivated()).isTrue();
        assertThat(mappedUser.getAuthProvider()).isEqualTo("GOOGLE");
        assertThat(mappedUser.getAuthorityIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(mappedUser.getCreatedDate()).isEqualTo(Instant.parse("2026-10-26T08:00:00Z"));
        assertThat(mappedUser.getEmail()).isEqualTo("user@localhost.com");
        assertThat(mappedUser.getFirstName()).isEqualTo("First");
        assertThat(mappedUser.getId()).isEqualTo(42L);
        assertThat(mappedUser.getImageUrl()).isEqualTo("https://example.com/user.png");
        assertThat(mappedUser.getLangKey()).isEqualTo("en");
        assertThat(mappedUser.getLastName()).isEqualTo("Last");
        assertThat(mappedUser.getLogin()).isEqualTo("user@localhost.com");
        assertThat(mappedUser.getProviderId()).isEqualTo("provider-id");
        assertThat(mappedUser.isTotpEnabled()).isTrue();
        assertThat(mappedUser.getUuid()).isEqualTo(UUID.fromString("0b3f3c3e-8f0e-4c55-9d3a-2f7c1f0e6a11"));

        assertThat(mappedUser.getActivationKey()).isNull();
        assertThat(mappedUser.getPassword()).isNull();
        assertThat(mappedUser.getResetKey()).isNull();
        assertThat(mappedUser.getTotpSecret()).isNull();
    }

    @Test
    void testUserWithNullFieldsRoundTrip() {
        User user = new User();

        user.setLogin("minimal@localhost.com");

        User mappedUser = UserProtoMapper.toUser(UserProtoMapper.toRemoteUser(user));

        assertThat(mappedUser.getLogin()).isEqualTo("minimal@localhost.com");
        assertThat(mappedUser.getId()).isNull();
        assertThat(mappedUser.getEmail()).isNull();
        assertThat(mappedUser.getCreatedDate()).isNull();
    }

    @Test
    void testAuthorityRoundTrip() {
        Authority authority = new Authority();

        authority.setId(1L);
        authority.setName("ROLE_ADMIN");

        assertThat(UserProtoMapper.toAuthority(UserProtoMapper.toRemoteAuthority(authority))).isEqualTo(authority);
    }
}
