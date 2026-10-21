/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppVersion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppVersionService {

    AppVersion create(AppVersion appVersion);

    Optional<AppVersion> fetchAppVersion(long appId, String name);

    AppVersion getAppVersion(long id);

    List<AppVersion> getAppVersions(long appId, int limit);

    List<AppVersion> getAppVersionsByIds(Collection<Long> ids);
}
