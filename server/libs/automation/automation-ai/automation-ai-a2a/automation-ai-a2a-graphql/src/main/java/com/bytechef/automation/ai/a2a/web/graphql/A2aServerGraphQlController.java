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

package com.bytechef.automation.ai.a2a.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for CRUD management of {@link A2aServer} entities — the secret-key-addressed endpoints that expose
 * agent-backed workflows over the A2A protocol.
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
public class A2aServerGraphQlController {

    private final A2aServerService a2aServerService;

    @SuppressFBWarnings("EI")
    public A2aServerGraphQlController(A2aServerService a2aServerService) {
        this.a2aServerService = a2aServerService;
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<A2aServer> a2aServers(@Argument PlatformType type) {
        return a2aServerService.getA2aServers(type);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public A2aServer a2aServer(@Argument long id) {
        return a2aServerService.getA2aServer(id);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aServer createA2aServer(@Argument CreateA2aServerInput input) {
        A2aServer a2aServer = new A2aServer(
            input.name(), input.description(), input.type(), Environment.values()[(int) input.environmentId()]);

        if (input.authenticationRequired() != null) {
            a2aServer.setAuthenticationRequired(input.authenticationRequired());
        }

        return a2aServerService.create(a2aServer);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aServer updateA2aServer(@Argument long id, @Argument UpdateA2aServerInput input) {
        A2aServer a2aServer = a2aServerService.getA2aServer(id);

        if (input.name() != null) {
            a2aServer.setName(input.name());
        }

        if (input.description() != null) {
            a2aServer.setDescription(input.description());
        }

        if (input.enabled() != null) {
            a2aServer.setEnabled(input.enabled());
        }

        if (input.authenticationRequired() != null) {
            a2aServer.setAuthenticationRequired(input.authenticationRequired());
        }

        return a2aServerService.update(a2aServer);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public boolean deleteA2aServer(@Argument long id) {
        a2aServerService.delete(id);

        return true;
    }

    @SchemaMapping(typeName = "A2aServer", field = "environmentId")
    public Long environmentId(A2aServer a2aServer) {
        Environment environment = a2aServer.getEnvironment();

        return (long) environment.ordinal();
    }

    public record CreateA2aServerInput(
        String name, @Nullable String description, PlatformType type, long environmentId,
        @Nullable Boolean authenticationRequired) {
    }

    public record UpdateA2aServerInput(
        @Nullable String name, @Nullable String description, @Nullable Boolean enabled,
        @Nullable Boolean authenticationRequired) {
    }
}
