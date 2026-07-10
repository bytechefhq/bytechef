/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.user.facade.AuthorityFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.Authority;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for fetching Authorities (roles).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
public class AuthorityGraphQlController {

    private final AuthorityFacade authorityFacade;

    @SuppressFBWarnings("EI")
    public AuthorityGraphQlController(AuthorityFacade authorityFacade) {
        this.authorityFacade = authorityFacade;
    }

    @QueryMapping(name = "authorities")
    public List<String> authorities() {
        return authorityFacade.getAuthorities()
            .stream()
            .map(Authority::getName)
            .collect(Collectors.toList());
    }
}
