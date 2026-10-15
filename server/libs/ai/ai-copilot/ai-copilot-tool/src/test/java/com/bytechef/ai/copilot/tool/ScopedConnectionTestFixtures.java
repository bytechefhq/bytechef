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

package com.bytechef.ai.copilot.tool;

import com.bytechef.ai.copilot.connection.CopilotConnectionLister;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * @author Ivica Cardic
 */
final class ScopedConnectionTestFixtures {

    static final String EXTERNAL_USER_ID = "1234567890";

    private ScopedConnectionTestFixtures() {
    }

    static ScopedConnectionGuard newGuard(List<CopilotConnectionLister> connectionListers) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        for (int index = 0; index < connectionListers.size(); index++) {
            beanFactory.addBean("connectionLister" + index, connectionListers.get(index));
        }

        return new ScopedConnectionGuard(beanFactory.getBeanProvider(CopilotConnectionLister.class));
    }

    static ScopedConnectionGuard newGuardEntitledTo(List<Long> connectionIds) {
        return newGuard(List.of(new FixedLister(connectionIds)));
    }

    static ToolContext scopedToolContext() {
        Map<String, Object> context = new HashMap<>(
            AgentToolInvocationContext.builder()
                .environmentId(0L)
                .authentication(new UsernamePasswordAuthenticationToken(EXTERNAL_USER_ID, null, List.of()))
                .build()
                .toToolContext());

        context.put(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, 1114L);

        return new ToolContext(context);
    }

    record FixedLister(List<Long> connectionIds) implements CopilotConnectionLister {

        @Override
        public boolean supports(CopilotConnectionRequest request) {
            return EXTERNAL_USER_ID.equals(request.externalUserId());
        }

        @Override
        public List<CopilotConnection> listConnections(CopilotConnectionRequest request) {
            return connectionIds.stream()
                .map(id -> new CopilotConnection(id, "Slack " + id, 0, true))
                .toList();
        }
    }
}
