/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.automation.ai.tool.ToolArtifactRecorder;

/**
 * AI-Hub-side artifact recorder. Inherits the full record contract from the CE {@link ToolArtifactRecorder} SPI (the
 * relocated asset-file tool callbacks depend on the CE type, so both AI-Hub and non-AI-Hub tools share one interface).
 * Implementations are provided by the service layer and injected into the callbacks at configuration time; they persist
 * into the AI Hub chat's artifacts sidebar (keyed by the AG-UI {@code threadId}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatArtifactRecorder extends ToolArtifactRecorder {
}
