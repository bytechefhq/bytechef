/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.impl;

import com.bytechef.commons.utils.MapUtils;
import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.connection.domain.Connection;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ConnectionParametersImpl implements ConnectionParameters {
    private final String authorizationName;
    private final String name;
    private final Map<String, Object> parameters;

    public ConnectionParametersImpl(Connection connection) {
        this.authorizationName = connection.getAuthorizationName();
        this.name = connection.getName();
        this.parameters = connection.getParameters();
    }

    @Override
    public String getAuthorizationName() {
        return authorizationName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String name) {
        return (T) parameters.get(name);
    }

    @Override
    public String getParameter(String name, String defaultValue) {
        return MapUtils.getString(parameters, name, defaultValue);
    }
}
