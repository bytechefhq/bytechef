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

package com.bytechef.automation.ai.a2a.server.web.rest.config;

import com.bytechef.automation.ai.a2a.server.facade.AutomationA2AServerFacade;
import com.bytechef.platform.ai.a2a.A2AAgentCardFactory;
import com.bytechef.platform.ai.a2a.A2AProtocolHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class AutomationA2AServerRestTestConfiguration {

    @Bean
    A2AAgentCardFactory a2aAgentCardFactory() {
        return new A2AAgentCardFactory();
    }

    @Bean
    A2AProtocolHandler a2aProtocolHandler(AutomationA2AServerFacade automationA2AServerFacade) {
        return new A2AProtocolHandler(automationA2AServerFacade);
    }
}
