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

package com.bytechef.commons.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author Ivica Cardic
 */
public class EncodingUtils {

    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder();

    public static byte[] decodeBase64(final byte[] bytes) {
        return DECODER.decode(bytes);
    }

    public static byte[] decodeBase64(final String data) {
        return DECODER.decode(data);
    }

    public static String decodeBase64ToString(final String data) {
        return new String(decodeBase64(data), StandardCharsets.UTF_8);
    }

    public static byte[] encodeBase64(final byte[] bytes) {
        return ENCODER.encode(bytes);
    }

    public static byte[] encodeBase64(final String data) {
        return ENCODER.encode(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeBase64ToString(final byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }

    public static String encodeBase64ToString(final String data) {
        return ENCODER.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeToString(byte[] token) {
        return URL_ENCODER.withoutPadding()
            .encodeToString(token);
    }
}
