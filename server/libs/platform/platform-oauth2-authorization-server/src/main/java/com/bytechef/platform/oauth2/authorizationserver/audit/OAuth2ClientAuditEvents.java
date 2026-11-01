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

package com.bytechef.platform.oauth2.authorizationserver.audit;

/**
 * @author Ivica Cardic
 */
public final class OAuth2ClientAuditEvents {

    public static final String OAUTH2_CLIENT_ALL_GRANTS_REVOKED = "OAUTH2_CLIENT_ALL_GRANTS_REVOKED";
    public static final String OAUTH2_CLIENT_DELETED = "OAUTH2_CLIENT_DELETED";
    public static final String OAUTH2_CLIENT_GRANT_REVOKED = "OAUTH2_CLIENT_GRANT_REVOKED";

    private OAuth2ClientAuditEvents() {
    }
}
