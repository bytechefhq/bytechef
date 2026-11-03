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

package com.bytechef.platform.notification.facade;

import com.bytechef.commons.util.EncodingUtils;
import java.security.SecureRandom;

/**
 * @author Ivica Cardic
 */
final class WebhookSigningSecrets {

    private static final String PREFIX = "whsec_";
    private static final int SECRET_BYTE_COUNT = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private WebhookSigningSecrets() {
    }

    static String generate() {
        byte[] bytes = new byte[SECRET_BYTE_COUNT];

        SECURE_RANDOM.nextBytes(bytes);

        return PREFIX + EncodingUtils.urlEncodeBase64ToString(bytes);
    }
}
