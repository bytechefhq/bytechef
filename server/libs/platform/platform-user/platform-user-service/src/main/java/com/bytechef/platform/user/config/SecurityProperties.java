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

package com.bytechef.platform.user.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
@ConfigurationProperties(prefix = "bytechef.security", ignoreUnknownFields = false)
public class SecurityProperties {

    private String contentSecurityPolicy;
    private RememberMe rememberMe = new RememberMe();

    public String getContentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    public RememberMe getRememberMe() {
        return rememberMe;
    }

    public void setContentSecurityPolicy(String contentSecurityPolicy) {
        this.contentSecurityPolicy = contentSecurityPolicy;
    }

    public void setRememberMe(RememberMe rememberMe) {
        this.rememberMe = rememberMe;
    }

    public static class RememberMe {

        private String key;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
