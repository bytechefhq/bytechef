/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import com.bytechef.ee.embedded.configuration.domain.AppEvent;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppEventService {

    AppEvent create(AppEvent appEvent);

    void delete(long id);

    AppEvent getAppEvent(long id);

    List<AppEvent> getAppEvents();

    AppEvent update(AppEvent appEvent);
}
