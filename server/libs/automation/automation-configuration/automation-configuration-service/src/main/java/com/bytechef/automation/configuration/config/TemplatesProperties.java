/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.configuration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "templates")
public class TemplatesProperties {

    private String owner;
    private String ref;
    private String repo;

    public String getOwner() {
        return owner;
    }

    public String getRef() {
        return ref;
    }

    public String getRepo() {
        return repo;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }
}
