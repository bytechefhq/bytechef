/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.dto.AppIconDTO;
import com.bytechef.ee.automation.app.dto.PortalAppDTO;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;

/**
 * The App Portal: the apps the current user may launch, across every workspace they belong to.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppPortalFacade {

    Optional<AppIconDTO> getPortalAppIcon(long appId);

    List<PortalAppDTO> getPortalApps(Environment environment);

    void recordAppLaunch(long appInstanceId);

    void setAppFavorite(long appId, boolean favorite);
}
