/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single definition of which connections an embedded connected user is entitled to, in one environment.
 *
 * <p>
 * It exists because that question used to be answered twice, by two hand-written unions of the same two sources --
 * {@code ConnectedUserConnectionFacadeImpl.getConnections}, which decides what the connection picker SHOWS, and
 * {@code ConnectedUserResourceMembershipResolver.resolveConnection}, which decides what authorization GRANTS. Two
 * copies of an entitlement rule drift, and the shape the drift takes is a connection that appears in the picker and
 * then 403s when it is used. Both now call this, so they cannot disagree: the picker's result is this set filtered by
 * component name, and the resolver's grant is membership of this set.
 *
 * <p>
 * The two public methods are one definition, not two: {@link #getOwnedConnectionIds(long, Environment)} is the base and
 * {@link #getConnectionIds(long, Environment)} is that base plus source 3, both built from the same private overload
 * over the same instance list. {@code owned} is therefore a subset of {@code entitled} structurally rather than by
 * convention, and an edit to one cannot leave the other behind.
 *
 * <p>
 * Three sources, unioned:
 * <ol>
 * <li>the connection on each {@link IntegrationInstance} the connected user owns;</li>
 * <li>the connections the connected user created for themselves, via {@link ConnectedUserConnectionService};</li>
 * <li>the connections the tenant admin bound at the {@code IntegrationInstanceConfiguration} level, reached through the
 * configurations the connected user's own instances derive from.</li>
 * </ol>
 *
 * <p>
 * The third source is what a "shared connection" is in this data model, and deriving it here replaces the host's
 * {@code sharedConnectionIds} request parameter, which the server could not verify and therefore now ignores. A
 * configuration-level binding is a record the server already holds, so it needs no host declaration and no token
 * plumbing -- and unbinding the connection in the configuration revokes the entitlement on the next request, which a
 * signed copy carried in an embed token could not.
 *
 * <p>
 * The third source starts from THIS caller's instances and never enumerates configurations directly. One configuration
 * is shared by every connected user whose instance derives from it, so walking configurations would hand each of them
 * the others' entitlements; walking the caller's instances yields only the configurations that caller is actually
 * attached to.
 *
 * <p>
 * One caveat on the cannot-disagree claim above, which is about the RULE and not about the inputs. The picker reaches
 * this class with the environment on the {@code ConnectedUser} the {@code X-Environment} header selected, while the
 * resolver reaches it with the environment on the authenticated principal. Both are the caller's environment on every
 * path that exists today, but they are read from different places, so a deployment that let them differ would have the
 * picker over-list and the resolver deny -- the deny-safe direction, and a pre-existing property of those two entry
 * points rather than of this computation. What is guaranteed here is that for the SAME environment the two get the same
 * set.
 *
 * <p>
 * The environment axis is carried by source 1 and inherited by source 3 rather than re-applied to it. The instance
 * lookup is environment-scoped -- {@code integration_instance_configuration.environment} is the column it joins on --
 * so the configuration ids source 3 walks are, by construction, the ids of configurations in the requested environment.
 * A caller confined to DEVELOPMENT therefore cannot reach a PRODUCTION configuration's connections: the PRODUCTION
 * configuration's id never enters the query at all, because no instance of the caller's carries it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectedUserConnectionMembership {

    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionMembership(
        ConnectedUserConnectionService connectedUserConnectionService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService) {

        this.connectedUserConnectionService = connectedUserConnectionService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
    }

    /**
     * Every connection id this connected user is entitled to in {@code environment}: the three sources above, unioned.
     * Insertion-ordered so the picker's listing stays stable across requests; callers that only need membership can
     * treat it as a plain set.
     */
    @Transactional(readOnly = true)
    public Set<Long> getConnectionIds(long connectedUserId, Environment environment) {
        List<IntegrationInstance> integrationInstances =
            integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId, environment);

        Set<Long> connectionIds = getOwnedConnectionIds(connectedUserId, integrationInstances);

        connectionIds.addAll(getIntegrationInstanceConfigurationConnectionIds(integrationInstances));

        return connectionIds;
    }

    /**
     * The subset the connected user OWNS -- sources 1 and 2 only, never the configuration-level bindings of source 3.
     *
     * <p>
     * Entitlement and ownership are deliberately different sets, and this is the method that keeps them apart. A
     * configuration-level connection is entitled to every connected user attached to that configuration, so deleting or
     * reauthorizing it through an end user's own credentials would act on all of them at once; it belongs to the tenant
     * admin who bound it. Callers that MUTATE a connection must use this, callers that merely list or authorize a read
     * must use {@link #getConnectionIds(long, Environment)}.
     */
    @Transactional(readOnly = true)
    public Set<Long> getOwnedConnectionIds(long connectedUserId, Environment environment) {
        return getOwnedConnectionIds(
            connectedUserId,
            integrationInstanceService.getConnectedUserIntegrationInstances(connectedUserId, environment));
    }

    private Set<Long> getOwnedConnectionIds(long connectedUserId, List<IntegrationInstance> integrationInstances) {
        Set<Long> connectionIds = new LinkedHashSet<>();

        for (IntegrationInstance integrationInstance : integrationInstances) {
            connectionIds.add(integrationInstance.getConnectionId());
        }

        connectionIds.addAll(connectedUserConnectionService.getConnectionIds(connectedUserId));

        return connectionIds;
    }

    /**
     * The connections bound at the configuration level of the configurations these instances derive from.
     *
     * <p>
     * A configuration binds its connections per workflow: the row lives on
     * {@link IntegrationInstanceConfigurationWorkflowConnection}, which hangs off
     * {@link IntegrationInstanceConfigurationWorkflow}, which carries the configuration id. So a configuration id
     * reaches its connections through its workflows, never directly.
     */
    private Set<Long> getIntegrationInstanceConfigurationConnectionIds(
        List<IntegrationInstance> integrationInstances) {

        List<Long> integrationInstanceConfigurationIds = integrationInstances.stream()
            .map(IntegrationInstance::getIntegrationInstanceConfigurationId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (integrationInstanceConfigurationIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> connectionIds = new LinkedHashSet<>();

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationIds);

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {
            for (IntegrationInstanceConfigurationWorkflowConnection workflowConnection : integrationInstanceConfigurationWorkflow
                .getConnections()) {

                connectionIds.add(workflowConnection.getConnectionId());
            }
        }

        return connectionIds;
    }
}
