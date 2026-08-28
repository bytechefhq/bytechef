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

package com.bytechef.platform.component.owner;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ClusterElementContextAware;
import com.bytechef.platform.component.definition.TriggerContextAware;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.owner.Owner;
import com.bytechef.platform.owner.OwnerResolver;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The one place a component derives the {@link Owner} it is acting for. Shared rather than duplicated, because the
 * editor branch below is the subtle part and two copies of it would not stay in step.
 *
 * @author Ivica Cardic
 */
public final class OwnerResolution {

    private OwnerResolution() {
    }

    /**
     * @return the owner this invocation belongs to, or empty when the caller owns nothing in particular and may see
     *         everything -- Community Edition, or an automation caller that is not a connected user
     */
    public static Optional<Owner> resolve(
        ActionContextAware actionContextAware, ObjectProvider<OwnerResolver> ownerResolverProvider) {

        OwnerResolver ownerResolver = ownerResolverProvider.getIfAvailable();

        if (ownerResolver == null) {
            return Optional.empty();
        }

        // An editor test run has no persisted job and therefore no job principal. Falling through to the job branch
        // would read a null principal and answer "no owner" while a connected user drives the Test button, so the two
        // branches are exclusive rather than one being a fallback for the other.
        if (actionContextAware.isEditorEnvironment()) {
            return ownerResolver.resolveCurrentPrincipal();
        }

        Long jobPrincipalId = actionContextAware.getJobPrincipalId();
        PlatformType platformType = actionContextAware.getPlatformType();

        if (jobPrincipalId == null || platformType == null) {
            return Optional.empty();
        }

        return ownerResolver.resolveJobPrincipal(jobPrincipalId, platformType);
    }

    /**
     * Cluster-element form. A cluster element runs as a tool of an AI agent action, and
     * {@link ClusterElementContextAware#getAgentActionContext()} is that action's context -- but it is nullable, so a
     * cluster element invoked outside an agent falls back to the security context. Never wider than the action form.
     */
    public static Optional<Owner> resolve(
        ClusterElementContext clusterElementContext, ObjectProvider<OwnerResolver> ownerResolverProvider) {

        OwnerResolver ownerResolver = ownerResolverProvider.getIfAvailable();

        if (ownerResolver == null) {
            return Optional.empty();
        }

        if (clusterElementContext instanceof ClusterElementContextAware clusterElementContextAware) {
            ActionContext agentActionContext = clusterElementContextAware.getAgentActionContext();

            if (agentActionContext instanceof ActionContextAware actionContextAware) {
                return resolve(actionContextAware, ownerResolverProvider);
            }
        }

        return ownerResolver.resolveCurrentPrincipal();
    }

    /**
     * Trigger form. {@link TriggerContextAware} carries no editor flag, so the job principal is used when there is one
     * and the security context otherwise -- which is the editor case, where a trigger builds its sample output. Never
     * wider than either branch alone.
     */
    public static Optional<Owner> resolve(
        TriggerContextAware triggerContextAware, ObjectProvider<OwnerResolver> ownerResolverProvider) {

        OwnerResolver ownerResolver = ownerResolverProvider.getIfAvailable();

        if (ownerResolver == null) {
            return Optional.empty();
        }

        Long jobPrincipalId = triggerContextAware.getJobPrincipalId();
        PlatformType platformType = triggerContextAware.getType();

        if (jobPrincipalId == null || platformType == null) {
            return ownerResolver.resolveCurrentPrincipal();
        }

        return ownerResolver.resolveJobPrincipal(jobPrincipalId, platformType);
    }
}
