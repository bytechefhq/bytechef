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

package com.bytechef.platform.security.web.authentication;

import com.bytechef.platform.constant.AppType;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public class ApiKeyAuthenticationToken extends AbstractPublicApiAuthenticationToken {

    private String secretKey;

    public ApiKeyAuthenticationToken(String secretKey, Environment environment, String tenantId, AppType type) {
        super(environment, tenantId, type);

        this.secretKey = secretKey;
    }

    @SuppressFBWarnings("EI")
    public ApiKeyAuthenticationToken(User user) {
        super(user);
    }

    public String getSecretKey() {
        return secretKey;
    }
}
