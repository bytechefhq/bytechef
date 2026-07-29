/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import java.util.List;

/**
 * Per-controller admin facade for the app-event read operations. {@code AppEventService.getAppEvent(s)} is shared with
 * the app-event component trigger (which runs during workflow execution, with no admin identity), so it cannot carry an
 * admin gate; this facade wraps the reads behind {@code isTenantAdmin()} for the management REST controller, keeping
 * the gate on the facade rather than the controller. The write operations stay gated on {@code AppEventService} itself
 * (single admin caller).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppEventFacade {

    AppEvent getAppEvent(long id);

    List<AppEvent> getAppEvents();
}
