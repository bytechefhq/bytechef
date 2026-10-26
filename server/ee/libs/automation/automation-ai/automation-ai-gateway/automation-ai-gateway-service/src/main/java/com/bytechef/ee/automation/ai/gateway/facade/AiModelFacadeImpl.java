/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.catalog.AiModelCatalogReconciler;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.service.AiModelService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiModelFacadeImpl implements AiModelFacade {

    private final AiModelCatalogReconciler aiModelCatalogReconciler;
    private final AiModelService aiModelService;

    @SuppressFBWarnings("EI")
    AiModelFacadeImpl(
        AiModelService aiModelService, AiModelCatalogReconciler aiModelCatalogReconciler) {
        this.aiModelService = aiModelService;
        this.aiModelCatalogReconciler = aiModelCatalogReconciler;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiModel getModel(long id) {
        return aiModelService.getModel(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiModel> getModels() {
        return aiModelService.getModels();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiModel> getModelsByProviderId(long providerId) {
        return aiModelService.getModelsByProviderId(providerId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiModel create(AiModel model) {
        return aiModelService.create(model);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(long id) {
        aiModelService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiModel update(AiModel model) {
        return aiModelService.update(model);
    }

    @Override
    @Async
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void reconcileCatalog() {
        aiModelCatalogReconciler.reconcile();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AiModel unpin(long id) {
        return aiModelService.unpin(id);
    }
}
