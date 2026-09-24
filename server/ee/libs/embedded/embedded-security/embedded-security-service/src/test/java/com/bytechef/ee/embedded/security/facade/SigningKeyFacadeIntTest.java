/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.embedded.security.config.EmbeddedSecurityIntTestConfiguration;
import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = EmbeddedSecurityIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.change-log=classpath:config/liquibase/embedded-security-testint.xml",
        "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    PermissionScopeRegistry.class, ProjectRepository.class, UserService.class, WorkspaceScopeCacheService.class,
    WorkspaceUserRepository.class
})
class SigningKeyFacadeIntTest {

    private static final String NON_ADMIN_LOGIN = "non-admin";
    private static final String OTHER_ADMIN_LOGIN = "other-admin";
    private static final String OTHER_NON_ADMIN_LOGIN = "other-non-admin";
    private static final String OWNER_ADMIN_LOGIN = "owner-admin";

    private static final Map<String, Long> USER_IDS = Map.of(
        NON_ADMIN_LOGIN, 1003L, OTHER_ADMIN_LOGIN, 1002L, OTHER_NON_ADMIN_LOGIN, 1004L, OWNER_ADMIN_LOGIN, 1001L);

    private static final long ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();

    @Autowired
    private SigningKeyFacade signingKeyFacade;

    @Autowired
    private SigningKeyRepository signingKeyRepository;

    @Autowired
    private SigningKeyService signingKeyService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        when(userService.getCurrentUser()).thenAnswer(invocation -> user(
            SecurityUtils.fetchCurrentUserLogin()
                .orElseThrow()));
        when(userService.getUser(anyString())).thenAnswer(invocation -> user(invocation.getArgument(0)));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        signingKeyRepository.deleteAll();
    }

    @Test
    void testTenantAdminOwnerCanCreateListGetUpdateAndDeleteTheirKey() {
        authenticate(OWNER_ADMIN_LOGIN, AuthorityConstants.ADMIN);

        String name = "owner-key-" + UUID.randomUUID();

        String privateKey = signingKeyFacade.create(newSigningKey(name), PlatformType.EMBEDDED);

        assertThat(privateKey).startsWith("-----BEGIN PRIVATE KEY-----");

        List<SigningKey> signingKeys = signingKeyFacade.getSigningKeys(PlatformType.EMBEDDED, ENVIRONMENT_ID);

        assertThat(signingKeys)
            .singleElement()
            .satisfies(signingKey -> {
                assertThat(signingKey.getName()).isEqualTo(name);
                assertThat(signingKey.getUserId()).isEqualTo(USER_IDS.get(OWNER_ADMIN_LOGIN));
            });

        long id = signingKeys.getFirst()
            .getId();

        assertThat(signingKeyFacade.getSigningKey(id)
            .getName()).isEqualTo(name);

        SigningKey renamedSigningKey = new SigningKey();

        renamedSigningKey.setId(id);
        renamedSigningKey.setName("renamed-" + name);

        signingKeyFacade.update(renamedSigningKey);

        assertThat(signingKeyFacade.getSigningKey(id)
            .getName()).isEqualTo("renamed-" + name);

        signingKeyFacade.delete(id);

        assertThat(signingKeyRepository.findById(id)).isEmpty();
    }

    @Test
    void testNonAdminIsDeniedOnEveryFacadeMethodEvenForTheirOwnKey() {
        SigningKey signingKey = saveSigningKey(NON_ADMIN_LOGIN);

        long id = signingKey.getId();

        authenticate(NON_ADMIN_LOGIN, AuthorityConstants.USER);

        assertThatThrownBy(
            () -> signingKeyFacade.create(newSigningKey("non-admin-key-" + UUID.randomUUID()), PlatformType.EMBEDDED))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.getSigningKeys(PlatformType.EMBEDDED, ENVIRONMENT_ID))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.getSigningKey(id))
            .isInstanceOf(AccessDeniedException.class);

        SigningKey renamedSigningKey = new SigningKey();

        renamedSigningKey.setId(id);
        renamedSigningKey.setName("renamed");

        assertThatThrownBy(() -> signingKeyFacade.update(renamedSigningKey))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> signingKeyFacade.delete(id))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(signingKeyRepository.findAll())
            .singleElement()
            .satisfies(persistedSigningKey -> assertThat(persistedSigningKey.getName())
                .isEqualTo(signingKey.getName()));
    }

    @Test
    void testAnyTenantAdminCanManageAKeyAnotherTenantAdminCreated() {
        SigningKey signingKey = saveSigningKey(OWNER_ADMIN_LOGIN);

        long id = signingKey.getId();

        authenticate(OTHER_ADMIN_LOGIN, AuthorityConstants.ADMIN);

        assertThat(signingKeyFacade.getSigningKey(id)
            .getUserId()).isEqualTo(USER_IDS.get(OWNER_ADMIN_LOGIN));

        SigningKey renamedSigningKey = new SigningKey();

        renamedSigningKey.setId(id);
        renamedSigningKey.setName("renamed-by-other-admin");

        signingKeyFacade.update(renamedSigningKey);

        assertThat(signingKeyRepository.findById(id))
            .hasValueSatisfying(persistedSigningKey -> assertThat(persistedSigningKey.getName())
                .isEqualTo("renamed-by-other-admin"));

        signingKeyFacade.delete(id);

        assertThat(signingKeyRepository.findById(id)).isEmpty();
    }

    @Test
    void testServiceDoesNotCheckTheCaller() {
        SigningKey signingKey = saveSigningKey(OWNER_ADMIN_LOGIN);

        long id = signingKey.getId();

        authenticate(OTHER_NON_ADMIN_LOGIN, AuthorityConstants.USER);

        assertThat(signingKeyService.getSigningKey(id)
            .getName()).isEqualTo(signingKey.getName());

        signingKeyService.delete(id);

        assertThat(signingKeyRepository.findById(id)).isEmpty();
    }

    private static void authenticate(String login, String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", List.of(new SimpleGrantedAuthority(authority))));
    }

    private static SigningKey newSigningKey(String name) {
        SigningKey signingKey = new SigningKey();

        signingKey.setEnvironment(Environment.PRODUCTION);
        signingKey.setName(name);

        return signingKey;
    }

    private SigningKey saveSigningKey(String ownerLogin) {
        SigningKey signingKey = newSigningKey(ownerLogin + "-key-" + UUID.randomUUID());

        signingKey.setKeyId(UUID.randomUUID()
            .toString());
        signingKey.setPublicKey("public-key");
        signingKey.setType(PlatformType.EMBEDDED);
        signingKey.setUserId(USER_IDS.get(ownerLogin));

        return signingKeyRepository.save(signingKey);
    }

    private static User user(String login) {
        User user = new User();

        user.setId(USER_IDS.get(login));
        user.setLogin(login);

        return user;
    }
}
