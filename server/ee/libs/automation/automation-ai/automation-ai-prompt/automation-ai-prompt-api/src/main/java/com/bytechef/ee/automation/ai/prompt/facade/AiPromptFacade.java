/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.prompt.facade;

import com.bytechef.ee.platform.ai.prompt.AiPrompt;
import com.bytechef.ee.platform.ai.prompt.AiPromptVersion;
import java.util.List;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface AiPromptFacade {

    AiPrompt getPrompt(long id);

    List<AiPrompt> getPromptsByWorkspace(Long workspaceId);

    List<AiPromptVersion> getVersionsByPrompt(Long promptId);

    AiPrompt createInWorkspace(AiPrompt prompt, long workspaceId);

    int getNextVersionNumber(Long promptId);

    AiPromptVersion createVersion(AiPromptVersion promptVersion);

    void deleteInWorkspace(long promptId);

    void setActiveVersion(long promptVersionId, String environment);

    AiPrompt update(AiPrompt prompt);
}
