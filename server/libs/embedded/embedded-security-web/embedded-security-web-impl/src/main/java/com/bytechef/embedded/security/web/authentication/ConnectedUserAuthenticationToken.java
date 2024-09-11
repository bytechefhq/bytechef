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

package com.bytechef.embedded.security.web.authentication;

import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.security.web.authentication.AbstractPublicApiAuthenticationToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.security.core.userdetails.User;

/**
 * @author Ivica Cardic
 */
public class ConnectedUserAuthenticationToken extends AbstractPublicApiAuthenticationToken {

    private String externalUserId;

    public ConnectedUserAuthenticationToken(
        Environment environment, int version, String externalUserId, String tenantId) {

        super(environment, version, tenantId);

        this.externalUserId = externalUserId;
    }

    @SuppressFBWarnings("EI")
    public ConnectedUserAuthenticationToken(User user) {
        super(user);
    }

    public String getExternalUserId() {
        return externalUserId;
    }
}
