/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.app.config.AppIntTestConfiguration;
import com.bytechef.ee.automation.app.domain.App;
import com.bytechef.ee.automation.app.domain.AppApiCollection;
import com.bytechef.ee.automation.app.domain.AppHostingTarget;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppInstanceStatus;
import com.bytechef.ee.automation.app.repository.AppApiCollectionRepository;
import com.bytechef.ee.automation.app.repository.AppInstanceRepository;
import com.bytechef.ee.automation.app.repository.AppRepository;
import com.bytechef.ee.automation.app.service.AppApiCollectionService.BindResult;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 */
@ActiveProfiles("testint")
@SpringBootTest(classes = AppIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
class AppApiCollectionServiceIntTest {

    @Autowired
    private AppApiCollectionRepository appApiCollectionRepository;

    @Autowired
    private AppApiCollectionService appApiCollectionService;

    @Autowired
    private AppInstanceRepository appInstanceRepository;

    @Autowired
    private AppRepository appRepository;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testBindIsIdempotent() {
        AppInstance appInstance = saveAppInstance();

        BindResult firstBindResult = appApiCollectionService.bind(appInstance.getId(), 21L);
        BindResult secondBindResult = appApiCollectionService.bind(appInstance.getId(), 21L);

        AppApiCollection first = firstBindResult.appApiCollection();
        AppApiCollection second = secondBindResult.appApiCollection();

        assertThat(firstBindResult.created()).isTrue();
        assertThat(secondBindResult.created()).isFalse();
        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getApiCollectionId()).isEqualTo(21L);
        assertThat(appApiCollectionRepository.findAllByApiCollectionId(21L)).hasSize(1);
        assertThat(appApiCollectionService.isBound(appInstance.getId(), 21L)).isTrue();
        assertThat(appApiCollectionService.isBound(appInstance.getId(), 22L)).isFalse();
    }

    @Test
    void testUnbindReturnsFalseWhenAbsent() {
        AppInstance appInstance = saveAppInstance();

        assertThat(appApiCollectionService.unbind(appInstance.getId(), 21L)).isFalse();

        appApiCollectionService.bind(appInstance.getId(), 21L);

        assertThat(appApiCollectionService.unbind(appInstance.getId(), 21L)).isTrue();
        assertThat(appApiCollectionService.isBound(appInstance.getId(), 21L)).isFalse();
    }

    @Test
    void testGetBindingsOrdersByCollectionId() {
        AppInstance appInstance = saveAppInstance();

        appApiCollectionService.bind(appInstance.getId(), 30L);
        appApiCollectionService.bind(appInstance.getId(), 10L);
        appApiCollectionService.bind(appInstance.getId(), 20L);

        assertThat(appApiCollectionService.getBindings(appInstance.getId()))
            .extracting(AppApiCollection::getApiCollectionId)
            .containsExactly(10L, 20L, 30L);
    }

    @Test
    void testDeleteBindingsRemovesOnlyTheBindingsOfTheInstance() {
        AppInstance appInstance = saveAppInstance();

        appApiCollectionService.bind(appInstance.getId(), 10L);
        appApiCollectionService.bind(appInstance.getId(), 20L);

        appApiCollectionService.deleteBindings(appInstance.getId());

        assertThat(appApiCollectionService.getBindings(appInstance.getId())).isEmpty();
        assertThat(appInstanceRepository.findById(appInstance.getId())).isPresent();
    }

    @Test
    void testDeletingAppCascades() {
        AppInstance appInstance = saveAppInstance();

        appApiCollectionService.bind(appInstance.getId(), 21L);

        appRepository.deleteById(appInstance.getAppId());

        assertThat(appApiCollectionRepository.findAllByAppInstanceIdOrderByApiCollectionId(appInstance.getId()))
            .isEmpty();
        assertThat(appApiCollectionRepository.findAllByApiCollectionId(21L)).isEmpty();
    }

    private AppInstance saveAppInstance() {
        App app = new App();

        app.setUuid(UUID.randomUUID()
            .toString());
        app.setName("crm-dashboard");
        app.setWorkspaceId(1L);
        app.setOwnerUserId(1L);
        app.setVisibility(ResourceVisibility.WORKSPACE);

        App savedApp = appRepository.save(app);

        AppInstance appInstance = new AppInstance();

        String secretKeyHash = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .repeat(2);

        appInstance.setUuid(UUID.randomUUID()
            .toString());
        appInstance.setAppId(savedApp.getId());
        appInstance.setName("Production");
        appInstance.setWorkspaceId(1L);
        appInstance.setOwnerUserId(1L);
        appInstance.setEnvironment(Environment.PRODUCTION);
        appInstance.setStatus(AppInstanceStatus.ACTIVE);
        appInstance.setHostingTarget(AppHostingTarget.VERCEL);
        appInstance.setSecretKeyHash(secretKeyHash);
        appInstance.setSecretKeyHint(secretKeyHash.substring(0, 8));

        return appInstanceRepository.save(appInstance);
    }
}
