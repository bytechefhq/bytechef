/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;

/**
 * Visibility and named-user grants for apps and app instances.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppSharingFacade {

    List<Long> getAppGrants(long workspaceId, long appId);

    void grantAppAccess(long workspaceId, long appId, long userId);

    void revokeAppAccess(long workspaceId, long appId, long userId);

    void setAppVisibility(long workspaceId, long appId, ResourceVisibility visibility);

    List<Long> getAppInstanceGrants(long workspaceId, long appInstanceId);

    void grantAppInstanceAccess(long workspaceId, long appInstanceId, long userId);

    void revokeAppInstanceAccess(long workspaceId, long appInstanceId, long userId);

    void setAppInstanceVisibility(long workspaceId, long appInstanceId, ResourceVisibility visibility);
}
