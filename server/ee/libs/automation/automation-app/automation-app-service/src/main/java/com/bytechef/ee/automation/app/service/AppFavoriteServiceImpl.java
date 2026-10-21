/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppFavorite;
import com.bytechef.ee.automation.app.repository.AppFavoriteRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
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
public class AppFavoriteServiceImpl implements AppFavoriteService {

    private final AppFavoriteRepository appFavoriteRepository;

    @SuppressFBWarnings("EI")
    public AppFavoriteServiceImpl(AppFavoriteRepository appFavoriteRepository) {
        this.appFavoriteRepository = appFavoriteRepository;
    }

    @Override
    public void addFavorite(long userId, long appId) {
        appFavoriteRepository.insertIfAbsent(userId, appId, Instant.now());
    }

    @Override
    public void deleteFavorites(long userId, Collection<Long> appIds) {
        if (appIds.isEmpty()) {
            return;
        }

        appFavoriteRepository.deleteAllByUserIdAndAppIdIn(userId, appIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppFavorite> getFavorites(long userId, Collection<Long> appIds) {
        if (appIds.isEmpty()) {
            return List.of();
        }

        return appFavoriteRepository.findAllByUserIdAndAppIdIn(userId, appIds);
    }

    @Override
    public void removeFavorite(long userId, long appId) {
        appFavoriteRepository.delete(userId, appId);
    }
}
