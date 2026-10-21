/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import com.bytechef.ee.automation.app.domain.AppInstance;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppUserAuthenticator {

    AppUser authenticate(AppInstance appInstance, String tenantId, String userToken);
}
