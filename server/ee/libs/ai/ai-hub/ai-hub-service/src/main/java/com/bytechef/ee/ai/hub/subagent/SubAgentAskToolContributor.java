/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

/**
 * Registers the specialist-facing {@code askUserQuestion} tool on a delegate's request.
 *
 * <p>
 * Attached per request through the contributor seam rather than at {@code ChatClient} construction because most
 * specialist {@code ChatClient} beans are built in CE configurations ({@code McpServerSubAgentConfiguration},
 * {@code ProjectDeploymentSubAgentConfiguration}, the Copilot domain configurations) that cannot import this EE tool.
 * Request-level tools are <b>added</b> to the builder's {@code defaultTools} rather than replacing them
 * ({@code DefaultChatClient} appends), so a specialist keeps its own tool set and simply gains this one.
 * </p>
 *
 * <p>
 * Only attached for specialists whose prompt documents the tool. A registered-but-undocumented tool is not harmless:
 * tool definitions are self-describing, so a model may call it spuriously and stop mid-chat to ask something the user
 * never needed to decide.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubAgentAskToolContributor implements SubAgentAdvisorContributor {

    @Override
    public ChatClientRequestSpec contribute(
        ChatClientRequestSpec chatClientRequestSpec, @Nullable Map<String, Object> toolContext) {

        return chatClientRequestSpec.toolCallbacks(new SubagentAskUserQuestionToolCallback());
    }
}
