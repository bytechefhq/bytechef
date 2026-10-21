/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppInstanceUsage;
import com.bytechef.ee.automation.app.repository.AppInstanceUsageRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AppInstanceUsageServiceImpl implements AppInstanceUsageService {

    static final Duration TOUCH_THROTTLE = Duration.ofMinutes(15);

    private final AppInstanceUsageRepository appInstanceUsageRepository;

    @SuppressFBWarnings("EI")
    public AppInstanceUsageServiceImpl(AppInstanceUsageRepository appInstanceUsageRepository) {
        this.appInstanceUsageRepository = appInstanceUsageRepository;
    }

    @Override
    public void deleteUsages(long userId, Collection<Long> appInstanceIds) {
        if (appInstanceIds.isEmpty()) {
            return;
        }

        appInstanceUsageRepository.deleteAllByUserIdAndAppInstanceIdIn(userId, appInstanceIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppInstanceUsage> getUsages(long userId, Collection<Long> appInstanceIds) {
        if (appInstanceIds.isEmpty()) {
            return List.of();
        }

        return appInstanceUsageRepository.findAllByUserIdAndAppInstanceIdIn(userId, appInstanceIds);
    }

    @Override
    public void recordLaunch(long userId, long appInstanceId) {
        appInstanceUsageRepository.recordLaunch(userId, appInstanceId, Instant.now());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void touchLastUsedDate(long userId, long appInstanceId) {
        Instant now = Instant.now();

        appInstanceUsageRepository.touchLastUsedDate(userId, appInstanceId, now, now.minus(TOUCH_THROTTLE));
    }
}
