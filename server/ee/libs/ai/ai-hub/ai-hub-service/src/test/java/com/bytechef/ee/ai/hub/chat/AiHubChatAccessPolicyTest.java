/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.constant.AuthorityConstants;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link AiHubChatAccessPolicyImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubChatAccessPolicyTest {

    private static final long OWNER_ID = 3L;
    private static final long OTHER_ID = 4L;

    private final AiHubChatAccessPolicy policy = new AiHubChatAccessPolicyImpl();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testOwnerCanDoEverything() {
        authenticate("ana");

        AiHubChat chat = chat();

        assertThat(policy.canView(chat, OWNER_ID)).isTrue();
        assertThat(policy.canParticipate(chat, OWNER_ID)).isTrue();
        assertThat(policy.canManage(chat, OWNER_ID)).isTrue();
    }

    @Test
    void testAdminCanDoEverythingOnAnotherUsersChat() {
        authenticate("root", AuthorityConstants.ADMIN);

        AiHubChat chat = chat();

        assertThat(policy.canView(chat, OTHER_ID)).isTrue();
        assertThat(policy.canParticipate(chat, OTHER_ID)).isTrue();
        assertThat(policy.canManage(chat, OTHER_ID)).isTrue();
    }

    @Test
    void testNonOwnerWithoutAdminAuthorityCanDoNothing() {
        authenticate("bob", AuthorityConstants.USER);

        AiHubChat chat = chat();

        assertThat(policy.canView(chat, OTHER_ID)).isFalse();
        assertThat(policy.canParticipate(chat, OTHER_ID)).isFalse();
        assertThat(policy.canManage(chat, OTHER_ID)).isFalse();
    }

    @Test
    void testUnauthenticatedNonOwnerCanDoNothing() {
        AiHubChat chat = chat();

        assertThat(policy.canView(chat, OTHER_ID)).isFalse();
        assertThat(policy.canParticipate(chat, OTHER_ID)).isFalse();
        assertThat(policy.canManage(chat, OTHER_ID)).isFalse();
    }

    private static AiHubChat chat() {
        return new AiHubChat(OWNER_ID);
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                login, "n/a", Arrays.stream(authorities)
                    .map(SimpleGrantedAuthority::new)
                    .toList()));
    }
}
