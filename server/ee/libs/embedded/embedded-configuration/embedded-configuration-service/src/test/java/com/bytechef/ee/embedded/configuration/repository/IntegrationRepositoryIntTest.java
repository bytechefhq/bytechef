/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ee.embedded.configuration.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationVersion;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationRepositoryIntTest {

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void afterEach() {
        integrationRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testCreate() {
        Integration integration = integrationRepository.save(getIntegration());

        assertThat(integration).isEqualTo(
            OptionalUtils.get(integrationRepository.findById(Validate.notNull(integration.getId(), "id"))));
    }

    @Test
    void testDelete() {
        Integration integration = integrationRepository.save(getIntegration());

        Integration resultIntegration = OptionalUtils.get(
            integrationRepository.findById(Validate.notNull(integration.getId(), "id")));

        assertThat(resultIntegration).isEqualTo(integration);

        integrationRepository.deleteById(Validate.notNull(resultIntegration.getId(), "id"));

        assertThat(integrationRepository.findById(integration.getId()))
            .isEmpty();
    }

    @Test
    void testFindAllIntegrations() {
        Tag firstTag = tagRepository.save(new Tag("first"));
        Tag secondTag = tagRepository.save(new Tag("second"));

        Integration betaIntegration = integrationRepository.save(
            getPublishedIntegration("Beta", List.of(firstTag.getId(), secondTag.getId())));
        Integration alphaIntegration = integrationRepository.save(getIntegration("Alpha"));

        List<Integration> integrations = integrationRepository.findAllIntegrations(null, List.of(), null, null);

        assertThat(integrations)
            .extracting(Integration::getName)
            .containsExactly("Alpha", "Beta");

        Integration resultAlphaIntegration = integrations.getFirst();

        assertThat(resultAlphaIntegration.isMultipleInstances()).isFalse();
        assertThat(resultAlphaIntegration.getCreatedDate()).isNotNull();
        assertThat(resultAlphaIntegration.getIntegrationVersions())
            .containsExactlyElementsOf(alphaIntegration.getIntegrationVersions());

        Integration resultBetaIntegration = integrations.getLast();

        assertThat(resultBetaIntegration.isMultipleInstances()).isTrue();
        assertThat(resultBetaIntegration.getTagIds()).containsExactlyInAnyOrder(firstTag.getId(), secondTag.getId());
        assertThat(resultBetaIntegration.getIntegrationVersions())
            .containsExactlyElementsOf(betaIntegration.getIntegrationVersions());
        assertThat(resultBetaIntegration.getLastModifiedDate()).isNotNull();
    }

    @Test
    void testFindAllIntegrationsWithFilters() {
        Tag firstTag = tagRepository.save(new Tag("first"));
        Tag unusedTag = tagRepository.save(new Tag("unused"));

        Integration betaIntegration = integrationRepository.save(
            getPublishedIntegration("Beta", List.of(firstTag.getId())));
        Integration alphaIntegration = integrationRepository.save(getIntegration("Alpha"));

        assertThat(
            integrationRepository.findAllIntegrations(null, List.of(), null,
                IntegrationVersion.Status.PUBLISHED.ordinal()))
                    .extracting(Integration::getId)
                    .containsExactly(betaIntegration.getId());

        assertThat(integrationRepository.findAllIntegrations(null, List.of(), firstTag.getId(), null))
            .extracting(Integration::getId)
            .containsExactly(betaIntegration.getId());

        assertThat(integrationRepository.findAllIntegrations(null, List.of(alphaIntegration.getId()), null, null))
            .extracting(Integration::getId)
            .containsExactly(alphaIntegration.getId());

        assertThat(integrationRepository.findAllIntegrations(null, List.of(), unusedTag.getId(), null)).isEmpty();
    }

    @Test
    void testFindById() {
        Integration integration = integrationRepository.save(getIntegration());

        Integration resultIntegration = OptionalUtils.get(
            integrationRepository.findById(Validate.notNull(integration.getId(), "id")));

        assertThat(resultIntegration).isEqualTo(integration);
    }

    @Test
    void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration());

        integrationRepository.save(integration);

        assertThat(integrationRepository.findById(Validate.notNull(integration.getId(), "id")))
            .hasValue(integration);
    }

    private static Integration getIntegration() {
        return getIntegration("Name");
    }

    private static Integration getIntegration(String name) {
        return Integration.builder()
            .componentName("componentName")
            .name(name)
            .build();
    }

    private static Integration getPublishedIntegration(String name, List<Long> tagIds) {
        Integration integration = Integration.builder()
            .componentName("componentName")
            .name(name)
            .tagIds(tagIds)
            .build();

        integration.setMultipleInstances(true);
        integration.setIntegrationVersions(
            List.of(
                new IntegrationVersion(
                    1, IntegrationVersion.Status.PUBLISHED.ordinal(), Instant.now()
                        .truncatedTo(ChronoUnit.MILLIS),
                    "description")));

        return integration;
    }
}
