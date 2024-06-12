/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.registry.domain;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.definition.ParameterConnection;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.lang.NonNull;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ComponentConnection(
    String componentName, int version, long connectionId, @NonNull Map<String, ?> parameters,
    @NonNull String authorizationName)
    implements ParameterConnection {

    public ComponentConnection {
        authorizationName = authorizationName.trim();

        if (authorizationName.isEmpty()) {
            throw new IllegalArgumentException("Authorization name must not be empty or blank");
        }
    }

    @Override
    public String getComponentName() {
        return componentName;
    }

    @Override
    public long getConnectionId() {
        return connectionId;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public Map<String, ?> getParameters() {
        return parameters;
    }

    @Override
    public <T> T getParameter(String key) {
        return MapUtils.get(parameters, key, new TypeReference<>() {});
    }

    @Override
    public String getAuthorizationName() {
        return authorizationName;
    }
}
