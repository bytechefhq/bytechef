
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

package com.bytechef.hermes.component.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ValueUtils {

    private static final Logger logger = LoggerFactory.getLogger(ValueUtils.class);

    public static Boolean booleanOf(String string) {
        return toBooleanObject(string);
    }

    public static Double doubleOf(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Integer intOf(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Long longOf(String string) {
        try {
            return Long.parseLong(string);
        } catch (NumberFormatException nfe) {
            if (logger.isTraceEnabled()) {
                logger.trace(nfe.getMessage(), nfe);
            }
        }

        return null;
    }

    public static Object valueOF(String string) {
        Object value = intOf(string);

        if (value == null) {
            value = longOf(string);
        }

        if (value == null) {
            value = doubleOf(string);
        }

        if (value == null) {
            value = booleanOf(string);
        }

        if (value == null) {
            value = string;
        }

        return value;
    }

    @SuppressFBWarnings("NP")
    private static Boolean toBooleanObject(final String str) {
        if (str == null) {
            return null;
        }

        if (str.equals("true")) {
            return Boolean.TRUE;
        }

        switch (str.length()) {
            case 1 -> {
                final char ch0 = str.charAt(0);

                if (ch0 == 'y' || ch0 == 'Y' || ch0 == 't' || ch0 == 'T' || ch0 == '1') {
                    return Boolean.TRUE;
                }
                if (ch0 == 'n' || ch0 == 'N' || ch0 == 'f' || ch0 == 'F' || ch0 == '0') {
                    return Boolean.FALSE;
                }
            }
            case 2 -> {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);

                if ((ch0 == 'o' || ch0 == 'O') && (ch1 == 'n' || ch1 == 'N')) {
                    return Boolean.TRUE;
                }

                if ((ch0 == 'n' || ch0 == 'N') && (ch1 == 'o' || ch1 == 'O')) {
                    return Boolean.FALSE;
                }
            }
            case 3 -> {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char ch2 = str.charAt(2);

                if ((ch0 == 'y' || ch0 == 'Y') && (ch1 == 'e' || ch1 == 'E') && (ch2 == 's' || ch2 == 'S')) {
                    return Boolean.TRUE;
                }

                if ((ch0 == 'o' || ch0 == 'O') && (ch1 == 'f' || ch1 == 'F') && (ch2 == 'f' || ch2 == 'F')) {
                    return Boolean.FALSE;
                }
            }
            case 4 -> {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char ch2 = str.charAt(2);
                final char ch3 = str.charAt(3);

                if ((ch0 == 't' || ch0 == 'T') && (ch1 == 'r' || ch1 == 'R') && (ch2 == 'u' || ch2 == 'U') &&
                    (ch3 == 'e' || ch3 == 'E')) {

                    return Boolean.TRUE;
                }
            }
            case 5 -> {
                final char ch0 = str.charAt(0);
                final char ch1 = str.charAt(1);
                final char ch2 = str.charAt(2);
                final char ch3 = str.charAt(3);
                final char ch4 = str.charAt(4);

                if ((ch0 == 'f' || ch0 == 'F') && (ch1 == 'a' || ch1 == 'A') && (ch2 == 'l' || ch2 == 'L') &&
                    (ch3 == 's' || ch3 == 'S') && (ch4 == 'e' || ch4 == 'E')) {

                    return Boolean.FALSE;
                }
            }
            default -> {
            }
        }

        return null;
    }
}
