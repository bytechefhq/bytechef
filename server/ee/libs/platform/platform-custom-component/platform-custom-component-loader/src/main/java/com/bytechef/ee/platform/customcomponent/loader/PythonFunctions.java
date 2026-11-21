/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import org.graalvm.polyglot.Value;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class PythonFunctions {

    private static final int VARARGS_FLAG = 0x04;

    private PythonFunctions() {
    }

    static int getAcceptedArgumentCount(Value function, int argumentCount) {
        if (!function.hasMember("__code__")) {
            return argumentCount;
        }

        Value code = function.getMember("__code__");

        Value flags = code.getMember("co_flags");

        if ((flags.asInt() & VARARGS_FLAG) != 0) {
            return argumentCount;
        }

        Value declaredArgumentCountValue = code.getMember("co_argcount");

        int declaredArgumentCount = declaredArgumentCountValue.asInt();

        if (function.hasMember("__func__") && function.hasMember("__self__")) {
            declaredArgumentCount--;
        }

        return Math.min(argumentCount, Math.max(declaredArgumentCount, 0));
    }
}
