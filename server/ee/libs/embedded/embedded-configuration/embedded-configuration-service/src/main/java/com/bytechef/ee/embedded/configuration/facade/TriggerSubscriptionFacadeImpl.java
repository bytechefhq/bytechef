/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.configuration.audit.TriggerSubscriptionAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.ee.embedded.configuration.dto.ComponentTriggerDTO;
import com.bytechef.ee.embedded.configuration.dto.TriggerSubscriptionDTO;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionEnableException;
import com.bytechef.ee.embedded.configuration.exception.TriggerSubscriptionErrorType;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.TriggerSubscriptionService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentication;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentications;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
public class TriggerSubscriptionFacadeImpl implements TriggerSubscriptionFacade {

    private static final String FORMULA_PREFIX = "=";
    private static final String TEMPLATE_EXPRESSION_PREFIX = "${";

    private static final Logger log = LoggerFactory.getLogger(TriggerSubscriptionFacadeImpl.class);

    private final Set<String> allowedWebhookHosts;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceService integrationInstanceService;
    private final int maxTriggerSubscriptionsPerConnectedUser;
    private final TriggerDefinitionService triggerDefinitionService;
    private final TriggerSubscriptionManager triggerSubscriptionManager;
    private final TriggerSubscriptionService triggerSubscriptionService;

    @SuppressFBWarnings("EI")
    public TriggerSubscriptionFacadeImpl(
        ApplicationProperties applicationProperties, ComponentDefinitionService componentDefinitionService,
        ConnectedUserService connectedUserService, IntegrationInstanceService integrationInstanceService,
        TriggerDefinitionService triggerDefinitionService, TriggerSubscriptionManager triggerSubscriptionManager,
        TriggerSubscriptionService triggerSubscriptionService) {

        ApplicationProperties.Embedded embedded = applicationProperties.getEmbedded();

        ApplicationProperties.Embedded.TriggerSubscription triggerSubscriptionProperties =
            embedded.getTriggerSubscription();
        ApplicationProperties.WebhookDelivery webhookDelivery = applicationProperties.getWebhookDelivery();

        this.allowedWebhookHosts = Set.copyOf(webhookDelivery.getAllowedHosts());
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceService = integrationInstanceService;
        this.maxTriggerSubscriptionsPerConnectedUser = triggerSubscriptionProperties.getMaxPerConnectedUser();
        this.triggerDefinitionService = triggerDefinitionService;
        this.triggerSubscriptionManager = triggerSubscriptionManager;
        this.triggerSubscriptionService = triggerSubscriptionService;
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_CREATED, data = {
        @AuditData(key = "triggerSubscriptionId", value = "#result?.id"),
        @AuditData(key = "externalUserId", value = "#externalUserId"),
        @AuditData(key = "componentName", value = "#componentName"),
        @AuditData(key = "componentVersion", value = "#componentVersion"),
        @AuditData(key = "triggerName", value = "#triggerName"),
        @AuditData(key = "environment", value = "#environment")
    })
    @PreAuthorize("#externalUserId == authentication.name")
    public TriggerSubscriptionDTO createTriggerSubscription(
        String externalUserId, String componentName, int componentVersion, String triggerName, Map<String, ?> input,
        @Nullable String name, @Nullable String externalId, @Nullable String webhookUrl, @Nullable Long instanceId,
        Environment environment) {

        validateInput(input);
        TriggerSubscriptionRules.validateWebhookUrl(webhookUrl, allowedWebhookHosts);

        if (webhookUrl != null) {
            checkWebhookUrlChangeAllowed();
        }

        Optional<ConnectedUser> connectedUserOptional = connectedUserService.fetchConnectedUser(
            externalUserId, environment);

        if (connectedUserOptional.isEmpty()) {
            throw new ConfigurationException(
                "Connected user %s does not exist".formatted(externalUserId),
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }

        ConnectedUser connectedUser = connectedUserOptional.get();

        long connectedUserId = Objects.requireNonNull(connectedUser.getId());

        checkTriggerSubscriptionLimit(connectedUserId, environment);

        IntegrationInstance integrationInstance = getIntegrationInstance(
            connectedUserId, componentName, instanceId, environment);

        if (integrationInstance != null) {
            TriggerSubscriptionRules.checkEnabled(integrationInstance);
        }

        Long integrationInstanceId = integrationInstance == null ? null : integrationInstance.getId();
        Long connectionId = integrationInstance == null ? null : integrationInstance.getConnectionId();

        TriggerSubscriptionRules.checkTriggerExists(
            triggerDefinitionService, componentName, componentVersion, triggerName);

        if (connectionId == null && isConnectionRequired(componentName, componentVersion)) {
            throw new ConfigurationException(
                "Component %s requires a connection, but the connected user has none".formatted(componentName),
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }

        TriggerSubscription triggerSubscription;

        try {
            triggerSubscription = triggerSubscriptionManager.create(
                connectedUser, integrationInstanceId, connectionId, componentName, componentVersion, triggerName,
                input, name, externalId, webhookUrl, maxTriggerSubscriptionsPerConnectedUser);
        } catch (TriggerSubscriptionEnableException triggerSubscriptionEnableException) {
            throw removeFailedTriggerSubscription(triggerSubscriptionEnableException);
        }

        return toTriggerSubscriptionDTO(triggerSubscription);
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_DELETED, data = {
        @AuditData(key = "triggerSubscriptionId", value = "#id"),
        @AuditData(key = "externalUserId", value = "#externalUserId"),
        @AuditData(key = "environment", value = "#environment")
    })
    @PreAuthorize("#externalUserId == authentication.name")
    public void deleteTriggerSubscription(String externalUserId, long id, Environment environment) {
        TriggerSubscription triggerSubscription = getOwnedTriggerSubscription(externalUserId, id, environment);

        triggerSubscriptionManager.delete(Objects.requireNonNull(triggerSubscription.getId()));
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_ENABLED_UPDATED, data = {
        @AuditData(key = "triggerSubscriptionId", value = "#id"),
        @AuditData(key = "externalUserId", value = "#externalUserId"),
        @AuditData(key = "enabled", value = "#enable"),
        @AuditData(key = "environment", value = "#environment")
    })
    @PreAuthorize("#externalUserId == authentication.name")
    public void enableTriggerSubscription(String externalUserId, long id, boolean enable, Environment environment) {
        TriggerSubscription triggerSubscription = getOwnedTriggerSubscription(externalUserId, id, environment);

        long triggerSubscriptionId = Objects.requireNonNull(triggerSubscription.getId());

        if (enable) {
            TriggerSubscriptionRules.checkIntegrationInstanceEnabled(triggerSubscription, integrationInstanceService);
            TriggerSubscriptionRules.checkConnectedUserEnabled(
                connectedUserService.getConnectedUser(triggerSubscription.getConnectedUserId()));

            triggerSubscriptionManager.enable(triggerSubscriptionId);
        } else {
            triggerSubscriptionManager.disable(triggerSubscriptionId, null);
        }
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public List<ComponentTriggerDTO> getComponentTriggers(
        String externalUserId, String componentName, int componentVersion) {

        return triggerDefinitionService.getTriggerDefinitions(componentName, componentVersion)
            .stream()
            .map(TriggerSubscriptionFacadeImpl::toComponentTriggerDTO)
            .toList();
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    public TriggerSubscriptionDTO getTriggerSubscription(String externalUserId, long id, Environment environment) {
        return toTriggerSubscriptionDTO(getOwnedTriggerSubscription(externalUserId, id, environment));
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    public List<TriggerSubscriptionDTO> getTriggerSubscriptions(String externalUserId, Environment environment) {
        Optional<ConnectedUser> connectedUserOptional = connectedUserService.fetchConnectedUser(
            externalUserId, environment);

        if (connectedUserOptional.isEmpty()) {
            return List.of();
        }

        ConnectedUser connectedUser = connectedUserOptional.get();

        return triggerSubscriptionService
            .getConnectedUserTriggerSubscriptions(Objects.requireNonNull(connectedUser.getId()), environment)
            .stream()
            .map(this::toTriggerSubscriptionDTO)
            .toList();
    }

    @Override
    @Audited(event = TriggerSubscriptionAuditEvents.TRIGGER_SUBSCRIPTION_UPDATED, data = {
        @AuditData(key = "triggerSubscriptionId", value = "#id"),
        @AuditData(key = "externalUserId", value = "#externalUserId"),
        @AuditData(key = "environment", value = "#environment")
    })
    @PreAuthorize("#externalUserId == authentication.name")
    public TriggerSubscriptionDTO updateTriggerSubscription(
        String externalUserId, long id, Map<String, ?> input, @Nullable String name, @Nullable String externalId,
        @Nullable String webhookUrl, Environment environment) {

        TriggerSubscription triggerSubscription = getOwnedTriggerSubscription(externalUserId, id, environment);

        validateInput(input);
        TriggerSubscriptionRules.validateWebhookUrl(webhookUrl, allowedWebhookHosts);

        if (!Objects.equals(StringUtils.trimToNull(webhookUrl), triggerSubscription.getWebhookUrl())) {
            checkWebhookUrlChangeAllowed();
        }

        TriggerSubscriptionRules.checkIntegrationInstanceEnabled(triggerSubscription, integrationInstanceService);

        TriggerSubscription updatedTriggerSubscription = triggerSubscriptionManager.update(
            Objects.requireNonNull(triggerSubscription.getId()), input, name, externalId, webhookUrl);

        return toTriggerSubscriptionDTO(updatedTriggerSubscription);
    }

    private static void checkWebhookUrlChangeAllowed() {
        Optional<ConnectedUserAuthentication> connectedUserAuthenticationOptional =
            ConnectedUserAuthentications.fetchCurrent();

        if (connectedUserAuthenticationOptional.isEmpty()) {
            return;
        }

        ConnectedUserAuthentication connectedUserAuthentication = connectedUserAuthenticationOptional.get();

        if (!connectedUserAuthentication.apiKeyAuthenticated()) {
            throw new ConfigurationException(
                "Only a call authenticated with the environment secret key may set the webhook URL",
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }
    }

    private void checkTriggerSubscriptionLimit(long connectedUserId, Environment environment) {
        if (maxTriggerSubscriptionsPerConnectedUser <= 0) {
            return;
        }

        List<TriggerSubscription> triggerSubscriptions = triggerSubscriptionService
            .getConnectedUserTriggerSubscriptions(connectedUserId, environment);

        TriggerSubscriptionRules.checkTriggerSubscriptionLimit(
            connectedUserId, triggerSubscriptions.size(), maxTriggerSubscriptionsPerConnectedUser);
    }

    private void checkIntegrationInstanceMatches(
        long connectedUserId, IntegrationInstance integrationInstance, String componentName, Environment environment) {

        if (!containsIntegrationInstance(
            integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId, environment),
            integrationInstance)) {

            throw new ConfigurationException(
                "Integration instance %d does not belong to the %s environment".formatted(
                    integrationInstance.getId(), environment),
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }

        if (!containsIntegrationInstance(
            integrationInstanceService.getIntegrationInstances(connectedUserId, componentName, environment),
            integrationInstance)) {

            throw new ConfigurationException(
                "Integration instance %d does not belong to an integration of component %s".formatted(
                    integrationInstance.getId(), componentName),
                TriggerSubscriptionErrorType.USER_INPUT_ERROR);
        }
    }

    @Nullable
    private IntegrationInstance getIntegrationInstance(
        long connectedUserId, String componentName, @Nullable Long instanceId, Environment environment) {

        if (instanceId == null) {
            Optional<IntegrationInstance> integrationInstanceOptional =
                integrationInstanceService.fetchIntegrationInstance(connectedUserId, componentName, environment);

            return integrationInstanceOptional.orElse(null);
        }

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(instanceId);

        if (!Objects.equals(connectedUserId, integrationInstance.getConnectedUserId())) {
            throw new AccessDeniedException(
                "Integration instance " + instanceId + " is not owned by the connected user");
        }

        checkIntegrationInstanceMatches(connectedUserId, integrationInstance, componentName, environment);

        return integrationInstance;
    }

    private TriggerSubscription getOwnedTriggerSubscription(String externalUserId, long id, Environment environment) {
        Optional<ConnectedUser> connectedUserOptional = connectedUserService.fetchConnectedUser(
            externalUserId, environment);

        if (connectedUserOptional.isEmpty()) {
            throw createNotFoundException(id);
        }

        ConnectedUser connectedUser = connectedUserOptional.get();

        Optional<TriggerSubscription> triggerSubscriptionOptional =
            triggerSubscriptionService.fetchTriggerSubscription(id);

        if (triggerSubscriptionOptional.isEmpty()) {
            throw createNotFoundException(id);
        }

        TriggerSubscription triggerSubscription = triggerSubscriptionOptional.get();

        if (!Objects.equals(triggerSubscription.getConnectedUserId(), connectedUser.getId()) ||
            triggerSubscription.getEnvironment() != environment) {

            throw createNotFoundException(id);
        }

        return triggerSubscription;
    }

    private boolean isConnectionRequired(String componentName, int componentVersion) {
        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, componentVersion);

        return componentDefinition.isConnectionRequired();
    }

    private TriggerSubscriptionEnableException removeFailedTriggerSubscription(
        TriggerSubscriptionEnableException triggerSubscriptionEnableException) {

        Long triggerSubscriptionId = triggerSubscriptionEnableException.getTriggerSubscriptionId();

        if (triggerSubscriptionId == null) {
            return triggerSubscriptionEnableException;
        }

        try {
            triggerSubscriptionManager.delete(triggerSubscriptionId);
        } catch (RuntimeException runtimeException) {
            log.error(
                "Unable to remove trigger subscription id={} after its first enable failed", triggerSubscriptionId,
                runtimeException);

            triggerSubscriptionEnableException.addSuppressed(runtimeException);

            return triggerSubscriptionEnableException;
        }

        return new TriggerSubscriptionEnableException(
            null, triggerSubscriptionEnableException.getErrorCode(), triggerSubscriptionEnableException.getMessage(),
            triggerSubscriptionEnableException.getCause());
    }

    private TriggerSubscriptionDTO toTriggerSubscriptionDTO(TriggerSubscription triggerSubscription) {
        return new TriggerSubscriptionDTO(
            triggerSubscription, triggerSubscriptionManager.getStaticWebhookUrl(triggerSubscription));
    }

    private static ConfigurationException createNotFoundException(long id) {
        return new ConfigurationException(
            "Trigger subscription id=%d not found".formatted(id),
            TriggerSubscriptionErrorType.TRIGGER_SUBSCRIPTION_NOT_FOUND);
    }

    private static boolean containsIntegrationInstance(
        List<IntegrationInstance> integrationInstances, IntegrationInstance integrationInstance) {

        return integrationInstances.stream()
            .anyMatch(curIntegrationInstance -> Objects.equals(
                curIntegrationInstance.getId(), integrationInstance.getId()));
    }

    private static boolean isExpression(String value) {
        String trimmedValue = value.trim();

        if (trimmedValue.startsWith(FORMULA_PREFIX) && !trimmedValue.equals(FORMULA_PREFIX)) {
            return true;
        }

        return value.contains(TEMPLATE_EXPRESSION_PREFIX);
    }

    private static ComponentTriggerDTO toComponentTriggerDTO(TriggerDefinition triggerDefinition) {
        String outputSchema = null;
        OutputResponse outputResponse = triggerDefinition.getOutputResponse();

        if (outputResponse != null && outputResponse.outputSchema() instanceof ObjectProperty objectProperty) {
            outputSchema = JsonSchemaGeneratorUtils.generateInputSchema(objectProperty.getProperties());
        }

        return new ComponentTriggerDTO(
            triggerDefinition.getName(), triggerDefinition.getTitle(), triggerDefinition.getDescription(),
            String.valueOf(triggerDefinition.getType()),
            JsonSchemaGeneratorUtils.generateInputSchema(triggerDefinition.getProperties()), outputSchema,
            triggerDefinition.isOutputFunctionDefined());
    }

    private static void validateInput(Map<String, ?> input) {
        validateInputValue(input, "input");
    }

    private static void validateInputValue(@Nullable Object value, String path) {
        if (value instanceof String string) {
            if (isExpression(string)) {
                throw new ConfigurationException(
                    "Expressions are not allowed in trigger subscription input: %s".formatted(path),
                    TriggerSubscriptionErrorType.USER_INPUT_ERROR);
            }
        } else if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                validateInputValue(entry.getValue(), path + "." + entry.getKey());
            }
        } else if (value instanceof Collection<?> collection) {
            int index = 0;

            for (Object item : collection) {
                validateInputValue(item, path + "[" + index + "]");

                index++;
            }
        }
    }
}
