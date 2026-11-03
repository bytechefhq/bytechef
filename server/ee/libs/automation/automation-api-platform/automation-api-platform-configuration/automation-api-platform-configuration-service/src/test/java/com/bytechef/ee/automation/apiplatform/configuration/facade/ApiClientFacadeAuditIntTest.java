/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.apiplatform.configuration.audit.ApiClientAuditEvents;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfiguration;
import com.bytechef.ee.automation.apiplatform.configuration.config.ApiCollectionIntTestConfigurationSharedMocks;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.repository.ApiClientRepository;
import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.AuditEventFilter;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ApiCollectionIntTestConfiguration.class, ApiClientFacadeAuditIntTest.AuditConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.audit.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@ApiCollectionIntTestConfigurationSharedMocks
class ApiClientFacadeAuditIntTest {

    private static final String ADMIN_LOGIN = "admin";
    private static final long UNKNOWN_API_CLIENT_ID = Long.MAX_VALUE;

    @Autowired
    private ApiClientFacade apiClientFacade;

    @Autowired
    private ApiClientRepository apiClientRepository;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @BeforeEach
    void beforeEach() {
        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(ADMIN_LOGIN, "n/a", List.of()));

        persistenceAuditEventRepository.deleteAll();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        apiClientRepository.deleteAll();
        persistenceAuditEventRepository.deleteAll();
    }

    @Test
    void testCreateRecordsTheClientWithoutItsSecretKey() {
        ApiClient apiClient = createApiClient("audited-client");

        String secretKey = apiClientFacade.createApiClient(apiClient);

        PersistentAuditEvent persistentAuditEvent = findSingleEvent(ApiClientAuditEvents.API_CLIENT_CREATED);

        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(ADMIN_LOGIN);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiClientId", String.valueOf(apiClient.getId()))
            .containsEntry("name", "audited-client")
            .doesNotContainValue(secretKey)
            .doesNotContainKey("dataError");
    }

    @Test
    void testUpdateAndDeleteRecordTheClient() {
        ApiClient apiClient = createApiClient("audited-client");

        apiClientFacade.createApiClient(apiClient);

        persistenceAuditEventRepository.deleteAll();

        ApiClient renamedApiClient = createApiClient("renamed-client");

        renamedApiClient.setId(apiClient.getId());

        apiClientFacade.updateApiClient(renamedApiClient);

        assertThat(findSingleEvent(ApiClientAuditEvents.API_CLIENT_UPDATED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiClientId", String.valueOf(apiClient.getId()))
            .containsEntry("name", "renamed-client")
            .doesNotContainKey("dataError");

        apiClientFacade.deleteApiClient(apiClient.getId());

        assertThat(findSingleEvent(ApiClientAuditEvents.API_CLIENT_DELETED).getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("apiClientId", String.valueOf(apiClient.getId()));
    }

    @Test
    void testUpdateOfAnUnknownClientRecordsAnError() {
        ApiClient apiClient = createApiClient("renamed-client");

        apiClient.setId(UNKNOWN_API_CLIENT_ID);

        assertThatThrownBy(() -> apiClientFacade.updateApiClient(apiClient))
            .isInstanceOf(RuntimeException.class);

        assertThat(findSingleEvent(ApiClientAuditEvents.API_CLIENT_UPDATED).getData())
            .containsEntry("result", "ERROR")
            .containsEntry("apiClientId", String.valueOf(UNKNOWN_API_CLIENT_ID))
            .containsKey("errorClass")
            .doesNotContainKey("name");
    }

    private static ApiClient createApiClient(String name) {
        ApiClient apiClient = new ApiClient();

        apiClient.setName(name);

        return apiClient;
    }

    private PersistentAuditEvent findSingleEvent(String eventType) {
        Page<PersistentAuditEvent> page = persistenceAuditEventRepository.findAllFiltered(
            AuditEventFilter.empty(), PageRequest.of(0, 25));

        List<PersistentAuditEvent> pageContent = page.getContent();

        List<PersistentAuditEvent> persistentAuditEvents = pageContent.stream()
            .filter(persistentAuditEvent -> eventType.equals(persistentAuditEvent.getEventType()))
            .toList();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    @Configuration
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditEventService.class, AuditMapperResolver.class
    })
    static class AuditConfiguration {
    }
}
