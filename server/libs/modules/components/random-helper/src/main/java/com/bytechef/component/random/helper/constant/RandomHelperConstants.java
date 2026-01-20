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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Random;

/**
 * Constants for the Random Helper component.
 *
 * <p>
 * <b>Security Note:</b> The use of {@link java.util.Random} is intentional for this component. The Random Helper
 * component is designed to generate random data for workflow automation purposes, such as generating test data,
 * randomizing workflow behavior, or creating non-sensitive random identifiers. The PREDICTABLE_RANDOM suppression is
 * appropriate because:
 *
 * <ul>
 * <li>This component is explicitly for non-cryptographic random generation</li>
 * <li>Workflow creators understand they are using a "random helper" not a "secure random helper"</li>
 * <li>For security-sensitive operations (tokens, keys, passwords), users should use appropriate cryptographic
 * components</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PREDICTABLE_RANDOM")
public class RandomHelperConstants {

    public static final String ALPHANUMERIC_CHARACTERS =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static final String CHARACTER_SET = "characterSet";
    public static final String END_INCLUSIVE = "endInclusive";
    public static final String LENGTH = "length";
    public static final String START_INCLUSIVE = "startInclusive";
    public static final String SYMBOL_CHARACTERS = "~`!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?";

    /**
     * Shared Random instance for generating non-cryptographic random values.
     */
    public static final Random RANDOM = new Random();
}
