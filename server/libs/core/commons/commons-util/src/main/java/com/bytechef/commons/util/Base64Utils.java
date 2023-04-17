
/*
 * Copyright 2021 <your company/name>.
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
public class Base64Utils {

    private static final Base64.Decoder DECODER = Base64.getDecoder();
    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public static byte[] decode(String src) {
        return DECODER.decode(src);
    }

    public static String decodeToString(String src) {
        return new String(decode(src), StandardCharsets.UTF_8);
    }

    public static byte[] encode(String src) {
        return ENCODER.encode(src.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeToString(String src) {
        return ENCODER.encodeToString(src.getBytes(StandardCharsets.UTF_8));
    }
}
