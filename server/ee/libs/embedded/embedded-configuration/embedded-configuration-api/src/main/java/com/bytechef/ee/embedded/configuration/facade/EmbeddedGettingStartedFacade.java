/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.dto.EmbeddedGettingStartedDTO;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface EmbeddedGettingStartedFacade {

    void dismissEmbeddedGettingStarted();

    EmbeddedGettingStartedDTO getEmbeddedGettingStarted();

    boolean isEmbeddedGettingStartedCompleted();
}
