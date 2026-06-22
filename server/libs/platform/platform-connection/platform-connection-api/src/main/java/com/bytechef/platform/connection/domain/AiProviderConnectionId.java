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

/**
 * Encodes the synthetic identity of an AI-provider-backed virtual connection as a single negative {@code long}. Real
 * connection ids are positive (autoincrement from 1050); a negative id therefore never collides with the
 * {@code connection} table and is reversible to its {@code (provider, environment)}.
 *
 * @author Ivica Cardic
 */
public final class AiProviderConnectionId {

    private static final int ENVIRONMENT_RADIX = 100;

    private AiProviderConnectionId() {
    }

    public static long encode(int providerId, int environmentId) {
        return -((long) providerId * ENVIRONMENT_RADIX + environmentId);
    }

    public static boolean isAiProviderConnectionId(long id) {
        return id < 0;
    }

    public static int providerId(long id) {
        return (int) (-id / ENVIRONMENT_RADIX);
    }

    public static int environmentId(long id) {
        return (int) (-id % ENVIRONMENT_RADIX);
    }
}
