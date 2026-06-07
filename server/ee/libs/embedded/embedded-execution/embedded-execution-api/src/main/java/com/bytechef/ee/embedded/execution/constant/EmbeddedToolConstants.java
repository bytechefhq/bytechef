/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.constant;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.HashMap;
import java.util.Map;

/**
 * Reserved tool-execution parameter keys that the embedded tool facades inject from the connected-user request context
 * so component actions can read the connected user's identity without it being an LLM-supplied input. The {@code __}
 * prefix marks these as server-injected and prevents collision with any declared tool property.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class EmbeddedToolConstants {

    public static final String ENVIRONMENT = "__environment";
    public static final String EXTERNAL_USER_ID = "__externalUserId";

    private EmbeddedToolConstants() {
    }

    public static Map<String, Object> withConnectedUserContext(
        Map<String, ?> inputParameters, String externalUserId, Environment environment) {

        Map<String, Object> parameters = new HashMap<>(inputParameters);

        parameters.put(EXTERNAL_USER_ID, externalUserId);
        parameters.put(ENVIRONMENT, environment.name());

        return parameters;
    }
}
