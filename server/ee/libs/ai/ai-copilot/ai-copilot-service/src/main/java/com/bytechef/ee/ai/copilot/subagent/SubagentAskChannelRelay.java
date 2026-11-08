/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.subagent;

import com.bytechef.ai.copilot.tool.ask.SubAgentAskRelay;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.function.Supplier;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
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
