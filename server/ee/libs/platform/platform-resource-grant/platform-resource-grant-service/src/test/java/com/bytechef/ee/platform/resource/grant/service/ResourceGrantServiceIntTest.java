/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.resource.grant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.bytechef.ee.platform.resource.grant.config.ResourceGrantIntTestConfiguration;
import com.bytechef.ee.platform.resource.grant.repository.ResourceGrantRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ResourceGrantIntTestConfiguration.class, properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
public class ResourceGrantServiceIntTest {

    private static final String CONNECTION = "Connection";

    @Autowired
    private ResourceGrantRepository resourceGrantRepository;

    @Autowired
    private ResourceGrantService resourceGrantService;

    @AfterEach
    public void afterEach() {
        resourceGrantRepository.deleteAll();
    }

    @Test
    public void testGrantIsIdempotent() {
        resourceGrantService.grant(CONNECTION, 10L, 7L);
        resourceGrantService.grant(CONNECTION, 10L, 7L);

        assertThat(resourceGrantService.getGrantedUserIds(CONNECTION, 10L)).containsExactly(7L);
    }

    @Test
    public void testRevokeRemovesTheGrant() {
        resourceGrantService.grant(CONNECTION, 11L, 7L);
        resourceGrantService.revoke(CONNECTION, 11L, 7L);

        assertThat(resourceGrantService.getGrantedUserIds(CONNECTION, 11L)).isEmpty();
    }

    @Test
    public void testRevokeOfAbsentGrantIsSilent() {
        assertThatCode(() -> resourceGrantService.revoke(CONNECTION, 12L, 7L)).doesNotThrowAnyException();

        assertThat(resourceGrantService.getGrantedUserIds(CONNECTION, 12L)).isEmpty();
    }

    @Test
    public void testFilterGrantedResourceIdsReturnsOnlyGrantedSubset() {
        resourceGrantService.grant(CONNECTION, 20L, 7L);
        resourceGrantService.grant(CONNECTION, 22L, 7L);
        resourceGrantService.grant(CONNECTION, 21L, 8L);

        Set<Long> grantedIds = resourceGrantService.filterGrantedResourceIds(
            CONNECTION, 7L, List.of(20L, 21L, 22L, 23L));

        assertThat(grantedIds).containsExactlyInAnyOrder(20L, 22L);
    }

    @Test
    public void testFilterGrantedResourceIdsWithEmptyCandidatesReturnsEmpty() {
        resourceGrantService.grant(CONNECTION, 30L, 7L);

        assertThat(resourceGrantService.filterGrantedResourceIds(CONNECTION, 7L, List.of())).isEmpty();
    }

    @Test
    public void testGrantsAreScopedByResourceType() {
        resourceGrantService.grant(CONNECTION, 40L, 7L);

        // resource_id is polymorphic, so the same numeric id in another resource family must not collide.
        assertThat(resourceGrantService.getGrantedUserIds("Project", 40L)).isEmpty();
        assertThat(resourceGrantService.filterGrantedResourceIds("Project", 7L, List.of(40L))).isEmpty();
    }

    @Test
    public void testDeleteGrantsRemovesEveryGrantForTheResource() {
        resourceGrantService.grant(CONNECTION, 50L, 7L);
        resourceGrantService.grant(CONNECTION, 50L, 8L);
        resourceGrantService.grant(CONNECTION, 51L, 7L);

        resourceGrantService.deleteGrants(CONNECTION, 50L);

        assertThat(resourceGrantService.getGrantedUserIds(CONNECTION, 50L)).isEmpty();
        assertThat(resourceGrantService.getGrantedUserIds(CONNECTION, 51L)).containsExactly(7L);
    }

    @Test
    public void testCreatedByAndCreatedDateArePopulated() {
        resourceGrantService.grant(CONNECTION, 60L, 7L);

        assertThat(resourceGrantRepository.findAllByResourceTypeAndResourceId(CONNECTION, 60L))
            .singleElement()
            .satisfies(resourceGrant -> {
                assertThat(resourceGrant.getCreatedDate()).isNotNull();
                assertThat(resourceGrant.getResourceType()).isEqualTo(CONNECTION);
                assertThat(resourceGrant.getResourceId()).isEqualTo(60L);
                assertThat(resourceGrant.getUserId()).isEqualTo(7L);
            });
    }
}
