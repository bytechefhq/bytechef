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
import com.bytechef.ee.automation.app.domain.AppStatus;
import com.bytechef.ee.automation.app.repository.AppApiCollectionRepository;
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
    private AppRepository appRepository;

    @AfterEach
    void afterEach() {
        appRepository.deleteAll();
    }

    @Test
    void testBindIsIdempotent() {
        App app = appRepository.save(newApp());

        BindResult firstBindResult = appApiCollectionService.bind(app.getId(), 21L);
        BindResult secondBindResult = appApiCollectionService.bind(app.getId(), 21L);

        AppApiCollection first = firstBindResult.appApiCollection();
        AppApiCollection second = secondBindResult.appApiCollection();

        assertThat(firstBindResult.created()).isTrue();
        assertThat(secondBindResult.created()).isFalse();
        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getApiCollectionId()).isEqualTo(21L);
        assertThat(appApiCollectionRepository.findAllByApiCollectionId(21L)).hasSize(1);
        assertThat(appApiCollectionService.isBound(app.getId(), 21L)).isTrue();
        assertThat(appApiCollectionService.isBound(app.getId(), 22L)).isFalse();
    }

    @Test
    void testUnbindReturnsFalseWhenAbsent() {
        App app = appRepository.save(newApp());

        assertThat(appApiCollectionService.unbind(app.getId(), 21L)).isFalse();

        appApiCollectionService.bind(app.getId(), 21L);

        assertThat(appApiCollectionService.unbind(app.getId(), 21L)).isTrue();
        assertThat(appApiCollectionService.isBound(app.getId(), 21L)).isFalse();
    }

    @Test
    void testGetBindingsOrdersByCollectionId() {
        App app = appRepository.save(newApp());

        appApiCollectionService.bind(app.getId(), 30L);
        appApiCollectionService.bind(app.getId(), 10L);
        appApiCollectionService.bind(app.getId(), 20L);

        assertThat(appApiCollectionService.getBindings(app.getId()))
            .extracting(AppApiCollection::getApiCollectionId)
            .containsExactly(10L, 20L, 30L);
    }

    @Test
    void testDeletingAppCascades() {
        App app = appRepository.save(newApp());

        appApiCollectionService.bind(app.getId(), 21L);

        appRepository.deleteById(app.getId());

        assertThat(appApiCollectionRepository.findAllByAppIdOrderByApiCollectionId(app.getId())).isEmpty();
        assertThat(appApiCollectionRepository.findAllByApiCollectionId(21L)).isEmpty();
    }

    private static App newApp() {
        App app = new App();

        String secretKeyHash = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .repeat(2);

        app.setUuid(UUID.randomUUID()
            .toString());
        app.setName("crm-dashboard");
        app.setWorkspaceId(1L);
        app.setEnvironment(Environment.PRODUCTION);
        app.setOwnerUserId(1L);
        app.setStatus(AppStatus.ENABLED);
        app.setVisibility(ResourceVisibility.WORKSPACE);
        app.setHostingTarget(AppHostingTarget.VERCEL);
        app.setSecretKeyHash(secretKeyHash);
        app.setSecretKeyHint(secretKeyHash.substring(0, 8));

        return app;
    }
}
