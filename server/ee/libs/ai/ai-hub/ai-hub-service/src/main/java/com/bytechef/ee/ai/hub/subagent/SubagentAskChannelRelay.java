/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelay;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.function.Supplier;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Binds the CE {@link SubAgentAskRelay} seam to the EE {@link SubagentAskChannel}, so the CE
 * {@code IntelligentToolCatalog} can make every intelligent delegate ask-capable without depending on EE.
 *
 * <p>
 * Registered as a plain {@link Component} rather than a bean of {@code AiHubConfiguration}: the capability belongs to
 * every surface the catalog serves — the Copilot panels and the management MCP server as much as the AI Hub — wherever
 * this module is on the classpath, and gating it on {@code bytechef.ai.hub.enabled} would silently switch it off for
 * the other two. A deployment that does not carry ai-hub-service has no {@code SubAgentAskRelay} bean at all, and the
 * catalog then attaches nothing — the pre-ask behaviour.
 * </p>
 *
 * <p>
 * The pending question is read <b>inside</b> the channel binding and returned alongside the delegation's result. This
 * instance is a singleton shared by every concurrent delegation, so it deliberately holds no state of its own —
 * stashing the question on the instance so it could be read after the binding unwound would let two conversations race
 * and surface one user's question in another's chat.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class SubagentAskChannelRelay implements SubAgentAskRelay {

    @Override
    public ToolCallback askUserQuestionToolCallback() {
        return new SubagentAskUserQuestionToolCallback();
    }

    @Override
    public <T> AskOutcome<T> runWithChannel(Supplier<T> supplier) {
        return SubagentAskChannel.runWithChannel(() -> {
            T result = supplier.get();

            return new AskOutcome<>(result, SubagentAskChannel.pending());
        });
    }
}
