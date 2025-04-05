/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.configuration.remote.client.service;

import com.bytechef.platform.configuration.domain.Event;
import com.bytechef.platform.configuration.service.EventService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteEventServiceClient implements EventService {

    @Override
    public List<Event> getEvents() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Event> getEvents(List<Long> eventIds) {
        throw new UnsupportedOperationException();
    }
}
