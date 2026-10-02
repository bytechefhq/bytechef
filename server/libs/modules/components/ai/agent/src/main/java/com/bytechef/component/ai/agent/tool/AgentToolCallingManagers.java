/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.agent.tool;

import com.bytechef.platform.ai.sensitivedata.SensitiveDataMetrics;
import com.bytechef.platform.ai.sensitivedata.SensitiveDataRedactor;
import com.bytechef.platform.ai.sensitivedata.tokenization.PiiTokenBoundaryToolCallingManager;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public final class AgentToolCallingManagers {

    private final ToolCallingManager defaultToolCallingManager;
    private final SensitiveDataRedactor sensitiveDataRedactor;

    public AgentToolCallingManagers(ToolCallingManager defaultToolCallingManager) {
        this.defaultToolCallingManager = defaultToolCallingManager;
        this.sensitiveDataRedactor = new SensitiveDataRedactor(List.of());
    }

    @Autowired
    public AgentToolCallingManagers(
        ToolCallingManager defaultToolCallingManager,
        ObjectProvider<SensitiveDataRedactor> sensitiveDataRedactorProvider) {

        this.defaultToolCallingManager = defaultToolCallingManager;
        this.sensitiveDataRedactor =
            sensitiveDataRedactorProvider.getIfAvailable(() -> new SensitiveDataRedactor(List.of()));
    }

    public ToolCallingManager getToolCallingManager(@Nullable SensitiveDataMetrics metrics) {
        return PiiTokenBoundaryToolCallingManager.wrap(defaultToolCallingManager, sensitiveDataRedactor, () -> metrics);
    }
}
