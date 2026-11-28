/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import com.bytechef.ee.ai.hub.util.Mode;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;

/**
 * Source of each agent mode's PINNED tool callbacks, the always-on tools registered on the agent's builder. The
 * approval facade re-finds an approved call's tool here by name, so an implementation must return the same tools the
 * agent of that mode was built with, unwrapped (neither gated nor rehydrated): the facade applies its own ungated wrap
 * before executing.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface AiHubPinnedToolCallbacks {

    List<ToolCallback> pinnedToolCallbacks(Mode mode);
}
