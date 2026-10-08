/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.ee.embedded.configuration.config.IntegrationIntTestConfigurationSharedMocks;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.repository.IntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.configuration.repository.IntegrationRepository;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        IntegrationIntTestConfiguration.class, IntegrationTagFacadeIntTest.MethodSecurityConfiguration.class
    },
    properties = "bytechef.workflow.repository.jdbc.enabled=true")
@Import(PostgreSQLContainerConfiguration.class)
@IntegrationIntTestConfigurationSharedMocks
class IntegrationTagFacadeIntTest {

    @Autowired
    private IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;

    @Autowired
    private IntegrationInstanceConfigurationRepository integrationInstanceConfigurationRepository;

    @Autowired
    private IntegrationInstanceConfigurationService integrationInstanceConfigurationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationTagFacade integrationTagFacade;

    @Autowired
    private TagRepository tagRepository;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        integrationInstanceConfigurationRepository.deleteAll();
        integrationRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    void testGetIntegrationTagsRequiresTenantAdmin() {
        Tag tag = tagRepository.save(new Tag(uniqueName("integration-tag")));

        createIntegration(List.of(tag));

        authenticate(tenantAdmin());

        assertThat(integrationTagFacade.getIntegrationTags())
            .extracting(Tag::getName)
            .contains(tag.getName());

        assertDeniedToNonAdminAndConnectedUser(() -> integrationTagFacade.getIntegrationTags());
    }

    @Test
    void testUpdateIntegrationTagsRequiresTenantAdmin() {
        Tag existingTag = tagRepository.save(new Tag(uniqueName("integration-tag")));

        long integrationId = createIntegration(List.of(existingTag));

        String deniedTagName = uniqueName("denied-integration-tag");

        assertDeniedToNonAdminAndConnectedUser(
            () -> integrationTagFacade.updateIntegrationTags(integrationId, List.of(new Tag(deniedTagName))));

        Integration integration = integrationService.getIntegration(integrationId);

        assertThat(integration.getTagIds()).containsExactly(existingTag.getId());
        assertThat(tagRepository.findByName(deniedTagName)).isEmpty();

        String grantedTagName = uniqueName("granted-integration-tag");

        authenticate(tenantAdmin());

        integrationTagFacade.updateIntegrationTags(integrationId, List.of(new Tag(grantedTagName)));

        Tag grantedTag = tagRepository.findByName(grantedTagName)
            .orElseThrow();

        integration = integrationService.getIntegration(integrationId);

        assertThat(integration.getTagIds()).containsExactly(grantedTag.getId());
    }

    @Test
    void testGetIntegrationInstanceConfigurationTagsRequiresTenantAdmin() {
        Tag tag = tagRepository.save(new Tag(uniqueName("instance-configuration-tag")));

        createIntegrationInstanceConfiguration(createIntegration(List.of()), List.of(tag));

        authenticate(tenantAdmin());

        assertThat(integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationTags())
            .extracting(Tag::getName)
            .contains(tag.getName());

        assertDeniedToNonAdminAndConnectedUser(
            () -> integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationTags());
    }

    @Test
    void testUpdateIntegrationInstanceConfigurationTagsRequiresTenantAdmin() {
        Tag existingTag = tagRepository.save(new Tag(uniqueName("instance-configuration-tag")));

        long integrationInstanceConfigurationId = createIntegrationInstanceConfiguration(
            createIntegration(List.of()), List.of(existingTag));

        String deniedTagName = uniqueName("denied-instance-configuration-tag");

        assertDeniedToNonAdminAndConnectedUser(
            () -> integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
                integrationInstanceConfigurationId, List.of(new Tag(deniedTagName))));

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        assertThat(integrationInstanceConfiguration.getTagIds()).containsExactly(existingTag.getId());
        assertThat(tagRepository.findByName(deniedTagName)).isEmpty();

        String grantedTagName = uniqueName("granted-instance-configuration-tag");

        authenticate(tenantAdmin());

        integrationInstanceConfigurationFacade.updateIntegrationInstanceConfigurationTags(
            integrationInstanceConfigurationId, List.of(new Tag(grantedTagName)));

        Tag grantedTag = tagRepository.findByName(grantedTagName)
            .orElseThrow();

        integrationInstanceConfiguration = integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
            integrationInstanceConfigurationId);

        assertThat(integrationInstanceConfiguration.getTagIds()).containsExactly(grantedTag.getId());
    }

    private static void assertDeniedToNonAdminAndConnectedUser(ThrowingCallable throwingCallable) {
        for (Authentication authentication : List.of(nonAdmin(), connectedUser())) {
            authenticate(authentication);

            assertThatThrownBy(throwingCallable)
                .as("denied to %s", authentication.getName())
                .isInstanceOf(AccessDeniedException.class);
        }

        SecurityContextHolder.clearContext();
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private static Authentication connectedUser() {
        return new EmbeddedApiKeyAuthenticationToken(
            Environment.PRODUCTION.ordinal(), 1L, new User("external-user-1", "", List.of()), false);
    }

    private long createIntegration(List<Tag> tags) {
        Integration integration = new Integration();

        integration.setComponentName("componentName");
        integration.setName(uniqueName("integration"));
        integration.setTags(tags);

        integration = integrationService.create(integration);

        return integration.getId();
    }

    private long createIntegrationInstanceConfiguration(long integrationId, List<Tag> tags) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setConnectionParameters(Map.of());
        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(1);
        integrationInstanceConfiguration.setName(uniqueName("instance-configuration"));
        integrationInstanceConfiguration.setTags(tags);

        integrationInstanceConfiguration = integrationInstanceConfigurationService.create(
            integrationInstanceConfiguration);

        return integrationInstanceConfiguration.getId();
    }

    private static Authentication nonAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "user", "n/a", List.of(new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    private static Authentication tenantAdmin() {
        return new UsernamePasswordAuthenticationToken(
            "admin", "n/a",
            List.of(
                new SimpleGrantedAuthority(AuthorityConstants.ADMIN),
                new SimpleGrantedAuthority(AuthorityConstants.USER)));
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    @Configuration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    static class MethodSecurityConfiguration {

        @Bean("permissionService")
        PermissionService permissionService() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.isTenantAdmin())
                .thenAnswer(invocation -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN));

            return permissionService;
        }
    }
}
