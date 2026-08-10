/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.subagent;

import com.bytechef.automation.ai.tool.SubAgentAskRelay;
import java.util.function.Supplier;

/**
 * Binds the CE {@link SubAgentAskRelay} seam to the EE {@link SubagentAskChannel}, so a delegate {@code ToolCallback}
 * in CE can surface a question a specialist raised without depending on EE.
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
public final class SubagentAskChannelRelay implements SubAgentAskRelay {

    @Override
    public <T> AskOutcome<T> runWithChannel(Supplier<T> supplier) {
        return SubagentAskChannel.runWithChannel(() -> {
            T result = supplier.get();

            return new AskOutcome<>(result, SubagentAskChannel.pending());
        });
    }
}
