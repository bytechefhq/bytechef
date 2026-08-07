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

import static com.bytechef.component.definition.approval.ApprovalChannelFunction.APPROVAL_CHANNELS;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface ApprovalComponentDefinition extends ClusterRootComponentDefinition {

    String REQUEST_APPROVAL = "requestApproval";

    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(APPROVAL_CHANNELS);
    }

    /**
     * Child types are declared per cluster element, not just for the component as a whole: the type list above applies
     * to the component, so an element left out of this map inherits every type the component declares. Only the tool
     * that raises a request may carry channels.
     */
    @Override
    default Map<String, List<String>> getClusterElementClusterElementTypes() {
        return Map.of(REQUEST_APPROVAL, List.of(APPROVAL_CHANNELS.name()));
    }
}
