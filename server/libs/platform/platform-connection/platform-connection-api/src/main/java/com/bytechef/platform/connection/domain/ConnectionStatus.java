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

package com.bytechef.platform.connection.domain;

import java.util.Set;

/**
 * @author Ivica Cardic
 */
public enum ConnectionStatus {

    ACTIVE(0),
    PENDING_REASSIGNMENT(1),
    REVOKED(2);

    private static final Set<ConnectionStatus> ACTIVE_TRANSITIONS = Set.of(PENDING_REASSIGNMENT, REVOKED);
    private static final Set<ConnectionStatus> PENDING_REASSIGNMENT_TRANSITIONS = Set.of(ACTIVE, REVOKED);
    private static final Set<ConnectionStatus> REVOKED_TRANSITIONS = Set.of();

    private final int code;

    ConnectionStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ConnectionStatus fromCode(int code) {
        for (ConnectionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown ConnectionStatus code: " + code);
    }

    public boolean canTransitionTo(ConnectionStatus target) {
        return switch (this) {
            case ACTIVE -> ACTIVE_TRANSITIONS.contains(target);
            case PENDING_REASSIGNMENT -> PENDING_REASSIGNMENT_TRANSITIONS.contains(target);
            case REVOKED -> REVOKED_TRANSITIONS.contains(target);
        };
    }
}
