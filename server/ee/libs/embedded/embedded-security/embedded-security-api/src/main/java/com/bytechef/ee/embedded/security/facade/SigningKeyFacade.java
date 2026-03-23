/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.facade;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.platform.constant.PlatformType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface SigningKeyFacade {

    String create(SigningKey signingKey, PlatformType type);
}
