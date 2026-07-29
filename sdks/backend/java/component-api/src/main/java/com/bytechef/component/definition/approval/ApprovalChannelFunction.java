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

package com.bytechef.component.definition.approval;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;

/**
 * Implements an approval channel, the cluster element that delivers a human-approval request (for example, via chat or
 * email) and returns the outcome. The calling approval action publishes its form metadata under the well-known keys
 * defined here so the channel can decide how to render the request.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ApprovalChannelFunction {

    /**
     * The cluster element type under which approval channels are registered.
     */
    ClusterElementType APPROVAL_CHANNELS =
        new ClusterElementType("APPROVAL_CHANNELS", "approvalChannels", "Channels", true, false);

    /**
     * Key under which the calling approval action publishes the form description.
     */
    String FORM_DESCRIPTION = "formDescription";

    /**
     * Key under which the calling approval action publishes the form title.
     */
    String FORM_TITLE = "formTitle";

    /**
     * Key under which the calling approval action publishes its form-input definitions. Channels can consult this list
     * to decide between rendering a single "open form" link (when the form has fields the user must fill in) and a pair
     * of one-click Approve/Discard actions (when the list is empty).
     */
    String INPUTS = "inputs";

    /**
     * Delivers the approval request through this channel and returns the channel-specific result.
     *
     * @param inputParameters      the input parameters configured for the channel
     * @param connectionParameters the connection parameters
     * @param formUrl              the URL of the approval form the user opens to respond
     * @param context              the cluster element execution context
     * @return the result of delivering the approval request
     * @throws Exception if the request cannot be delivered
     */
    Object apply(
        Parameters inputParameters, Parameters connectionParameters, String formUrl, ClusterElementContext context)
        throws Exception;
}
