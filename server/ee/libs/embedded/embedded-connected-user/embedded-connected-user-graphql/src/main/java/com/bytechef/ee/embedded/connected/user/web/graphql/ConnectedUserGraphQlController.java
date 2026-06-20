/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.facade.ConnectedUserFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class ConnectedUserGraphQlController {

    private final ConnectedUserFacade connectedUserFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserGraphQlController(ConnectedUserFacade connectedUserFacade) {
        this.connectedUserFacade = connectedUserFacade;
    }

    @QueryMapping
    public ConnectedUser connectedUser(@Argument long id) {
        return connectedUserFacade.getConnectedUserEntity(id);
    }

    @QueryMapping
    public ConnectedUserPage connectedUsers(
        @Argument Long environmentId, @Argument String name, @Argument String createDateFrom,
        @Argument String createDateTo, @Argument Long integrationId, @Argument Integer pageNumber) {

        LocalDate dateFrom = createDateFrom != null ? LocalDate.parse(createDateFrom) : null;
        LocalDate dateTo = createDateTo != null ? LocalDate.parse(createDateTo) : null;
        int page = pageNumber != null ? pageNumber : 0;

        Page<ConnectedUser> connectedUsersPage = connectedUserFacade.getConnectedUserEntities(
            environmentId, name, dateFrom, dateTo, integrationId, page);

        return new ConnectedUserPage(
            connectedUsersPage.getContent(), connectedUsersPage.getTotalElements(), connectedUsersPage.getTotalPages(),
            connectedUsersPage.getNumber(), connectedUsersPage.getSize());
    }

    @SuppressFBWarnings("EI")
    public record ConnectedUserPage(
        List<ConnectedUser> content, long totalElements, int totalPages, int number, int size) {
    }
}
