/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppApiCollection;
import com.bytechef.ee.automation.app.repository.AppApiCollectionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
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
public class AppApiCollectionServiceImpl implements AppApiCollectionService {

    private final AppApiCollectionRepository appApiCollectionRepository;

    @SuppressFBWarnings("EI")
    public AppApiCollectionServiceImpl(AppApiCollectionRepository appApiCollectionRepository) {
        this.appApiCollectionRepository = appApiCollectionRepository;
    }

    @Override
    public BindResult bind(long appId, long apiCollectionId) {
        Optional<AppApiCollection> existing = appApiCollectionRepository.findByAppIdAndApiCollectionId(
            appId, apiCollectionId);

        if (existing.isPresent()) {
            return new BindResult(existing.get(), false);
        }

        AppApiCollection appApiCollection = new AppApiCollection();

        appApiCollection.setApiCollectionId(apiCollectionId);
        appApiCollection.setAppId(appId);

        return new BindResult(appApiCollectionRepository.save(appApiCollection), true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppApiCollection> getBindings(long appId) {
        return appApiCollectionRepository.findAllByAppIdOrderByApiCollectionId(appId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBound(long appId, long apiCollectionId) {
        return appApiCollectionRepository.findByAppIdAndApiCollectionId(appId, apiCollectionId)
            .isPresent();
    }

    @Override
    public boolean unbind(long appId, long apiCollectionId) {
        Optional<AppApiCollection> binding = appApiCollectionRepository.findByAppIdAndApiCollectionId(
            appId, apiCollectionId);

        binding.ifPresent(appApiCollectionRepository::delete);

        return binding.isPresent();
    }
}
