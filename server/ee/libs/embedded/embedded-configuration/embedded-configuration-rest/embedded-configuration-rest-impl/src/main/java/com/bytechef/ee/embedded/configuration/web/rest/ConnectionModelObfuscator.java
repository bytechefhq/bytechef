/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.ObfuscateUtils;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectionModel;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ConnectionModelObfuscator {

    private ConnectionModelObfuscator() {
    }

    static ConnectionModel obfuscate(ConnectionModel connectionModel) {
        Map<String, Object> authorizationParameters = connectionModel.getAuthorizationParameters();

        if (authorizationParameters != null) {
            connectionModel.authorizationParameters(
                MapUtils.toMap(
                    authorizationParameters,
                    Map.Entry::getKey,
                    entry -> ObfuscateUtils.obfuscate(String.valueOf(entry.getValue()), 28, 8)));
        }

        return connectionModel.parameters(null);
    }
}
