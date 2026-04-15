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
 * <li>{@code CONNECTION_ALREADY_AT_TARGET_VISIBILITY} — benign concurrent race: the row was promoted by another admin
 * between a bulk operation's pre-filter read and the per-row call. Bulk promote classifies this as {@code skipped}, NOT
 * {@code failed}, so "N promoted, M skipped" reads as a success. Do not broaden the catch to include other error keys —
 * doing so hides real authorization failures as benign skips.</li>
 * <li>{@code CONNECTION_IS_USED} — a destructive operation was attempted while the connection is still wired to an
 * active deployment workflow or test configuration. The caller must disconnect or unshare first.</li>
 * <li>{@code CONNECTION_NOT_ACTIVE} — the row's {@code ConnectionStatus} is not ACTIVE (PENDING_REASSIGNMENT or
 * REVOKED). Emitted by operations that refuse to run against non-active credentials.</li>
 * <li>{@code INVALID_CONNECTION} — either the id does not exist in this workspace, or the caller lacks authorization.
 * Deliberately ambiguous to avoid a connection-id enumeration oracle for non-admin callers.</li>
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
