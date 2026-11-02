/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.audit.UserAuditEvents;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.UserNotFoundException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@ActiveProfiles("testint")
@Import({
    PostgreSQLContainerConfiguration.class, UserManagementFacadeAuditIntTest.UserManagementFacadeConfiguration.class
})
class UserManagementFacadeAuditIntTest {

    private static final String INVITE_PASSWORD = "Inv1tePassw0rd";

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private UserManagementFacade userManagementFacade;

    @MockitoBean
    private AuthorityService authorityService;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.ADMIN))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testInviteUserRecordsTheEmailAndRoleButNeverThePassword() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(1L, "ROLE_USER")));
        when(userService.fetchUserByEmail("carol@localhost.com")).thenReturn(Optional.empty());
        when(userService.registerUser(any(), anyString())).thenReturn(new User());

        userManagementFacade.inviteUser("carol@localhost.com", INVITE_PASSWORD, "ROLE_USER");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(UserAuditEvents.USER_INVITED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("email", "carol@localhost.com")
            .containsEntry("role", "ROLE_USER")
            .doesNotContainValue(INVITE_PASSWORD)
            .doesNotContainKey("dataError");
    }

    @Test
    void testUpdateUserRoleRecordsTheLoginAndTheNewRole() {
        User user = new User();

        user.setLogin("bob@localhost.com");

        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(2L, "ROLE_ADMIN")));
        when(userService.fetchUserByLogin("bob@localhost.com")).thenReturn(Optional.of(user));
        when(userService.update(any())).thenReturn(Optional.of(user));

        userManagementFacade.updateUserRole("bob@localhost.com", "ROLE_ADMIN");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(UserAuditEvents.USER_ROLE_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("login", "bob@localhost.com")
            .containsEntry("role", "ROLE_ADMIN")
            .doesNotContainKey("dataError");
    }

    @Test
    void testDeleteUserRecordsTheDeletedLogin() {
        when(userService.getUser("bob@localhost.com")).thenReturn(new User());

        userManagementFacade.deleteUser("bob@localhost.com");

        verify(userService).delete("bob@localhost.com");

        PersistentAuditEvent persistentAuditEvent = findSingle();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(UserAuditEvents.USER_DELETED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("login", "bob@localhost.com")
            .doesNotContainKey("dataError");
    }

    @Test
    void testDeleteOfAnUnknownUserIsRecordedAsAnError() {
        when(userService.getUser("ghost@localhost.com")).thenThrow(new UserNotFoundException());

        assertThatThrownBy(() -> userManagementFacade.deleteUser("ghost@localhost.com"))
            .isInstanceOf(UserNotFoundException.class);

        verify(userService, never()).delete("ghost@localhost.com");

        assertThat(findSingle().getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", UserNotFoundException.class.getName())
            .containsEntry("login", "ghost@localhost.com");
    }

    private static Authority createAuthority(long id, String name) {
        Authority authority = new Authority();

        authority.setId(id);
        authority.setName(name);

        return authority;
    }

    private PersistentAuditEvent findSingle() {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> persistentAuditEvents = page.getContent();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    @EnableMethodSecurity
    static class UserManagementFacadeConfiguration {

        @Bean
        UserManagementFacade userManagementFacade(
            AuthorityService authorityService, TenantService tenantService, UserService userService) {

            return new UserManagementFacadeImpl(authorityService, mock(MailService.class), tenantService, userService);
        }
    }
}
