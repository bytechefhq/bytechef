/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.remote.client.facade;

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.facade.ConnectionDefinitionFacade;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionDefinitionFacadeClient implements ConnectionDefinitionFacade {

    @Override
    public ComponentConnection executeConnectionRefresh(Long connectionId) {
        throw new UnsupportedOperationException();
    }
}
