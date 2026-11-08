/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.service.UserService;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CurrentUserResolverTest {

    private static final String LOGIN = "mcp-anonymous:management";

    private final UserService userService = mock(UserService.class);
    private final InMemoryTransactionManager transactionManager = new InMemoryTransactionManager();

    private CurrentUserResolver currentUserResolver;

    @BeforeEach
    void beforeEach() {
        when(userService.getUser(anyString())).thenThrow(new UserNotFoundException());
        when(userService.fetchUserByLogin(anyString())).thenReturn(Optional.empty());

        currentUserResolver = new CurrentUserResolver(transactionManager.transactionalProxy(userService));

        SecurityContextHolder.getContext()
            .setAuthentication(new TestingAuthenticationToken(LOGIN, null));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testMissingUserDoesNotRollBackTheEnclosingTransaction() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        OptionalLong userId = transactionTemplate.execute(status -> currentUserResolver.fetchCurrentUserId());

        assertThat(userId).isEmpty();
        assertThat(transactionManager.isCommitted()).isTrue();
        assertThat(transactionManager.isRolledBack()).isFalse();
    }

    @Test
    void testMissingUserInRequestScopeDoesNotRollBackTheEnclosingTransaction() {
        RequestContextHolder.setRequestAttributes(mock(RequestAttributes.class));

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        OptionalLong userId = transactionTemplate.execute(status -> currentUserResolver.fetchCurrentUserId());

        assertThat(userId).isEmpty();
        assertThat(transactionManager.isCommitted()).isTrue();
        assertThat(transactionManager.isRolledBack()).isFalse();
    }

    @Test
    void testExistingUserResolvesItsId() {
        User user = new User();

        user.setId(7L);

        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.of(user));
        doReturn(user).when(userService)
            .getUser(LOGIN);

        assertThat(currentUserResolver.fetchCurrentUserId()).hasValue(7L);

        RequestContextHolder.setRequestAttributes(mock(RequestAttributes.class));

        assertThat(currentUserResolver.fetchCurrentUserId()).hasValue(7L);
    }
}
