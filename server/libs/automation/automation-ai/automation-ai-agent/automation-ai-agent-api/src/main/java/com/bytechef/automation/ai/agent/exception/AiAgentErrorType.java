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

package com.bytechef.automation.ai.agent.exception;

import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.exception.AbstractErrorType;

/**
 * @author Ivica Cardic
 */
public class AiAgentErrorType extends AbstractErrorType {

    public static final AiAgentErrorType ELEMENT_KIND_ALREADY_PRESENT = new AiAgentErrorType(100);

    public static final AiAgentErrorType CHANNEL_NOT_DELETABLE = new AiAgentErrorType(101);

    public static final AiAgentErrorType CHANNEL_ALREADY_PRESENT = new AiAgentErrorType(102);

    public static final AiAgentErrorType UNKNOWN_CHANNEL_TYPE = new AiAgentErrorType(103);

    public static final AiAgentErrorType AGENT_HAS_DEPLOYMENTS = new AiAgentErrorType(104);

    public static final AiAgentErrorType AGENT_REFERENCED_AS_SUB_AGENT = new AiAgentErrorType(105);

    public static final AiAgentErrorType SUB_AGENT_CYCLE = new AiAgentErrorType(106);

    public static final AiAgentErrorType MODEL_MISSING = new AiAgentErrorType(107);

    public static final AiAgentErrorType CHANNEL_CONNECTION_MISSING = new AiAgentErrorType(108);

    public static final AiAgentErrorType SUB_AGENT_NOT_PUBLISHED = new AiAgentErrorType(109);

    public static final AiAgentErrorType BUILT_IN_TOOL_CONNECTION_MISSING = new AiAgentErrorType(112);

    public static final AiAgentErrorType INVALID_AGENT_IMPORT = new AiAgentErrorType(113);

    public static final AiAgentErrorType CHANNEL_PARAMETER_MISSING = new AiAgentErrorType(114);

    public static final AiAgentErrorType INVALID_AGENT = new AiAgentErrorType(115);

    public static final AiAgentErrorType NATIVE_WEB_SEARCH_UNSUPPORTED = new AiAgentErrorType(116);

    public static final AiAgentErrorType THINKING_UNSUPPORTED = new AiAgentErrorType(117);

    public static final AiAgentErrorType CONNECTION_NOT_IN_WORKSPACE = new AiAgentErrorType(118);

    public static final AiAgentErrorType AGENT_VERSION_NOT_FOUND = new AiAgentErrorType(119);

    public static final AiAgentErrorType REFERENCE_NOT_IN_WORKSPACE = new AiAgentErrorType(120);

    public AiAgentErrorType(int errorKey) {
        super(AiAgentFacade.class, errorKey);
    }
}
