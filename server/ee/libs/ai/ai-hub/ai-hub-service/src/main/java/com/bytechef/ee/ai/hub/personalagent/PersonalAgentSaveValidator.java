/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

// Gateway-based validation disabled for now — see the commented block at the bottom of this class.
// import com.bytechef.ee.automation.ai.gateway.domain.WorkspaceAiGatewayProvider;
// import com.bytechef.ee.automation.ai.gateway.facade.WorkspaceAiGatewayModelFacade;
// import com.bytechef.ee.automation.ai.gateway.service.WorkspaceAiGatewayProviderService;
// import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModel;
// import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
// import com.bytechef.ee.platform.ai.gateway.service.AiGatewayProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Save-time validator for the per-agent {@code (llmProvider, llmModel)} pair. Invoked from
 * {@code AiHubPersonalAgentServiceImpl#create} and {@code AiHubPersonalAgentServiceImpl#update} when both fields are
 * non-null.
 *
 * <p>
 * The AI Gateway provider/model validation is disabled for now (the gateway dependency was removed from this module).
 * {@link #validate} is a no-op: catalog-based selections (provider keys like {@code "ai.provider.openAi"}) were always
 * resolved at runtime rather than validated here, and with the gateway path disabled every selection degrades to the
 * runtime resolver's warn-and-fallback behavior. The original validation logic is kept as a commented block at the
 * bottom of this class for easy restore.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class PersonalAgentSaveValidator {

    /**
     * No-op for now (gateway-based validation disabled). Kept so callers need no change; see the commented block below
     * to restore the provider/model checks.
     */
    public void validate(long workspaceId, String llmProvider, String llmModel) {
        // Catalog-based selections (provider keys like "ai.provider.openAi") resolve at runtime from the platform AI
        // provider catalog, never here. With the gateway path disabled there is nothing left to validate at save.
//        if (llmProvider.startsWith("ai.provider.")) {
//            return;
//        }

        // Gateway provider/model validation removed for now — see the commented block below to restore.
    }

    // ---------------------------------------------------------------------------------------------------------------
    // AI Gateway save-time validation — disabled for now. Restore by re-adding the gateway imports/fields/constructor
    // params and the validation body below.
    // ---------------------------------------------------------------------------------------------------------------
    //
    // private final WorkspaceAiGatewayProviderService workspaceAiGatewayProviderService;
    // private final WorkspaceAiGatewayModelFacade workspaceAiGatewayModelFacade;
    // private final AiGatewayProviderService aiGatewayProviderService;
    //
    // AiGatewayProvider provider = resolveProvider(workspaceId, llmProvider);
    //
    // if (provider == null) {
    // throw new IllegalArgumentException(
    // "LLM provider '" + llmProvider + "' is not enabled in this workspace. Enable it in AI Gateway " +
    // "settings before assigning it to a personal agent.");
    // }
    //
    // boolean modelFound = false;
    //
    // for (AiGatewayModel model : workspaceAiGatewayModelFacade.getWorkspaceModels(workspaceId)) {
    // if (model == null || !model.isEnabled()) {
    // continue;
    // }
    //
    // if (Objects.equals(model.getProviderId(), provider.getId()) && llmModel.equals(model.getName())) {
    // modelFound = true;
    //
    // break;
    // }
    // }
    //
    // if (!modelFound) {
    // throw new IllegalArgumentException(
    // "LLM model '" + llmModel + "' is not enabled in this workspace under provider '" + llmProvider +
    // "'. Enable it in AI Gateway settings before assigning it to a personal agent.");
    // }
    //
    // private @Nullable AiGatewayProvider resolveProvider(long workspaceId, String llmProvider) {
    // for (WorkspaceAiGatewayProvider workspaceProvider : workspaceAiGatewayProviderService
    // .getWorkspaceProviders(workspaceId)) {
    //
    // AiGatewayProvider provider = aiGatewayProviderService.getProvider(workspaceProvider.getProviderId());
    //
    // if (provider == null || !provider.isEnabled()) {
    // continue;
    // }
    //
    // if (provider.getType()
    // .name()
    // .equalsIgnoreCase(llmProvider)) {
    //
    // return provider;
    // }
    // }
    //
    // return null;
    // }
}
