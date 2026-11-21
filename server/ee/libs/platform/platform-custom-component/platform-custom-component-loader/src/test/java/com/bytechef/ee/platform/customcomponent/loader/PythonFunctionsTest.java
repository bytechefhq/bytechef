/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PythonFunctionsTest {

    @Test
    void testPlainFunctionAcceptsItsDeclaredArguments() {
        assertEquals(2, getAcceptedArgumentCount("python", "def perform(input, connection):\n    pass\nperform", 3));
    }

    @Test
    void testFunctionAcceptsNoMoreThanItIsGiven() {
        assertEquals(1,
            getAcceptedArgumentCount("python", "def perform(input, connection, context):\n    pass\nperform", 1));
    }

    @Test
    void testVarargsFunctionAcceptsEveryArgument() {
        assertEquals(3, getAcceptedArgumentCount("python", "def perform(*arguments):\n    pass\nperform", 3));
    }

    @Test
    void testBoundMethodDoesNotCountSelf() {
        String script = """
            class Action:
                def perform(self, input):
                    pass
            Action().perform
            """;

        assertEquals(1, getAcceptedArgumentCount("python", script, 3));
    }

    @Test
    void testJavaScriptFunctionAcceptsEveryArgument() {
        assertEquals(3, getAcceptedArgumentCount("js", "(function perform(input) {})", 3));
    }

    private static int getAcceptedArgumentCount(String languageId, String script, int argumentCount) {
        return PolyglotSandbox.call(languageId, polyglotContext -> {
            Value function = polyglotContext.eval(languageId, script);

            return PythonFunctions.getAcceptedArgumentCount(function, argumentCount);
        });
    }
}
