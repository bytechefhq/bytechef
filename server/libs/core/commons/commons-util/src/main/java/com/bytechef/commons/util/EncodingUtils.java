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

package com.bytechef.commons.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author Ivica Cardic
 */
public class EncodingUtils {

    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder();

    public static byte[] base64Decode(final byte[] bytes) {
        return DECODER.decode(bytes);
    }

    public static byte[] base64Decode(final String data) {
        return DECODER.decode(data);
    }

    public static String base64DecodeToString(final String data) {
        return new String(base64Decode(data), StandardCharsets.UTF_8);
    }

    public static String base64EncodeToString(final String... data) {
        var sb = new StringBuilder();

        for (String value : data) {
            sb.append(value);
        }

        return ENCODER.encodeToString(sb.toString()
            .getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] base64Encode(final byte[] bytes) {
        return ENCODER.encode(bytes);
    }

    public static byte[] base64Encode(final String data) {
        return ENCODER.encode(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64EncodeToString(final byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }

    public static String base64EncodeToString(final String data) {
        return ENCODER.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String urlEncode(String string) {
        return URLEncoder.encode(string, StandardCharsets.UTF_8);
    }

    public static String urlEncodeBase64ToString(byte[] token) {
        return URL_ENCODER.withoutPadding()
            .encodeToString(token);
    }

    public static String urlEncodeBase64ToString(String string) {
        return URL_ENCODER.withoutPadding()
            .encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] urlDecodeBase64FromString(String string) {
        return URL_DECODER.decode(string);
    }
}
