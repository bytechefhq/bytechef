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

package com.bytechef.platform.notification.delivery;

import java.util.Locale;
import java.util.Set;

/**
 * Header names an admin-defined webhook header can never set, because the delivery client owns them.
 *
 * @author Ivica Cardic
 */
public final class WebhookReservedHeaders {

    private static final String RESERVED_PREFIX = "x-bytechef-";
    private static final Set<String> RESERVED_NAMES = Set.of(
        "content-type", "content-length", "host", "transfer-encoding", "connection");

    private WebhookReservedHeaders() {
    }

    public static boolean isReserved(String headerName) {
        String lowerCaseName = headerName.toLowerCase(Locale.ROOT);

        return lowerCaseName.startsWith(RESERVED_PREFIX) || RESERVED_NAMES.contains(lowerCaseName);
    }
}
