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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface MultipleConnectionsResumePerformFunction extends ActionDefinition.BaseResumePerformFunction {

    /**
     * Resumes a suspended multi-connection action. The returned value is either a
     * {@link ActionDefinition.ResumePerformFunction.ResumeResponse} for a synchronous resume or an
     * {@link ActionDefinition.SseEmitterHandler} when the resumed turn streams its response. The service layer inspects
     * the concrete type and wraps a streaming handler so the post-output processor can consume it.
     *
     * @param inputParameters
     * @param componentConnections
     * @param extensions
     * @param continueParameters
     * @param data
     * @param context
     * @return a {@code ResumeResponse} or an {@code SseEmitterHandler}
     */
    Object apply(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        Parameters continueParameters, Parameters data, ActionContext context) throws Exception;
}
