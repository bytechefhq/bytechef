/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.remote.client.EmbeddedConfigurationGrpcTestServer;
import com.bytechef.ee.embedded.configuration.remote.grpc.EmbeddedConfigurationProtoMapper;
import com.bytechef.ee.embedded.configuration.remote.grpc.IntegrationInstanceConfigurationRemoteServiceGrpcServer;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = {
        "bytechef.edition=EE", "spring.liquibase.contexts=configuration"
    })
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class RemoteIntegrationInstanceConfigurationServiceClientIntTest {

    private final List<Long> tagIds = new ArrayList<>();

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagService tagService;

    private EmbeddedConfigurationGrpcTestServer embeddedConfigurationGrpcTestServer;
    private RemoteIntegrationInstanceConfigurationServiceClient remoteIntegrationInstanceConfigurationServiceClient;

    @BeforeEach
    void beforeEach() throws IOException {
        embeddedConfigurationGrpcTestServer = EmbeddedConfigurationGrpcTestServer.start(
            new IntegrationInstanceConfigurationRemoteServiceGrpcServer(
                integrationInstanceConfigurationService, new EmbeddedConfigurationProtoMapper(objectMapper)));

        remoteIntegrationInstanceConfigurationServiceClient = new RemoteIntegrationInstanceConfigurationServiceClient(
            embeddedConfigurationGrpcTestServer.getGrpcChannels(), objectMapper);
    }

    @AfterEach
    void afterEach() {
        embeddedConfigurationGrpcTestServer.shutdown();

        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();

        tagIds.forEach(tagService::delete);

        tagIds.clear();
    }

    @Test
    void testGetIntegrationInstanceConfigurationReturnsTheStoredConfigurationWithItsConnectionParameters() {
        long integrationId = createIntegration();

        IntegrationInstanceConfiguration integrationInstanceConfiguration = createConfiguration(
            integrationId, Environment.PRODUCTION, Map.of("apiKey", "secret", "retries", 3));

        IntegrationInstanceConfiguration remoteIntegrationInstanceConfiguration =
            remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfiguration(
                integrationInstanceConfiguration.getId());

        assertThat(remoteIntegrationInstanceConfiguration).usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                    integrationInstanceConfiguration.getId()));
        assertThat(remoteIntegrationInstanceConfiguration.getConnectionParameters())
            .isEqualTo(Map.of("apiKey", "secret", "retries", 3));
        assertThat(remoteIntegrationInstanceConfiguration.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(remoteIntegrationInstanceConfiguration.getIntegrationId()).isEqualTo(integrationId);
    }

    @Test
    void testGetIntegrationInstanceConfigurationsByIdsReturnsOnlyTheRequestedConfigurations() {
        long integrationId = createIntegration();

        IntegrationInstanceConfiguration productionIntegrationInstanceConfiguration = createConfiguration(
            integrationId, Environment.PRODUCTION, Map.of());
        IntegrationInstanceConfiguration stagingIntegrationInstanceConfiguration = createConfiguration(
            integrationId, Environment.STAGING, Map.of());

        createConfiguration(integrationId, Environment.DEVELOPMENT, Map.of());

        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(
            List.of(productionIntegrationInstanceConfiguration.getId(),
                stagingIntegrationInstanceConfiguration.getId())))
                    .extracting(IntegrationInstanceConfiguration::getId,
                        IntegrationInstanceConfiguration::getEnvironment)
                    .containsExactlyInAnyOrder(
                        tuple(
                            productionIntegrationInstanceConfiguration.getId(), Environment.PRODUCTION),
                        tuple(
                            stagingIntegrationInstanceConfiguration.getId(), Environment.STAGING));
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(List.of()))
            .isEmpty();
    }

    @Test
    void testGetIntegrationInstanceConfigurationsByFilterAppliesEnvironmentIntegrationAndTag() {
        long airtableIntegrationId = createIntegration();
        long slackIntegrationId = createIntegration();

        Tag tag = createTag();

        IntegrationInstanceConfiguration taggedIntegrationInstanceConfiguration = createConfiguration(
            airtableIntegrationId, Environment.PRODUCTION, Map.of());

        integrationInstanceConfigurationService.update(taggedIntegrationInstanceConfiguration.getId(),
            List.of(tag.getId()));

        IntegrationInstanceConfiguration untaggedIntegrationInstanceConfiguration = createConfiguration(
            airtableIntegrationId, Environment.PRODUCTION, Map.of());
        IntegrationInstanceConfiguration stagingIntegrationInstanceConfiguration = createConfiguration(
            airtableIntegrationId, Environment.STAGING, Map.of());
        IntegrationInstanceConfiguration slackIntegrationInstanceConfiguration = createConfiguration(
            slackIntegrationId, Environment.PRODUCTION, Map.of());

        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(
            Environment.PRODUCTION, airtableIntegrationId, tag.getId()))
                .extracting(IntegrationInstanceConfiguration::getId)
                .containsExactly(taggedIntegrationInstanceConfiguration.getId());
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(
            Environment.PRODUCTION, airtableIntegrationId, null))
                .extracting(IntegrationInstanceConfiguration::getId)
                .containsExactlyInAnyOrder(
                    taggedIntegrationInstanceConfiguration.getId(), untaggedIntegrationInstanceConfiguration.getId());
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(
            Environment.STAGING, null, null))
                .extracting(IntegrationInstanceConfiguration::getId)
                .containsExactly(stagingIntegrationInstanceConfiguration.getId());
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(
            null, slackIntegrationId, null))
                .extracting(IntegrationInstanceConfiguration::getId)
                .containsExactly(slackIntegrationInstanceConfiguration.getId());
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(
            null, null, null))
                .hasSize(4);
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.getIntegrationInstanceConfigurations(
            Environment.DEVELOPMENT, null, null))
                .isEmpty();
    }

    @Test
    void testIsIntegrationInstanceConfigurationEnabledReflectsTheStoredFlag() {
        long integrationId = createIntegration();

        IntegrationInstanceConfiguration enabledIntegrationInstanceConfiguration = createConfiguration(
            integrationId, Environment.PRODUCTION, Map.of());
        IntegrationInstanceConfiguration disabledIntegrationInstanceConfiguration = createConfiguration(
            integrationId, Environment.PRODUCTION, Map.of());

        integrationInstanceConfigurationService.updateEnabled(enabledIntegrationInstanceConfiguration.getId(), true);

        assertThat(remoteIntegrationInstanceConfigurationServiceClient.isIntegrationInstanceConfigurationEnabled(
            enabledIntegrationInstanceConfiguration.getId())).isTrue();
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.isIntegrationInstanceConfigurationEnabled(
            disabledIntegrationInstanceConfiguration.getId())).isFalse();
        assertThat(remoteIntegrationInstanceConfigurationServiceClient.isIntegrationInstanceConfigurationEnabled(
            Long.MAX_VALUE)).isFalse();
    }

    private IntegrationInstanceConfiguration createConfiguration(
        long integrationId, Environment environment, Map<String, ?> connectionParameters) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(connectionParameters);
        integrationInstanceConfiguration.setDescription("description");
        integrationInstanceConfiguration.setEnvironment(environment);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName("instance-configuration-" + UUID.randomUUID());

        return integrationInstanceConfigurationService.create(integrationInstanceConfiguration);
    }

    private long createIntegration() {
        Integration integration = new Integration();

        integration.setComponentName("airtable");
        integration.setName("integration-" + UUID.randomUUID());

        integration = integrationService.create(integration);

        return integration.getId();
    }

    private Tag createTag() {
        List<Tag> tags = tagService.save(List.of(new Tag("tag-" + UUID.randomUUID())));

        Tag tag = tags.getFirst();

        tagIds.add(tag.getId());

        return tag;
    }
}
