/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.configuration.audit.GitConfigurationAuditEvents;
import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacade;
import com.bytechef.ee.platform.configuration.facade.GitConfigurationFacadeImpl;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
@ActiveProfiles("testint")
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = AuditIntTestConfiguration.class)
@Import({
    GitConfigurationAuditIntTest.GitConfigurationFacadeConfiguration.class, PostgreSQLContainerConfiguration.class
})
class GitConfigurationAuditIntTest {

    private static final String PASSWORD = "confidential-password";
    private static final String URL = "https://git.example.com/repository.git";
    private static final String USERNAME = "git-user";
    private static final long WORKSPACE_ID = 1049L;

    @Autowired
    private GitConfigurationFacade gitConfigurationFacade;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @MockitoBean
    private PropertyService propertyService;

    @BeforeEach
    void beforeEach() {
        persistenceAuditEventRepository.deleteAll();

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@localhost.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testSaveRecordsTheWorkspaceWithoutTheCredentials() {
        when(propertyService.fetchProperty(anyString(), any(), anyLong())).thenReturn(Optional.empty());

        gitConfigurationFacade.save(new GitConfigurationDTO(URL, USERNAME, PASSWORD), WORKSPACE_ID);

        assertThat(findSingleEvent(GitConfigurationAuditEvents.GIT_CONFIGURATION_SAVED))
            .containsEntry("result", "SUCCESS")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .doesNotContainValue(URL)
            .doesNotContainValue(USERNAME)
            .doesNotContainValue(PASSWORD);
    }

    @Test
    void testFailedSaveRecordsAnError() {
        when(propertyService.fetchProperty(anyString(), any(), anyLong())).thenReturn(Optional.empty());
        doThrow(new IllegalStateException()).when(propertyService)
            .save(anyString(), any(), any(), anyLong());

        assertThatThrownBy(
            () -> gitConfigurationFacade.save(new GitConfigurationDTO(URL, USERNAME, PASSWORD), WORKSPACE_ID))
                .isInstanceOf(IllegalStateException.class);

        assertThat(findSingleEvent(GitConfigurationAuditEvents.GIT_CONFIGURATION_SAVED))
            .containsEntry("result", "ERROR")
            .containsEntry("workspaceId", String.valueOf(WORKSPACE_ID))
            .containsEntry("errorClass", IllegalStateException.class.getName());
    }

    private Map<String, String> findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page =
            persistenceAuditEventRepository.findAllFiltered(AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        List<PersistentAuditEvent> persistentAuditEvents = pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        PersistentAuditEvent persistentAuditEvent = persistentAuditEvents.getFirst();

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo("admin@localhost.com");

        return persistentAuditEvent.getData();
    }

    @Configuration
    static class GitConfigurationFacadeConfiguration {

        @Bean
        GitConfigurationFacade gitConfigurationFacade(PropertyService propertyService) {
            return new GitConfigurationFacadeImpl(propertyService);
        }
    }
}
