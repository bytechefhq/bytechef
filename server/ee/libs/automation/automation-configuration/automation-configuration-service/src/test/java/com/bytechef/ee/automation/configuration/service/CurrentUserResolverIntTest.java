/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.automation.configuration.config.EmbeddedOwnerContributorsTestConfiguration;
import com.bytechef.encryption.Encryption;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.user.callback.UserCallback;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.service.UserServiceImpl;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = CurrentUserResolverIntTest.Config.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.contexts=mono,dev"
    })
@Import(PostgreSQLContainerConfiguration.class)
class CurrentUserResolverIntTest {

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private UserService userService;

    @AfterEach
    void afterEach() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFetchCurrentUserIdResolvesAKnownLoginInsideAReadOnlyTransaction() {
        authenticate("admin");

        assertThat(fetchCurrentUserIdInReadOnlyTransaction()).hasValue(1L);
    }

    @Test
    void testFetchCurrentUserIdIsEmptyForALoginWithoutAUserInsideAReadOnlyTransaction() {
        authenticate("deleted-user");

        assertThat(fetchCurrentUserIdInReadOnlyTransaction()).isEmpty();
    }

    @Test
    void testFetchCurrentUserIdIsEmptyForALoginWithoutAUserDuringARequestInsideAReadOnlyTransaction() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        authenticate("deleted-user");

        assertThat(fetchCurrentUserIdInReadOnlyTransaction()).isEmpty();
        assertThat(fetchCurrentUserIdInReadOnlyTransaction()).isEmpty();

        authenticate("user");

        assertThat(fetchCurrentUserIdInReadOnlyTransaction()).hasValue(2L);
    }

    private OptionalLong fetchCurrentUserIdInReadOnlyTransaction() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.setReadOnly(true);

        CurrentUserResolver currentUserResolver = new CurrentUserResolver(userService);

        return transactionTemplate.execute(transactionStatus -> currentUserResolver.fetchCurrentUserId());
    }

    private static void authenticate(String login) {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(login, "n/a", List.of()));
    }

    @EnableAutoConfiguration
    @Import({
        EmbeddedOwnerContributorsTestConfiguration.class, LiquibaseConfiguration.class, UserCallback.class,
        CurrentUserResolverIntTest.Config.UserJdbcConfiguration.class
    })
    @SpringBootConfiguration
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        Encryption encryption() {
            return mock(Encryption.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        UserService userService(
            AuthorityRepository authorityRepository, CacheManager cacheManager, PasswordEncoder passwordEncoder,
            PersistentTokenRepository persistentTokenRepository, UserRepository userRepository,
            ObjectProvider<WorkspaceMembershipAssigner> workspaceMembershipAssignerProvider) {

            return new UserServiceImpl(
                authorityRepository, cacheManager, passwordEncoder, persistentTokenRepository, userRepository,
                workspaceMembershipAssignerProvider);
        }

        @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
        static class UserJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
        }
    }
}
