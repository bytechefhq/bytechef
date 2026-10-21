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
    public BindResult bind(long appInstanceId, long apiCollectionId) {
        Optional<AppApiCollection> existing = appApiCollectionRepository.findByAppInstanceIdAndApiCollectionId(
            appInstanceId, apiCollectionId);

        if (existing.isPresent()) {
            return new BindResult(existing.get(), false);
        }

        AppApiCollection appApiCollection = new AppApiCollection();

        appApiCollection.setApiCollectionId(apiCollectionId);
        appApiCollection.setAppInstanceId(appInstanceId);

        return new BindResult(appApiCollectionRepository.save(appApiCollection), true);
    }

    @Override
    public void deleteBindings(long appInstanceId) {
        appApiCollectionRepository.deleteAll(
            appApiCollectionRepository.findAllByAppInstanceIdOrderByApiCollectionId(appInstanceId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppApiCollection> getBindings(long appInstanceId) {
        return appApiCollectionRepository.findAllByAppInstanceIdOrderByApiCollectionId(appInstanceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBound(long appInstanceId, long apiCollectionId) {
        return appApiCollectionRepository.findByAppInstanceIdAndApiCollectionId(appInstanceId, apiCollectionId)
            .isPresent();
    }

    @Override
    public boolean unbind(long appInstanceId, long apiCollectionId) {
        Optional<AppApiCollection> binding = appApiCollectionRepository.findByAppInstanceIdAndApiCollectionId(
            appInstanceId, apiCollectionId);

        binding.ifPresent(appApiCollectionRepository::delete);

        return binding.isPresent();
    }
}
