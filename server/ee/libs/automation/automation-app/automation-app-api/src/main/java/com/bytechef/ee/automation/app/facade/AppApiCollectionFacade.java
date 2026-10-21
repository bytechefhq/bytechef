/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import com.bytechef.ee.automation.app.dto.AppApiCollectionBindingDTO;
import com.bytechef.ee.automation.app.dto.AppBindableApiCollectionDTO;
import java.util.List;

/**
 * The gated admin surface for the API collections an app serves.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppApiCollectionFacade {

    List<AppApiCollectionBindingDTO> getApiCollectionBindings(long appId);

    /**
     * The API collections of the app's workspace and environment the app can be bound to, limited to projects the
     * caller can see.
     */
    List<AppBindableApiCollectionDTO> getBindableApiCollections(long appId);

    AppApiCollectionBindingDTO bindApiCollection(long appId, long apiCollectionId);

    void unbindApiCollection(long appId, long apiCollectionId);
}
