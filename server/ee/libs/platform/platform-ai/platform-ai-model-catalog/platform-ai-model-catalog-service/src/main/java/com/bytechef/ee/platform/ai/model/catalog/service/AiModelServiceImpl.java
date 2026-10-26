/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.model.catalog.service;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import com.bytechef.ee.platform.ai.model.catalog.repository.AiModelRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
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
@SuppressFBWarnings("EI")
class AiModelServiceImpl implements AiModelService {

    private final List<AiModelDeleteListener> aiModelDeleteListeners;
    private final AiModelRepository aiModelRepository;

    AiModelServiceImpl(
        ObjectProvider<AiModelDeleteListener> aiModelDeleteListenerProvider, AiModelRepository aiModelRepository) {
        this.aiModelDeleteListeners = aiModelDeleteListenerProvider.orderedStream()
            .toList();
        this.aiModelRepository = aiModelRepository;
    }

    @Override
    public AiModel create(AiModel model) {
        Validate.notNull(model, "'model' must not be null");
        Validate.isTrue(model.getId() == null, "'id' must be null");

        return aiModelRepository.save(model);
    }

    @Override
    public void delete(long id) {
        for (AiModelDeleteListener aiModelDeleteListener : aiModelDeleteListeners) {
            aiModelDeleteListener.beforeDelete(id);
        }

        aiModelRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiModel> findByModelIdentifier(String modelIdentifier) {
        return aiModelRepository.findFirstByNameOrderByIdAsc(modelIdentifier);
    }

    @Override
    @Transactional(readOnly = true)
    public AiModel getModel(long id) {
        return aiModelRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Model not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public AiModel getModel(long providerId, String name) {
        return aiModelRepository.findByProviderIdAndName(providerId, name)
            .orElseThrow(() -> new IllegalArgumentException(
                "Model not found: providerId=" + providerId + ", name=" + name));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModel> getModels() {
        return aiModelRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModel> getModelsByProviderId(long providerId) {
        return aiModelRepository.findAllByProviderId(providerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModel> getEnabledModels() {
        return aiModelRepository.findAllByEnabled(true);
    }

    @Override
    public AiModel update(AiModel model) {
        Validate.notNull(model, "'model' must not be null");

        AiModel existingModel = getModel(model.getId());

        if (catalogOwnedFieldChanged(existingModel, model)) {
            existingModel.setCatalogPinned(true);
        }

        return applyAndSave(existingModel, model);
    }

    @Override
    public AiModel updateFromCatalog(AiModel model) {
        Validate.notNull(model, "'model' must not be null");

        return applyAndSave(getModel(model.getId()), model);
    }

    @Override
    public AiModel unpin(long id) {
        AiModel model = getModel(id);

        model.setCatalogPinned(false);

        return aiModelRepository.save(model);
    }

    private AiModel applyAndSave(AiModel existingModel, AiModel model) {
        existingModel.setAlias(model.getAlias());
        existingModel.setCapabilities(model.getCapabilities());
        existingModel.setContextWindow(model.getContextWindow());
        existingModel.setDefaultRoutingPolicyId(model.getDefaultRoutingPolicyId());
        existingModel.setEnabled(model.isEnabled());
        existingModel.setInputCostPerMTokens(model.getInputCostPerMTokens());
        existingModel.setName(model.getName());
        existingModel.setOutputCostPerMTokens(model.getOutputCostPerMTokens());

        return aiModelRepository.save(existingModel);
    }

    private static boolean catalogOwnedFieldChanged(AiModel existingModel, AiModel model) {
        return valueChanged(existingModel.getContextWindow(), model.getContextWindow())
            || valueChanged(existingModel.getCapabilities(), model.getCapabilities())
            || decimalChanged(existingModel.getInputCostPerMTokens(), model.getInputCostPerMTokens())
            || decimalChanged(existingModel.getOutputCostPerMTokens(), model.getOutputCostPerMTokens());
    }

    private static boolean decimalChanged(@Nullable BigDecimal existingValue, @Nullable BigDecimal value) {
        if (existingValue == null || value == null) {
            return existingValue != value;
        }

        return existingValue.compareTo(value) != 0;
    }

    private static boolean valueChanged(@Nullable Object existingValue, @Nullable Object value) {
        return !Objects.equals(existingValue, value);
    }
}
