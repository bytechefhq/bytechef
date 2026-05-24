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

package com.bytechef.platform.configuration.workflow.connection;

import com.bytechef.platform.configuration.domain.ComponentConnection;
import java.util.List;
import java.util.Map;

/**
 * Supplements {@link ComponentConnectionFactory} for cluster elements that require dynamic connection discovery (e.g.
 * connections inferred from embedded scripts rather than explicitly listed in workflow extensions).
 *
 * <p>
 * Implementations are picked up by {@code ClusterRootComponentConnectionFactory} for each cluster element whose
 * {@code componentName} and {@code clusterElementName} are supported.
 *
 * @author Ivica Cardic
 */
public interface ClusterElementConnectionFactory {

    /**
     * Returns {@code true} if this factory handles connections for the given component / cluster-element combination.
     */
    boolean supports(String componentName, String clusterElementName);

    /**
     * Creates component connections for the cluster element identified by the given workflow node name and parameters.
     */
    List<ComponentConnection> create(String workflowNodeName, Map<String, ?> parameters);
}
