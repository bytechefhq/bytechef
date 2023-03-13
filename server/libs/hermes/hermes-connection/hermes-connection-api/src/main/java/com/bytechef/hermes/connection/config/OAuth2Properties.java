
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.hermes.connection.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Arik Cohen
 */
@Component
@ConfigurationProperties(prefix = "bytechef.oauth2")
@SuppressFBWarnings("EI")
public class OAuth2Properties {

    private List<Map<String, OAuth2App>> apps = new ArrayList<>();

    public List<Map<String, OAuth2App>> getApps() {
        return apps;
    }

    public void setApps(List<Map<String, OAuth2App>> apps) {
        this.apps = apps;
    }

    public record OAuth2App(String clientId, String clientSecret) {
    }
}
