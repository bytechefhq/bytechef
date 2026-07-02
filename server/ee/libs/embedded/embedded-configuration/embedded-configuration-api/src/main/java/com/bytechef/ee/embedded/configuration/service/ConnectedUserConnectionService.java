/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.service;

import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ConnectedUserConnectionService {

    void create(long connectedUserId, long connectionId);

    List<Long> getConnectionIds(long connectedUserId);
}
