/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.EmbeddedGettingStartedDTO;
import com.bytechef.ee.embedded.configuration.dto.LastFailedCallDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class EmbeddedGettingStartedFacadeImpl implements EmbeddedGettingStartedFacade {

    private static final String COMPLETION_TYPE = "completionType";
    private static final String GETTING_STARTED = "gettingStarted";

    private final ApiKeyService apiKeyService;
    private final ConnectedUserService connectedUserService;
    private final ExternalInvocationLogService externalInvocationLogService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final PropertyService propertyService;

    @SuppressFBWarnings("EI")
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public EmbeddedGettingStartedFacadeImpl(
        ApiKeyService apiKeyService, ConnectedUserService connectedUserService,
        ExternalInvocationLogService externalInvocationLogService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService, PropertyService propertyService) {

        this.apiKeyService = apiKeyService;
        this.connectedUserService = connectedUserService;
        this.externalInvocationLogService = externalInvocationLogService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.propertyService = propertyService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void dismissEmbeddedGettingStarted() {
        complete(CompletionType.DISMISSED);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public EmbeddedGettingStartedDTO getEmbeddedGettingStarted() {
        Set<Long> publishedIntegrationIds = getPublishedIntegrationIds();

        Optional<String> completionTypeNameOptional = fetchCompletionTypeName();

        EmbeddedGettingStartedDTO embeddedGettingStartedDTO =
            evaluate(Environment.DEVELOPMENT, publishedIntegrationIds);

        if (completionTypeNameOptional.isEmpty() && isSetupComplete(embeddedGettingStartedDTO)) {
            complete(CompletionType.COMPLETED);

            completionTypeNameOptional = fetchCompletionTypeName();
        }

        return new EmbeddedGettingStartedDTO(
            embeddedGettingStartedDTO.apiKeyCreated(), embeddedGettingStartedDTO.integrationCreated(),
            embeddedGettingStartedDTO.integrationHasWorkflows(), embeddedGettingStartedDTO.integrationPublished(),
            embeddedGettingStartedDTO.integrationConfigurationCreated(),
            embeddedGettingStartedDTO.integrationConfigurationEnabled(), embeddedGettingStartedDTO.testUserAdded(),
            embeddedGettingStartedDTO.testUserConnected(), embeddedGettingStartedDTO.testUserExternalId(),
            embeddedGettingStartedDTO.firstCallMade(),
            embeddedGettingStartedDTO.lastFailedCall(), completionTypeNameOptional.isPresent(),
            completionTypeNameOptional.orElse(null),
            embeddedGettingStartedDTO.integrationId(), embeddedGettingStartedDTO.integrationInstanceConfigurationId(),
            embeddedGettingStartedDTO.testUserId(), embeddedGettingStartedDTO.componentName(),
            embeddedGettingStartedDTO.componentVersion());
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public boolean isEmbeddedGettingStartedCompleted() {
        Optional<String> completionTypeNameOptional = fetchCompletionTypeName();

        if (completionTypeNameOptional.isPresent()) {
            return true;
        }

        if (!isSetupComplete(Environment.DEVELOPMENT, getPublishedIntegrationIds())) {
            return false;
        }

        complete(CompletionType.COMPLETED);

        return true;
    }

    private void complete(CompletionType completionType) {
        Optional<String> completionTypeNameOptional = fetchCompletionTypeName();

        if (completionTypeNameOptional.isEmpty()) {
            propertyService.save(GETTING_STARTED, Map.of(COMPLETION_TYPE, completionType.name()), Scope.EMBEDDED, null);
        }
    }

    private Optional<String> fetchCompletionTypeName() {
        List<Property> properties = propertyService.getProperties(List.of(GETTING_STARTED), Scope.EMBEDDED, null);

        return properties.stream()
            .findFirst()
            .map(property -> (String) property.get(COMPLETION_TYPE));
    }

    private Set<Long> getPublishedIntegrationIds() {
        List<Integration> integrations = integrationService.getIntegrations();

        return integrations.stream()
            .filter(Integration::isPublished)
            .map(Integration::getId)
            .collect(Collectors.toSet());
    }

    private boolean hasApiKey(Environment environment) {
        List<ApiKey> apiKeys = apiKeyService.getApiKeys(environment.ordinal(), PlatformType.EMBEDDED);

        return !apiKeys.isEmpty();
    }

    private List<IntegrationInstanceConfiguration> getPublishedIntegrationInstanceConfigurations(
        Environment environment, Set<Long> publishedIntegrationIds) {

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(environment, null, null);

        return integrationInstanceConfigurations.stream()
            .filter(integrationInstanceConfiguration -> publishedIntegrationIds.contains(
                integrationInstanceConfiguration.getIntegrationId()))
            .toList();
    }

    private boolean isConfigurationEnabled(Environment environment, Set<Long> publishedIntegrationIds) {
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(environment, true);

        return integrationInstanceConfigurations.stream()
            .map(IntegrationInstanceConfiguration::getIntegrationId)
            .anyMatch(publishedIntegrationIds::contains);
    }

    private boolean isFirstCallMade(Environment environment) {
        return externalInvocationLogService.hasExternalInvocationLog(
            PlatformType.EMBEDDED, environment.ordinal(), ExternalInvocationOutcome.SUCCESS);
    }

    private boolean isSetupComplete(Environment environment, Set<Long> publishedIntegrationIds) {
        if (!hasApiKey(environment) || !isConfigurationEnabled(environment, publishedIntegrationIds)) {
            return false;
        }

        Optional<IntegrationInstance> runnableIntegrationInstanceOptional =
            integrationInstanceService.fetchFirstRunnableIntegrationInstance(environment);

        return runnableIntegrationInstanceOptional.isPresent() && isFirstCallMade(environment);
    }

    private EmbeddedGettingStartedDTO evaluate(Environment environment, Set<Long> publishedIntegrationIds) {
        List<Integration> integrations = integrationService.getIntegrations();

        Optional<IntegrationInstance> runnableIntegrationInstanceOptional =
            integrationInstanceService.fetchFirstRunnableIntegrationInstance(environment);

        String testUserExternalId = runnableIntegrationInstanceOptional
            .map(this::getConnectedUserExternalId)
            .orElse(null);

        boolean firstCallMade = isFirstCallMade(environment);

        LastFailedCallDTO lastFailedCall = null;

        if (!firstCallMade) {
            Optional<ExternalInvocationLog> lastFailedExternalInvocationLogOptional =
                externalInvocationLogService.fetchLastFailedExternalInvocationLog(
                    PlatformType.EMBEDDED, environment.ordinal());

            lastFailedCall = lastFailedExternalInvocationLogOptional
                .map(EmbeddedGettingStartedFacadeImpl::toLastFailedCallDTO)
                .orElse(null);
        }

        List<IntegrationInstanceConfiguration> publishedIntegrationInstanceConfigurations =
            getPublishedIntegrationInstanceConfigurations(environment, publishedIntegrationIds);

        Optional<IntegrationInstanceConfiguration> newestIntegrationInstanceConfigurationOptional =
            publishedIntegrationInstanceConfigurations.stream()
                .max(Comparator.comparing(IntegrationInstanceConfiguration::getId));

        Long integrationInstanceConfigurationId = newestIntegrationInstanceConfigurationOptional
            .map(IntegrationInstanceConfiguration::getId)
            .orElse(null);

        Long targetIntegrationId = getTargetIntegrationId(integrations, publishedIntegrationIds);

        Optional<IntegrationInstanceConfiguration> componentIntegrationInstanceConfigurationOptional =
            runnableIntegrationInstanceOptional
                .map(this::getIntegrationInstanceConfiguration)
                .or(() -> newestIntegrationInstanceConfigurationOptional);

        Long componentIntegrationId = componentIntegrationInstanceConfigurationOptional
            .map(IntegrationInstanceConfiguration::getIntegrationId)
            .orElse(targetIntegrationId);

        Optional<Integration> componentIntegrationOptional = integrations.stream()
            .filter(integration -> Objects.equals(integration.getId(), componentIntegrationId))
            .findFirst();

        Long testUserId = runnableIntegrationInstanceOptional
            .map(IntegrationInstance::getConnectedUserId)
            .orElseGet(() -> getLastEnabledConnectedUserId(environment));

        return new EmbeddedGettingStartedDTO(
            hasApiKey(environment), !integrations.isEmpty(), hasIntegrationWorkflows(integrations, targetIntegrationId),
            !publishedIntegrationIds.isEmpty(),
            !publishedIntegrationInstanceConfigurations.isEmpty(),
            isConfigurationEnabled(environment, publishedIntegrationIds), testUserId != null,
            testUserExternalId != null, testUserExternalId, firstCallMade, lastFailedCall, false, null,
            targetIntegrationId, integrationInstanceConfigurationId, testUserId,
            componentIntegrationOptional.map(Integration::getComponentName)
                .orElse(null),
            componentIntegrationOptional.map(Integration::getComponentVersion)
                .orElse(null));
    }

    private @Nullable Long getLastEnabledConnectedUserId(Environment environment) {
        Optional<ConnectedUser> lastEnabledConnectedUserOptional =
            connectedUserService.fetchLastEnabledConnectedUser(environment);

        return lastEnabledConnectedUserOptional
            .map(ConnectedUser::getId)
            .orElse(null);
    }

    private String getConnectedUserExternalId(IntegrationInstance integrationInstance) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(integrationInstance.getConnectedUserId());

        return connectedUser.getExternalId();
    }

    private IntegrationInstanceConfiguration getIntegrationInstanceConfiguration(
        IntegrationInstance integrationInstance) {

        return integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
            integrationInstance.getIntegrationInstanceConfigurationId());
    }

    private boolean hasIntegrationWorkflows(List<Integration> integrations, @Nullable Long integrationId) {
        return integrations.stream()
            .filter(integration -> Objects.equals(integration.getId(), integrationId))
            .findFirst()
            .map(integration -> !integrationWorkflowService
                .getIntegrationWorkflowIds(integration.getId(), integration.getLastIntegrationVersion())
                .isEmpty())
            .orElse(false);
    }

    private static @Nullable Long getTargetIntegrationId(
        List<Integration> integrations, Set<Long> publishedIntegrationIds) {

        if (!publishedIntegrationIds.isEmpty()) {
            return publishedIntegrationIds.stream()
                .max(Long::compare)
                .orElse(null);
        }

        return integrations.stream()
            .map(Integration::getId)
            .max(Long::compare)
            .orElse(null);
    }

    private static boolean isSetupComplete(EmbeddedGettingStartedDTO embeddedGettingStartedDTO) {
        return embeddedGettingStartedDTO.apiKeyCreated() && embeddedGettingStartedDTO.integrationConfigurationEnabled()
            && embeddedGettingStartedDTO.testUserConnected() && embeddedGettingStartedDTO.firstCallMade();
    }

    private static LastFailedCallDTO toLastFailedCallDTO(ExternalInvocationLog externalInvocationLog) {
        ExternalInvocationOutcome outcome = externalInvocationLog.getOutcome();

        return new LastFailedCallDTO(
            outcome.name(), externalInvocationLog.getErrorMessage(), externalInvocationLog.getCreatedDate());
    }

    private enum CompletionType {
        COMPLETED, DISMISSED
    }
}
