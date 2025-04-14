/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.workflow.configuration.remote.client.facade;

import com.bytechef.platform.configuration.dto.NotificationDTO;
import com.bytechef.platform.configuration.facade.NotificationFacade;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteNotificationFacadeClient implements NotificationFacade {

    @Override
    public List<NotificationDTO> getNotifications() {
        throw new UnsupportedOperationException();
    }
}
