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

package com.bytechef.platform.component.polyglot;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author Ivica Cardic
 */
public final class GuestFunctions {

    private static final int PYTHON_VARARGS_FLAG = 0x04;

    private GuestFunctions() {
    }

    public static int getAcceptedArgumentCount(Value function, int argumentCount) {
        if (!function.hasMember("__code__")) {
            return argumentCount;
        }

        Value code = function.getMember("__code__");

        Value flags = code.getMember("co_flags");

        if ((flags.asInt() & PYTHON_VARARGS_FLAG) != 0) {
            return argumentCount;
        }

        Value declaredArgumentCountValue = code.getMember("co_argcount");

        int declaredArgumentCount = declaredArgumentCountValue.asInt();

        if (function.hasMember("__func__") && function.hasMember("__self__")) {
            declaredArgumentCount--;
        }

        return Math.min(argumentCount, Math.max(declaredArgumentCount, 0));
    }

    public static int getRequiredParameterCount(
        Context polyglotContext, String languageId, Value function, int defaultParameterCount) {

        String parameterCountFunctionSource = switch (languageId) {
            case "js" -> "(guestFunction) => guestFunction.length";
            case "python" -> "lambda guest_function: guest_function.__code__.co_argcount - " +
                "len(guest_function.__defaults__ or ())";
            default -> null;
        };

        if (parameterCountFunctionSource == null) {
            return defaultParameterCount;
        }

        Value parameterCountFunction = polyglotContext.eval(languageId, parameterCountFunctionSource);

        Value parameterCount = parameterCountFunction.execute(function);

        return parameterCount.fitsInInt() ? parameterCount.asInt() : defaultParameterCount;
    }
}
