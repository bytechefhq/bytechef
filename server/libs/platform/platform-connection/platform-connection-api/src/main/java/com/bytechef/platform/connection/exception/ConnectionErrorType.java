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

package com.bytechef.platform.connection.exception;

import com.bytechef.exception.AbstractErrorType;
import com.bytechef.platform.connection.domain.Connection;

/**
 * Domain error codes for the connection module. Keys are numeric and MUST remain stable — downstream consumers
 * (clients, exception resolvers, test assertions) key off {@link #getErrorKey()}.
 *
 * <p>
 * Classification guidance:
 * <ul>
 * <li>{@code CONNECTION_ALREADY_AT_TARGET_VISIBILITY} — reserved; no current operation raises it.</li>
 * <li>{@code CONNECTION_IS_USED} — the connection is still referenced by a deployment workflow (and, for delete, a test
 * configuration). The caller must disconnect it first.</li>
 * <li>{@code CONNECTION_NOT_ACTIVE} — the row's {@code ConnectionStatus} is not ACTIVE (PENDING_REASSIGNMENT or
 * REVOKED). Emitted by operations that refuse to run against non-active credentials.</li>
 * <li>{@code INVALID_CONNECTION} — the request names a connection, grantee, owner, visibility or status transition that
 * is not valid for it. Authorization failures are not reported here; they raise {@code AccessDeniedException}.</li>
 * <li>{@code INVALID_CONNECTION_COMPONENT_NAME} — componentName does not match a known component definition.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public class ConnectionErrorType extends AbstractErrorType {

    public static final ConnectionErrorType CONNECTION_ALREADY_AT_TARGET_VISIBILITY = new ConnectionErrorType(104);
    public static final ConnectionErrorType CONNECTION_IS_USED = new ConnectionErrorType(100);
    public static final ConnectionErrorType CONNECTION_NOT_ACTIVE = new ConnectionErrorType(103);
    public static final ConnectionErrorType INVALID_CONNECTION = new ConnectionErrorType(101);
    public static final ConnectionErrorType INVALID_CONNECTION_COMPONENT_NAME = new ConnectionErrorType(102);

    private ConnectionErrorType(int errorKey) {
        super(Connection.class, errorKey);
    }
}
