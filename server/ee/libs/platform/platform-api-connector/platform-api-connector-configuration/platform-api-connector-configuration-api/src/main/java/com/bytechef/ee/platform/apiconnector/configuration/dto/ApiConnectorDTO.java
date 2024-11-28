/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.dto;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnector;
import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorEndpoint;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ApiConnectorDTO(
    int connectorVersion, String createdBy, Instant createdDate, String definition, String description,
    boolean enabled, List<ApiConnectorEndpoint> endpoints, String icon, Long id, String lastModifiedBy,
    Instant lastModifiedDate, String name, String specification, String title, int version) {

    public ApiConnectorDTO(
        ApiConnector apiConnector, String definition, String specification, List<ApiConnectorEndpoint> endpoints) {

        this(
            apiConnector.getConnectorVersion(), apiConnector.getCreatedBy(), apiConnector.getCreatedDate(),
            definition, apiConnector.getDescription(), apiConnector.getEnabled(), endpoints,
            apiConnector.getIcon(), apiConnector.getId(), apiConnector.getLastModifiedBy(),
            apiConnector.getLastModifiedDate(), apiConnector.getName(), specification, apiConnector.getTitle(),
            apiConnector.getVersion());
    }
}
