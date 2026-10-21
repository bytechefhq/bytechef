/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppInstanceUsage;
import java.util.Collection;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppInstanceUsageService {

    void deleteUsages(long userId, Collection<Long> appInstanceIds);

    List<AppInstanceUsage> getUsages(long userId, Collection<Long> appInstanceIds);

    /**
     * Counts one launch and moves the last-used date to now.
     */
    void recordLaunch(long userId, long appInstanceId);

    /**
     * Moves the last-used date to now when the stored one is older than the throttle interval, without counting a
     * launch, in a transaction of its own.
     */
    void touchLastUsedDate(long userId, long appInstanceId);
}
