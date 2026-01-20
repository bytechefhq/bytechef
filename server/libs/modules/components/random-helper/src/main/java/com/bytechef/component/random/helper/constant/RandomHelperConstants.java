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

package com.bytechef.component.random.helper.constant;

import java.util.Random;

/**
 * @author Ivica Cardic
 */
public class RandomHelperConstants {

    public static final String ALPHANUMERIC_CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static final String CHARACTER_SET = "characterSet";
    public static final String END_INCLUSIVE = "endInclusive";
    public static final String LENGTH = "length";
    public static final String START_INCLUSIVE = "startInclusive";
    public static final String SYMBOL_CHARACTERS = "~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";

    public static final Random RANDOM = new Random();
}
