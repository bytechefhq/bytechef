/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class UserRequestForwardingToolCallbackTest {

    private static final String USER_MESSAGE = "Block messages that contain personal data.";

    private final ToolCallback delegate = mock(ToolCallback.class);
    private final UserRequestForwardingToolCallback toolCallback = new UserRequestForwardingToolCallback(delegate);

    @Test
    void testCallAppendsTheUserMessageToTheRequest() {
        String request =
            forwardedRequest("{\"request\":\"Workflow w-1: add a PII script\"}", userContext(USER_MESSAGE));

        assertThat(request).isEqualTo(
            "Workflow w-1: add a PII script" + UserRequestForwardingToolCallback.USER_MESSAGE_HEADER + USER_MESSAGE);
    }

    @Test
    void testCallLeavesARequestThatAlreadyQuotesTheUserMessage() {
        String toolInput = "{\"request\":\"Workflow w-1: " + USER_MESSAGE + "\"}";

        ToolContext toolContext = userContext(USER_MESSAGE);

        when(delegate.call(anyString(), any())).thenReturn("ok");

        toolCallback.call(toolInput, toolContext);

        verify(delegate).call(toolInput, toolContext);
    }

    @Test
    void testCallPassesTheInputThroughWithoutAUserMessage() {
        String toolInput = "{\"request\":\"Workflow w-1: build it\"}";
        ToolContext toolContext = new ToolContext(Map.of());

        when(delegate.call(anyString(), any())).thenReturn("ok");

        toolCallback.call(toolInput, toolContext);

        verify(delegate).call(toolInput, toolContext);
    }

    @Test
    void testCallPassesMalformedInputThroughToTheDelegate() {
        ToolContext toolContext = userContext(USER_MESSAGE);

        when(delegate.call(anyString(), any())).thenReturn("error");

        toolCallback.call("not json", toolContext);

        verify(delegate).call("not json", toolContext);
    }

    private String forwardedRequest(String toolInput, ToolContext toolContext) {
        ArgumentCaptor<String> inputCaptor = ArgumentCaptor.forClass(String.class);

        when(delegate.call(anyString(), any())).thenReturn("ok");

        toolCallback.call(toolInput, toolContext);

        verify(delegate).call(inputCaptor.capture(), any());

        return (String) JsonUtils.readMap(inputCaptor.getValue())
            .get("request");
    }

    private static ToolContext userContext(String userMessage) {
        return new ToolContext(Map.of(AiHubToolInvocationContext.TOOL_CONTEXT_LAST_USER_PROMPT_KEY, userMessage));
    }
}
