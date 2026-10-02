/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextConsumer;
import com.bytechef.component.definition.Context.Log;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class HostContextBridgeTest {

    @Test
    void testLogAcceptsLowerCaseLevels() {
        Log log = mock(Log.class);

        HostContextBridge hostContextBridge = new HostContextBridge(actionContext(log));

        hostContextBridge.log("warn", "low disk", null);
        hostContextBridge.log("error", "failed", "boom");
        hostContextBridge.log("Debug", "details", null);

        verify(log).warn("low disk");
        verify(log).error("failed: boom");
        verify(log).debug("details");
        verify(log, never()).info(any(String.class));
    }

    @Test
    void testLogFallsBackToInfoForUnknownOrMissingLevels() {
        Log log = mock(Log.class);

        HostContextBridge hostContextBridge = new HostContextBridge(actionContext(log));

        hostContextBridge.log("verbose", "first", null);
        hostContextBridge.log(null, "second", null);

        verify(log).info("first");
        verify(log).info("second");
    }

    @SuppressWarnings("unchecked")
    private static ActionContext actionContext(Log log) {
        ActionContext actionContext = mock(ActionContext.class);

        doAnswer(invocation -> {
            ContextConsumer<Log> logConsumer = invocation.getArgument(0);

            logConsumer.accept(log);

            return null;
        }).when(actionContext)
            .log(any(ContextConsumer.class));

        return actionContext;
    }
}
